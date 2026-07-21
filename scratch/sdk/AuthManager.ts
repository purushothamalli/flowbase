import type {LoginResponse, RequestContext, RequestOptions, ResponseContext, TokenStorage, User} from "./types";
import type {FlowBaseClient} from "./FlowBaseClient";
import {FlowBaseError, HttpError, NetworkError, RateLimitError} from "./errors";
import {MemoryStorage} from "./storage";

export class AuthManager {
    private token: string | null = null;
    private refreshToken: string | null = null;
    private currentUser: User | null = null;
    private client: FlowBaseClient;
    private storage: TokenStorage;
    private readonly STORAGE_ACCESS_TOKEN_KEY = "fb_access_token";
    private readonly STORAGE_REFRESH_TOKEN_KEY = "fb_refresh_token";
    private isRefreshing: boolean = false;
    private refreshQueue: Array<(token: string) => void> = [];

    constructor(client: FlowBaseClient) {
        this.client = client;
        this.storage = client.storageTokens || new MemoryStorage();
        this.token = this.storage.getItem(this.STORAGE_ACCESS_TOKEN_KEY);
        this.refreshToken = this.storage.getItem(this.STORAGE_REFRESH_TOKEN_KEY);
    }

    getToken(): string | null {
        return this.storage.getItem(this.STORAGE_ACCESS_TOKEN_KEY);
    }

    getCurrentUser(): User | null {
        return this.currentUser;
    }

    public async login(email: string, password: string): Promise<LoginResponse> {
        const response = await this.fetch<LoginResponse>("/v1/auth/login", {
            method: "POST", body: JSON.stringify({email, password})
        });
        this.token = response.accessToken;
        this.storage.setItem(this.STORAGE_ACCESS_TOKEN_KEY, response.accessToken);
        return response;
    }

    public async register(email: string, password: string, role = "DEVELOPER"): Promise<User> {
        const response = await this.fetch<User>("/v1/auth/register", {
            method: "POST",
            body: JSON.stringify({email, password, role})
        });
        this.currentUser = response;
        return response;
    }

    public async logout(): Promise<void> {
        await this.fetch<void>("/v1/auth/logout", {method: "POST"});
        this.currentUser = null;
        this.token = null;
        this.storage.removeItem(this.STORAGE_ACCESS_TOKEN_KEY);
        this.refreshToken = null;
        this.storage.removeItem(this.STORAGE_REFRESH_TOKEN_KEY);
    }

    public async refresh(): Promise<LoginResponse> {
        const headers: Record<string, string> = {};
        if (this.refreshToken) {
            headers["Cookie"] = `fb_refresh_token=${this.refreshToken}`;
        }
        const res = await this.fetch<LoginResponse>("/v1/auth/refresh", {
            method: "POST",
            headers
        });
        this.token = res.accessToken;
        this.storage.setItem(this.STORAGE_ACCESS_TOKEN_KEY, res.accessToken);
        return res;
    }

    private getAuthHeaders(): Record<string, string> {
        const headers: Record<string, string> = {
            "Content-Type": "application/json",
            'X-FlowBase-API-Key': this.client.apiKey
        }
        if (this.token != null) {
            headers.Authorization = `Bearer ${this.token}`;
        }
        return headers;
    }

    public async fetch<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
        const fetchEngine: typeof fetch = this.client.customFetch || fetch;
        const normalizedEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
        const url = endpoint.startsWith("http") ? endpoint : `${this.client.baseUrl}${normalizedEndpoint}`;
        const headers = {...this.getAuthHeaders(), ...(options.headers || {})};
        if (typeof FormData !== "undefined" && options.body instanceof FormData) {
            delete (headers as any)["Content-Type"];
        }
        const timeoutMs = options.timeoutMs || this.client.timeoutMs || 10000;
        const maxRetries = options.retry?.maxRetries ?? this.client.retryConfig?.maxRetries ?? 3;
        const retryStatusCodes = options.retry?.retryStatusCodes ?? this.client.retryConfig?.retryStatusCodes ?? [502, 503, 504];
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
        const signal = options.signal ? AbortSignal.any([options.signal, controller.signal]) : controller.signal;
        const config = {...options, headers, credentials: options.credentials || "include", signal};
        const request: RequestContext = {url, options: config};
        let attempts = 0;
        let response: Response;
        while (true) {
            let index = -1;
            const dispatch = async (i: number): Promise<ResponseContext> => {
                if (i <= index) throw new Error("next() called multiple times!");
                index = i;
                const chain = this.client.getMiddlewares();
                if (i === chain.length) {
                    const res = await fetchEngine(request.url, request.options);
                    return {response: res};
                }
                const fn = chain[i];
                return await fn(request, () => dispatch(i + 1));
            };
            try {
                response = (await dispatch(0)).response;
                if (!response.ok) {
                    if (response.status === 401 && !(options as any)._isRetry && this.refreshToken && !endpoint.includes("/v1/auth/login")) {
                        (options as any)._isRetry = true;
                        if (this.isRefreshing) {
                            return await new Promise((resolve) => {
                                this.refreshQueue.push((newToken) => {
                                    (config.headers as any).Authorization = "Bearer " + newToken;
                                    resolve(this.fetch<T>(endpoint, options));
                                });
                            });
                        }
                        this.isRefreshing = true;
                        try {
                            await this.refresh();
                        } catch (e) {
                            await this.logout();
                            throw e;
                        } finally {
                            this.isRefreshing = false;
                        }
                        this.refreshQueue.forEach(cb => cb(this.token!));
                        this.refreshQueue = [];
                        (config.headers as any).Authorization = "Bearer " + this.token;
                        return await this.fetch<T>(endpoint, options);
                    }
                    if (response.status === 429) {
                        const retryAfterHeader = response.headers.get("retry-after");
                        const retryAfterSeconds = retryAfterHeader ? parseInt(retryAfterHeader, 10) : 1;
                        if (attempts < maxRetries) {
                            attempts++;
                            await new Promise(r => setTimeout(r, retryAfterSeconds * 1000));
                            continue;
                        }
                        throw new RateLimitError(retryAfterSeconds, "Rate limit exceeded. Too many requests.");
                    }
                    if (retryStatusCodes.includes(response.status) && attempts < maxRetries) {
                        attempts++;
                        const delay = Math.min(1000 * Math.pow(2, attempts - 1), 10000);
                        await new Promise(r => setTimeout(r, delay));
                        continue;
                    }

                    let errorBody: any;
                    try {
                        errorBody = await response.json();
                    } catch {
                        errorBody = await response.text();
                    }
                    throw new HttpError(response.status, response.statusText, errorBody);
                }
                break; // 200 OK!
            } catch (err) {
                if (err instanceof FlowBaseError) throw err;
                if (attempts < maxRetries && !signal.aborted) {
                    attempts++;
                    const delay = Math.min(1000 * Math.pow(2, attempts - 1), 10000);
                    await new Promise(r => setTimeout(r, delay));
                    continue;
                }
                throw new NetworkError((err as Error).message);
            } finally {
                clearTimeout(timeoutId);
            }
        }
        const cookieHeader = response.headers.get("set-cookie");
        if (cookieHeader) {
            const match = cookieHeader.match(/fb_refresh_token=([^;]+)/);
            if (match) { // @ts-ignore
                this.refreshToken = match[1];
                this.storage.setItem(this.STORAGE_REFRESH_TOKEN_KEY, this.refreshToken as string);
            }
        }
        if (response.status == 204) return null as T;
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
            return (await response.json()) as T;
        }
        return (await response.text()) as unknown as T;
    }

    public async fetchRaw(endpoint: string, options: RequestOptions = {}): Promise<Response> {
        try {
            const customFetchEngine = this.client.customFetch || fetch;
            const normalizedEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
            const url = endpoint.startsWith("http") ? endpoint : `${this.client.baseUrl}${normalizedEndpoint}`;
            const headers = {...this.getAuthHeaders(), ...(options.headers || {})};
            if (typeof FormData !== "undefined" && options.body instanceof FormData) {
                delete (headers as any)["Content-Type"];
            }
            const config = {...options, headers, credentials: options.credentials || "include"};
            return await customFetchEngine(url, config);
        } catch (e) {
            console.log(e);
            throw new HttpError(500, "Error during raw fetch!", e);
        }
    }
}
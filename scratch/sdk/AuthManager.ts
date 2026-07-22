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
        this.client.emit("login", response);
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
        this.client.emit("logout");
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

    public getRefreshToken(): string | null {
        return this.refreshToken;
    }

    public setRefreshToken(token: string): void {
        this.refreshToken = token;
        this.storage.setItem(this.STORAGE_REFRESH_TOKEN_KEY, token);
    }

    public async refreshAccessTokenSync(): Promise<string> {
        if (this.isRefreshing) {
            return await new Promise((resolve, reject) => {
                this.refreshQueue.push((newToken) => {
                    if (newToken) resolve(newToken);
                    else reject(new Error("Refresh token sync execution failed"));
                });
            });
        }
        this.isRefreshing = true;
        try {
            const res = await this.refresh();
            this.token = res.accessToken;
            this.storage.setItem(this.STORAGE_ACCESS_TOKEN_KEY, res.accessToken);
            this.refreshQueue.forEach(cb => cb(res.accessToken));
            this.client.emit("refresh", res.accessToken);
            return res.accessToken;
        } catch (e) {
            this.refreshQueue.forEach(cb => cb(""));
            this.client.emit("error", e);
            throw e;
        } finally {
            this.isRefreshing = false;
            this.refreshQueue = [];
        }
    }

    public async fetch<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
        return await this.client.httpClient.request<T>(endpoint, options);
    }

    public async fetchRaw(endpoint: string, options: RequestOptions = {}): Promise<Response> {
        try {
            const customFetchEngine = this.client.customFetch || fetch;
            const normalizedEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
            const url = endpoint.startsWith("http") ? endpoint : `${this.client.baseUrl}${normalizedEndpoint}`;
            const headers: Record<string, string> = {
                "Content-Type": "application/json",
                "X-FlowBase-API-Key": this.client.apiKey
            };
            if (options.headers) {
                Object.assign(headers, options.headers);
            }
            if (this.token) {
                headers["Authorization"] = `Bearer ${this.token}`;
            }
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
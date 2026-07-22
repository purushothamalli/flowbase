import { FlowBaseClient } from "./FlowBaseClient";
import { RequestContext, ResponseContext, RequestOptions } from "./types";
import { HttpError, NetworkError, RateLimitError, FlowBaseError } from "./errors";

export class HttpClient {
    private client: FlowBaseClient;

    constructor(client: FlowBaseClient) {
        this.client = client;
    }

    private getBaseHeaders(): Record<string, string> {
        const headers: Record<string, string> = {
            "Content-Type": "application/json",
            "X-FlowBase-API-Key": this.client.apiKey
        };
        const token = this.client.auth.getToken();
        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }
        return headers;
    }

    private sleep(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    public async request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
        const maxRetries = options.retry?.maxRetries ?? this.client.retryConfig?.maxRetries ?? 3;
        const retryStatusCodes = options.retry?.retryStatusCodes ?? this.client.retryConfig?.retryStatusCodes ?? [429, 502, 503, 504];

        let attempt = 0;
        while (true) {
            try {
                return await this.executeRequest<T>(endpoint, options);
            } catch (err: any) {
                if (err instanceof FlowBaseError && !(err instanceof HttpError)) {
                    throw err;
                }
                const isTransient = err instanceof HttpError && retryStatusCodes.includes(err.status);
                const isNetworkError = err instanceof NetworkError;

                if (attempt < maxRetries && (isTransient || isNetworkError)) {
                    attempt++;
                    let delay = Math.min(1000 * Math.pow(2, attempt - 1), 10000);
                    if (err instanceof HttpError && err.status === 429) {
                        // Rate limit specific wait duration if header is present
                        const retryAfter = (err as any).retryAfterSeconds || 1;
                        delay = retryAfter * 1000;
                    }
                    console.warn(`[HttpClient] Transient error occurred. Retrying attempt ${attempt}/${maxRetries} in ${delay}ms...`);
                    await this.sleep(delay);
                    continue;
                }
                throw err;
            }
        }
    }

    private async executeRequest<T>(endpoint: string, options: RequestOptions): Promise<T> {
        const fetchEngine = this.client.customFetch || fetch;
        const normalizedEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
        const url = endpoint.startsWith("http") ? endpoint : `${this.client.baseUrl}${normalizedEndpoint}`;
        
        // Merge headers dynamically
        const mergedHeaders = { ...this.getBaseHeaders(), ...(options.headers || {}) };
        if (typeof FormData !== "undefined" && options.body instanceof FormData) {
            delete (mergedHeaders as any)["Content-Type"];
        }

        const timeoutMs = options.timeoutMs || this.client.timeoutMs || 10000;
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
        const signal = options.signal ? AbortSignal.any([options.signal, controller.signal]) : controller.signal;

        const config = { ...options, headers: mergedHeaders, credentials: options.credentials || "include", signal };
        const requestCtx: RequestContext = { url, options: config };

        let index = -1;
        const chain = this.client.getMiddlewares();

        const dispatch = async (i: number): Promise<ResponseContext> => {
            if (i <= index) throw new Error("next() called multiple times!");
            index = i;
            if (i === chain.length) {
                try {
                    const res = await fetchEngine(requestCtx.url, requestCtx.options);
                    return { response: res };
                } catch (err: any) {
                    if (err.name === "AbortError" || signal.aborted) {
                        throw new NetworkError(`Request timed out after ${timeoutMs}ms`);
                    }
                    throw new NetworkError(err.message || "Network connection error");
                } finally {
                    clearTimeout(timeoutId);
                }
            }
            const fn = chain[i];
            if (!fn) throw new Error("Interceptor out of bounds");
            return await fn(requestCtx, () => dispatch(i + 1));
        };

        const resCtx = await dispatch(0);
        const response = resCtx.response;

        // Perform set-cookie extraction if available
        const cookieHeader = response.headers.get("set-cookie");
        if (cookieHeader) {
            const match = cookieHeader.match(/fb_refresh_token=([^;]+)/);
            if (match) {
                this.client.auth.setRefreshToken(match[1]!);
            }
        }

        const headersRecord = options.headers as Record<string, any> | undefined;
        if (response.status === 401 && !headersRecord?.["X-Skip-Refresh"] && this.client.auth.getRefreshToken() && !endpoint.includes("/v1/auth/login") && !endpoint.includes("/v1/auth/refresh")) {
            try {
                // Trigger atomic token refresh sync
                const newAccessToken = await this.client.auth.refreshAccessTokenSync();
                const retryHeaders: Record<string, string> = {};
                if (options.headers) {
                    Object.assign(retryHeaders, options.headers);
                }
                retryHeaders["Authorization"] = `Bearer ${newAccessToken}`;
                retryHeaders["X-Skip-Refresh"] = "true"; // Prevent loops

                const retryOptions = {
                    ...options,
                    headers: retryHeaders
                };
                return await this.executeRequest<T>(endpoint, retryOptions);
            } catch (refreshErr) {
                await this.client.auth.logout();
                throw refreshErr;
            }
        }

        if (!response.ok) {
            let errorBody: any;
            try {
                errorBody = await response.json();
            } catch {
                try {
                    errorBody = await response.text();
                } catch {
                    errorBody = "";
                }
            }

            if (response.status === 429) {
                const retryAfterHeader = response.headers.get("retry-after");
                const retryAfterSeconds = retryAfterHeader ? parseInt(retryAfterHeader, 10) : 1;
                const err = new RateLimitError(retryAfterSeconds, "Rate limit exceeded. Too many requests.");
                (err as any).retryAfterSeconds = retryAfterSeconds;
                throw err;
            }
            throw new HttpError(response.status, response.statusText, errorBody);
        }

        if (response.status === 204) {
            return null as T;
        }

        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
            return await response.json() as T;
        }
        return await response.text() as unknown as T;
    }
}

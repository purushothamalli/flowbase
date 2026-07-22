"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.HttpClient = void 0;
const errors_1 = require("./errors");
class HttpClient {
    client;
    constructor(client) {
        this.client = client;
    }
    getBaseHeaders() {
        const headers = {
            "Content-Type": "application/json",
            "X-FlowBase-API-Key": this.client.apiKey
        };
        const token = this.client.auth.getToken();
        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }
        return headers;
    }
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    async request(endpoint, options = {}) {
        const maxRetries = options.retry?.maxRetries ?? this.client.retryConfig?.maxRetries ?? 3;
        const retryStatusCodes = options.retry?.retryStatusCodes ?? this.client.retryConfig?.retryStatusCodes ?? [429, 502, 503, 504];
        let attempt = 0;
        while (true) {
            try {
                return await this.executeRequest(endpoint, options);
            }
            catch (err) {
                if (err instanceof errors_1.FlowBaseError && !(err instanceof errors_1.HttpError)) {
                    throw err;
                }
                const isTransient = err instanceof errors_1.HttpError && retryStatusCodes.includes(err.status);
                const isNetworkError = err instanceof errors_1.NetworkError;
                if (attempt < maxRetries && (isTransient || isNetworkError)) {
                    attempt++;
                    let delay = Math.min(1000 * Math.pow(2, attempt - 1), 10000);
                    if (err instanceof errors_1.HttpError && err.status === 429) {
                        // Rate limit specific wait duration if header is present
                        const retryAfter = err.retryAfterSeconds || 1;
                        delay = retryAfter * 1000;
                    }
                    console.warn(`[HttpClient] Transient error occurred. Retrying attempt ${attempt}/${maxRetries} in ${delay}ms...`);
                    await this.sleep(delay);
                    continue;
                }
                this.client.emit("error", err);
                throw err;
            }
        }
    }
    async executeRequest(endpoint, options) {
        const fetchEngine = this.client.customFetch || fetch;
        const normalizedEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
        const url = endpoint.startsWith("http") ? endpoint : `${this.client.baseUrl}${normalizedEndpoint}`;
        // Merge headers dynamically
        const mergedHeaders = { ...this.getBaseHeaders(), ...(options.headers || {}) };
        if (typeof FormData !== "undefined" && options.body instanceof FormData) {
            delete mergedHeaders["Content-Type"];
        }
        const timeoutMs = options.timeoutMs || this.client.timeoutMs || 10000;
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
        const signal = options.signal ? AbortSignal.any([options.signal, controller.signal]) : controller.signal;
        const config = { ...options, headers: mergedHeaders, credentials: options.credentials || "include", signal };
        const requestCtx = { url, options: config };
        let index = -1;
        const chain = this.client.getMiddlewares();
        const dispatch = async (i) => {
            if (i <= index)
                throw new Error("next() called multiple times!");
            index = i;
            if (i === chain.length) {
                try {
                    const res = await fetchEngine(requestCtx.url, requestCtx.options);
                    return { response: res };
                }
                catch (err) {
                    if (err.name === "AbortError" || signal.aborted) {
                        throw new errors_1.NetworkError(`Request timed out after ${timeoutMs}ms`);
                    }
                    throw new errors_1.NetworkError(err.message || "Network connection error");
                }
                finally {
                    clearTimeout(timeoutId);
                }
            }
            const fn = chain[i];
            if (!fn)
                throw new Error("Interceptor out of bounds");
            return await fn(requestCtx, () => dispatch(i + 1));
        };
        const resCtx = await dispatch(0);
        const response = resCtx.response;
        // Perform set-cookie extraction if available
        const cookieHeader = response.headers.get("set-cookie");
        if (cookieHeader) {
            const match = cookieHeader.match(/fb_refresh_token=([^;]+)/);
            if (match) {
                this.client.auth.setRefreshToken(match[1]);
            }
        }
        const headersRecord = options.headers;
        if (response.status === 401 && !headersRecord?.["X-Skip-Refresh"] && this.client.auth.getRefreshToken() && !endpoint.includes("/v1/auth/login") && !endpoint.includes("/v1/auth/refresh")) {
            try {
                // Trigger atomic token refresh sync
                const newAccessToken = await this.client.auth.refreshAccessTokenSync();
                const retryHeaders = {};
                if (options.headers) {
                    Object.assign(retryHeaders, options.headers);
                }
                retryHeaders["Authorization"] = `Bearer ${newAccessToken}`;
                retryHeaders["X-Skip-Refresh"] = "true"; // Prevent loops
                const retryOptions = {
                    ...options,
                    headers: retryHeaders
                };
                return await this.executeRequest(endpoint, retryOptions);
            }
            catch (refreshErr) {
                await this.client.auth.logout();
                throw refreshErr;
            }
        }
        if (!response.ok) {
            let errorBody;
            try {
                errorBody = await response.json();
            }
            catch {
                try {
                    errorBody = await response.text();
                }
                catch {
                    errorBody = "";
                }
            }
            if (response.status === 429) {
                const retryAfterHeader = response.headers.get("retry-after");
                const retryAfterSeconds = retryAfterHeader ? parseInt(retryAfterHeader, 10) : 1;
                const err = new errors_1.RateLimitError(retryAfterSeconds, "Rate limit exceeded. Too many requests.");
                err.retryAfterSeconds = retryAfterSeconds;
                throw err;
            }
            throw new errors_1.HttpError(response.status, response.statusText, errorBody);
        }
        if (response.status === 204) {
            return null;
        }
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
            return await response.json();
        }
        return await response.text();
    }
}
exports.HttpClient = HttpClient;
//# sourceMappingURL=HttpClient.js.map
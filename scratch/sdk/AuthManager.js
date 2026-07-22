"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuthManager = void 0;
const errors_1 = require("./errors");
const storage_1 = require("./storage");
class AuthManager {
    token = null;
    refreshToken = null;
    currentUser = null;
    client;
    storage;
    STORAGE_ACCESS_TOKEN_KEY = "fb_access_token";
    STORAGE_REFRESH_TOKEN_KEY = "fb_refresh_token";
    isRefreshing = false;
    refreshQueue = [];
    constructor(client) {
        this.client = client;
        this.storage = client.storageTokens || new storage_1.MemoryStorage();
        this.token = this.storage.getItem(this.STORAGE_ACCESS_TOKEN_KEY);
        this.refreshToken = this.storage.getItem(this.STORAGE_REFRESH_TOKEN_KEY);
    }
    getToken() {
        return this.storage.getItem(this.STORAGE_ACCESS_TOKEN_KEY);
    }
    getCurrentUser() {
        return this.currentUser;
    }
    async login(email, password) {
        const response = await this.fetch("/v1/auth/login", {
            method: "POST", body: JSON.stringify({ email, password })
        });
        this.token = response.accessToken;
        this.storage.setItem(this.STORAGE_ACCESS_TOKEN_KEY, response.accessToken);
        this.client.emit("login", response);
        return response;
    }
    async register(email, password, role = "DEVELOPER") {
        const response = await this.fetch("/v1/auth/register", {
            method: "POST",
            body: JSON.stringify({ email, password, role })
        });
        this.currentUser = response;
        return response;
    }
    async logout() {
        await this.fetch("/v1/auth/logout", { method: "POST" });
        this.currentUser = null;
        this.token = null;
        this.storage.removeItem(this.STORAGE_ACCESS_TOKEN_KEY);
        this.refreshToken = null;
        this.storage.removeItem(this.STORAGE_REFRESH_TOKEN_KEY);
        this.client.emit("logout");
    }
    async refresh() {
        const headers = {};
        if (this.refreshToken) {
            headers["Cookie"] = `fb_refresh_token=${this.refreshToken}`;
        }
        const res = await this.fetch("/v1/auth/refresh", {
            method: "POST",
            headers
        });
        this.token = res.accessToken;
        this.storage.setItem(this.STORAGE_ACCESS_TOKEN_KEY, res.accessToken);
        return res;
    }
    getRefreshToken() {
        return this.refreshToken;
    }
    setRefreshToken(token) {
        this.refreshToken = token;
        this.storage.setItem(this.STORAGE_REFRESH_TOKEN_KEY, token);
    }
    async refreshAccessTokenSync() {
        if (this.isRefreshing) {
            return await new Promise((resolve, reject) => {
                this.refreshQueue.push((newToken) => {
                    if (newToken)
                        resolve(newToken);
                    else
                        reject(new Error("Refresh token sync execution failed"));
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
        }
        catch (e) {
            this.refreshQueue.forEach(cb => cb(""));
            this.client.emit("error", e);
            throw e;
        }
        finally {
            this.isRefreshing = false;
            this.refreshQueue = [];
        }
    }
    async fetch(endpoint, options = {}) {
        return await this.client.httpClient.request(endpoint, options);
    }
    async fetchRaw(endpoint, options = {}) {
        try {
            const customFetchEngine = this.client.customFetch || fetch;
            const normalizedEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
            const url = endpoint.startsWith("http") ? endpoint : `${this.client.baseUrl}${normalizedEndpoint}`;
            const headers = {
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
                delete headers["Content-Type"];
            }
            const config = { ...options, headers, credentials: options.credentials || "include" };
            return await customFetchEngine(url, config);
        }
        catch (e) {
            console.log(e);
            throw new errors_1.HttpError(500, "Error during raw fetch!", e);
        }
    }
}
exports.AuthManager = AuthManager;
//# sourceMappingURL=AuthManager.js.map
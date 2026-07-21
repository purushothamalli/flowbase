import type {LoginResponse, User} from "./types";
import type {FlowBaseClient} from "./FlowBaseClient";
import {FlowBaseError, HttpError, NetworkError} from "./errors";

export class AuthManager {
    private token: string | null = null;
    private refreshToken: string | null = null;
    private currentUser: User | null = null;
    private client: FlowBaseClient;

    constructor(client: FlowBaseClient) {
        this.client = client;
    }

    getToken(): string | null {
        return this.token;
    }

    getCurrentUser(): User | null {
        return this.currentUser;
    }

    public async login(email: string, password: string): Promise<LoginResponse> {
        const response = await this.fetch<LoginResponse>("/v1/auth/login", {
            method: "POST", body: JSON.stringify({email, password})
        });
        this.token = response.accessToken;
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

    public async fetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
        const fetchEngine: typeof fetch = this.client.customFetch || fetch;
        const normalizedEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
        const url = endpoint.startsWith("http") ? endpoint : `${this.client.baseUrl}${normalizedEndpoint}`;
        const headers = {...this.getAuthHeaders(), ...(options.headers || {})};
        const config = {...options, headers, credentials: options.credentials || "include"};
        let response;
        try {
            response = await fetchEngine(url, config);
        } catch (err) {
            if (err instanceof FlowBaseError) throw err;
            throw new NetworkError((err as Error).message);
        }
        if (!response.ok) {
            let errorBody: any;
            try {
                errorBody = await response.json();
            } catch {
                errorBody = await response.text();
            }
            throw new HttpError(response.status, response.statusText, errorBody);
        }
        const cookieHeader = response.headers.get("set-cookie");
        if (cookieHeader) {
            const match = cookieHeader.match(/fb_refresh_token=([^;]+)/);
            if (match) { // @ts-ignore
                this.refreshToken = match[1];
            }
        }
        if (response.status == 204) return null as T;
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
            return (await response.json()) as T;
        }
        return (await response.text()) as unknown as T;
    }
}
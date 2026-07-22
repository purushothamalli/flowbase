import type { LoginResponse, RequestOptions, User } from "./types";
import type { FlowBaseClient } from "./FlowBaseClient";
export declare class AuthManager {
    private token;
    private refreshToken;
    private currentUser;
    private client;
    private storage;
    private readonly STORAGE_ACCESS_TOKEN_KEY;
    private readonly STORAGE_REFRESH_TOKEN_KEY;
    private isRefreshing;
    private refreshQueue;
    constructor(client: FlowBaseClient);
    getToken(): string | null;
    getCurrentUser(): User | null;
    login(email: string, password: string): Promise<LoginResponse>;
    register(email: string, password: string, role?: string): Promise<User>;
    logout(): Promise<void>;
    refresh(): Promise<LoginResponse>;
    getRefreshToken(): string | null;
    setRefreshToken(token: string): void;
    refreshAccessTokenSync(): Promise<string>;
    fetch<T>(endpoint: string, options?: RequestOptions): Promise<T>;
    fetchRaw(endpoint: string, options?: RequestOptions): Promise<Response>;
}
//# sourceMappingURL=AuthManager.d.ts.map
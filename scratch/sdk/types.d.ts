export interface ClientConfig {
    apiKey: string;
    baseUrl?: string;
    customFetch?: typeof fetch;
    storage?: TokenStorage;
    retry?: retryConfig;
    timeoutMs?: number;
}
export interface User {
    id: string;
    email: string;
    role: "DEVELOPER" | "ADMIN";
}
export interface LoginResponse {
    accessToken: string;
    tokenType: string;
    expiresInSeconds: number;
}
export interface RealtimeEvent<T = any> {
    event: "insert" | "update" | "delete";
    collectionId: string;
    data: T;
}
export interface RequestContext {
    url: string;
    options: RequestInit;
}
export interface ResponseContext {
    response: Response;
    data?: any;
}
export type NextFunction = () => Promise<ResponseContext>;
export type Middleware = (context: RequestContext, next: NextFunction) => Promise<ResponseContext>;
export interface TokenStorage {
    getItem(key: string): string | null;
    setItem(key: string, value: string): void;
    removeItem(key: string): void;
}
export interface retryConfig {
    maxRetries?: number;
    retryStatusCodes?: number[];
}
export interface RequestOptions extends RequestInit {
    timeoutMs?: number;
    retry?: retryConfig;
}
export type FileMetadataResponse = {
    id: string;
    tenantId: string;
    uploaderId: string;
    filename: string;
    contentType: string;
    sizeBytes: number;
    createdAt: Date;
};
export interface OutboxResponse {
    id: string;
    tenantId: string;
    eventType: string;
    payload: string;
    status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
    retryCount: number;
    maxRetries: number;
    errorMessage: string;
    leasedUntil: string | null;
    createdAt: string;
    updatedAt: string;
}
//# sourceMappingURL=types.d.ts.map
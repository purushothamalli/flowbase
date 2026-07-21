export interface ClientConfig {
    apiKey: string;
    baseUrl?: string;
    customFetch?: typeof fetch;
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
    event: "insert" | "update" | "delete",
    collectionId: string,
    data: T
}
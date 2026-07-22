export declare class FlowBaseError extends Error {
    constructor(message: string);
}
export declare class HttpError extends FlowBaseError {
    readonly status: number;
    readonly statusText: string;
    readonly body: any;
    constructor(status: number, statusText: string, body: any);
}
export declare class NetworkError extends FlowBaseError {
    constructor(message: string);
}
export declare class RateLimitError extends HttpError {
    readonly retryAfterSeconds: number;
    constructor(retryAfterSeconds: number, message: string, body?: any);
}
//# sourceMappingURL=errors.d.ts.map
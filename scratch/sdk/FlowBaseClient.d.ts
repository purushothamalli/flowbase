import { ClientConfig, Middleware, retryConfig, TokenStorage } from "./types";
import { AuthManager } from "./AuthManager";
import { CollectionScope } from "./CollectionScope";
import { RealtimeClient } from "./RealtimeClient";
import { StorageScope } from "./storageScope";
import { JobScope } from "./JobScope";
import { HttpClient } from "./HttpClient";
import { QueryCache } from "./QueryCache";
import { Logger } from "./Logger";
export interface FlowBasePlugin {
    name: string;
    initialize(client: FlowBaseClient): void;
}
export declare class FlowBaseClient {
    readonly apiKey: string;
    readonly baseUrl: string;
    readonly customFetch: typeof fetch;
    readonly auth: AuthManager;
    readonly realtime: RealtimeClient;
    readonly httpClient: HttpClient;
    readonly queryCache: QueryCache;
    readonly logger: Logger;
    private middlewares;
    private listeners;
    readonly storageTokens: TokenStorage;
    readonly timeoutMs?: number;
    readonly retryConfig: retryConfig;
    readonly storage: StorageScope;
    readonly jobs: JobScope;
    collection<T = any>(collectionId: string): CollectionScope<T>;
    use(middleware: Middleware): this;
    on(event: string, callback: (...args: any[]) => void): this;
    emit(event: string, ...args: any[]): void;
    registerPlugin(plugin: FlowBasePlugin): this;
    getMiddlewares(): Middleware[];
    constructor(config: ClientConfig);
}
//# sourceMappingURL=FlowBaseClient.d.ts.map
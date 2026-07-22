import { ClientConfig, Middleware, retryConfig, TokenStorage } from "./types";
import { AuthManager } from "./AuthManager";
import { CollectionScope } from "./CollectionScope";
import { RealtimeClient } from "./RealtimeClient";
import { StorageScope } from "./storageScope";
import { JobScope } from "./JobScope";
import { HttpClient } from "./HttpClient";
export declare class FlowBaseClient {
    readonly apiKey: string;
    readonly baseUrl: string;
    readonly customFetch: typeof fetch;
    readonly auth: AuthManager;
    readonly realtime: RealtimeClient;
    readonly httpClient: HttpClient;
    private middlewares;
    readonly storageTokens: TokenStorage;
    readonly timeoutMs?: number;
    readonly retryConfig: retryConfig;
    readonly storage: StorageScope;
    readonly jobs: JobScope;
    collection<T = any>(collectionId: string): CollectionScope<T>;
    use(middleware: Middleware): this;
    getMiddlewares(): Middleware[];
    constructor(config: ClientConfig);
}
//# sourceMappingURL=FlowBaseClient.d.ts.map
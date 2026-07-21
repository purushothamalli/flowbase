import {ClientConfig, Middleware, retryConfig, TokenStorage} from "./types";
import {AuthManager} from "./AuthManager";
import {FlowBaseError} from "./errors";
import {CollectionScope} from "./CollectionScope";
import {RealtimeClient} from "./RealtimeClient";
import {MemoryStorage} from "./storage";

export class FlowBaseClient {
    public readonly apiKey: string;
    public readonly baseUrl: string;
    public readonly customFetch: typeof fetch;
    public readonly auth: AuthManager;
    public readonly realtime: RealtimeClient;
    private middlewares: Middleware[] = [];
    public readonly storage: TokenStorage;
    public readonly timeoutMs?: number;
    public readonly retryConfig: retryConfig;

    public collection<T = any>(collectionId: string): CollectionScope<T> {
        return new CollectionScope<T>(collectionId, this.auth, this.realtime);
    }

    public use(middleware: Middleware): this {
        this.middlewares.push(middleware);
        return this;
    }

    public getMiddlewares(): Middleware[] {
        return [...this.middlewares];
    }


    constructor(config: ClientConfig) {
        if (config.apiKey == null || config.apiKey.trim().length === 0) throw new FlowBaseError("apiKey should not be empty or null!");
        this.apiKey = config.apiKey;
        this.baseUrl = (config.baseUrl || "http://localhost:8080").replace(/\/+$/, "");
        this.storage = config.storage || new MemoryStorage();
        this.timeoutMs = config.timeoutMs || 10000;
        this.retryConfig = config.retry || { maxRetries: 3, retryStatusCodes: [429, 502, 503, 504] };
        this.auth = new AuthManager(this);
        this.customFetch = config.customFetch || fetch;
        this.realtime = new RealtimeClient(this);
    }
}
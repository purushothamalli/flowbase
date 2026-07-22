import {ClientConfig, Middleware, retryConfig, TokenStorage} from "./types";
import {AuthManager} from "./AuthManager";
import {FlowBaseError} from "./errors";
import {CollectionScope} from "./CollectionScope";
import {RealtimeClient} from "./RealtimeClient";
import {MemoryStorage} from "./storage";
import {StorageScope} from "./storageScope";
import {JobScope} from "./JobScope";
import {HttpClient} from "./HttpClient";

export class FlowBaseClient {
    public readonly apiKey: string;
    public readonly baseUrl: string;
    public readonly customFetch: typeof fetch;
    public readonly auth: AuthManager;
    public readonly realtime: RealtimeClient;
    public readonly httpClient: HttpClient;
    private middlewares: Middleware[] = [];
    public readonly storageTokens: TokenStorage;
    public readonly timeoutMs?: number;
    public readonly retryConfig: retryConfig;
    public readonly storage: StorageScope;
    public readonly jobs: JobScope;

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
        this.storageTokens = config.storage || new MemoryStorage();
        this.timeoutMs = config.timeoutMs || 10000;
        this.retryConfig = config.retry || {maxRetries: 3, retryStatusCodes: [429, 502, 503, 504]};
        this.httpClient = new HttpClient(this);
        this.auth = new AuthManager(this);
        this.storage = new StorageScope(this.auth);
        this.jobs = new JobScope(this.auth);
        this.customFetch = config.customFetch || fetch;
        this.realtime = new RealtimeClient(this);
        this.use(async (request, next) => {
            if (!request.options.headers || !(request.options.headers as any)["traceparent"]) {
                const traceId = Array.from({length: 32}, () => Math.floor(Math.random() * 16).toString(16)).join("");
                const spanId = Array.from({length: 16}, () => Math.floor(Math.random() * 16).toString(16)).join("");
                const traceparent = `00-${traceId}-${spanId}-01`;
                request.options.headers = {
                    ...request.options.headers, traceparent
                }
            }
            return await next();
        });
    }
}
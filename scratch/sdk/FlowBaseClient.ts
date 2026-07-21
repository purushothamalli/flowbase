import {ClientConfig} from "./types";
import {AuthManager} from "./AuthManager";
import {FlowBaseError} from "./errors";
import {CollectionScope} from "./CollectionScope";
import {RealtimeClient} from "./RealtimeClient";

export class FlowBaseClient {
    public readonly apiKey: string;
    public readonly baseUrl: string;
    public readonly customFetch: typeof fetch;
    public readonly auth: AuthManager;
    public readonly realtime: RealtimeClient;

    public collection<T = any>(collectionId: string): CollectionScope<T> {
        return new CollectionScope<T>(collectionId, this.auth, this.realtime);
    }

    constructor(config: ClientConfig) {
        if (config.apiKey == null || config.apiKey.trim().length === 0) throw new FlowBaseError("apiKey should not be empty or null!");
        this.apiKey = config.apiKey;
        this.baseUrl = (config.baseUrl || "http://localhost:8080").replace(/\/+$/, "");
        this.auth = new AuthManager(this);
        this.customFetch = config.customFetch || fetch;
        this.realtime = new RealtimeClient(this);
    }
}
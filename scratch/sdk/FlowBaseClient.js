"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FlowBaseClient = void 0;
const AuthManager_1 = require("./AuthManager");
const errors_1 = require("./errors");
const CollectionScope_1 = require("./CollectionScope");
const RealtimeClient_1 = require("./RealtimeClient");
const storage_1 = require("./storage");
const storageScope_1 = require("./storageScope");
const JobScope_1 = require("./JobScope");
const HttpClient_1 = require("./HttpClient");
const QueryCache_1 = require("./QueryCache");
const Logger_1 = require("./Logger");
class FlowBaseClient {
    apiKey;
    baseUrl;
    customFetch;
    auth;
    realtime;
    httpClient;
    queryCache;
    logger;
    middlewares = [];
    listeners = {};
    storageTokens;
    timeoutMs;
    retryConfig;
    storage;
    jobs;
    collection(collectionId) {
        return new CollectionScope_1.CollectionScope(collectionId, this.auth, this.realtime);
    }
    use(middleware) {
        this.middlewares.push(middleware);
        return this;
    }
    on(event, callback) {
        if (!this.listeners[event])
            this.listeners[event] = [];
        this.listeners[event].push(callback);
        return this;
    }
    emit(event, ...args) {
        const list = this.listeners[event];
        if (list) {
            list.forEach(cb => cb(...args));
        }
    }
    registerPlugin(plugin) {
        plugin.initialize(this);
        return this;
    }
    getMiddlewares() {
        return [...this.middlewares];
    }
    constructor(config) {
        if (config.apiKey == null || config.apiKey.trim().length === 0)
            throw new errors_1.FlowBaseError("apiKey should not be empty or null!");
        this.apiKey = config.apiKey;
        this.baseUrl = (config.baseUrl || "http://localhost:8080").replace(/\/+$/, "");
        this.storageTokens = config.storage || new storage_1.MemoryStorage();
        this.timeoutMs = config.timeoutMs || 10000;
        this.retryConfig = config.retry || { maxRetries: 3, retryStatusCodes: [429, 502, 503, 504] };
        this.httpClient = new HttpClient_1.HttpClient(this);
        this.queryCache = new QueryCache_1.QueryCache();
        this.logger = config.logger || new Logger_1.ConsoleLogger();
        this.auth = new AuthManager_1.AuthManager(this);
        this.storage = new storageScope_1.StorageScope(this.auth);
        this.jobs = new JobScope_1.JobScope(this.auth);
        this.customFetch = config.customFetch || fetch;
        this.realtime = new RealtimeClient_1.RealtimeClient(this);
        this.use(async (request, next) => {
            if (!request.options.headers || !request.options.headers["traceparent"]) {
                const traceId = Array.from({ length: 32 }, () => Math.floor(Math.random() * 16).toString(16)).join("");
                const spanId = Array.from({ length: 16 }, () => Math.floor(Math.random() * 16).toString(16)).join("");
                const traceparent = `00-${traceId}-${spanId}-01`;
                request.options.headers = {
                    ...request.options.headers, traceparent
                };
            }
            return await next();
        });
    }
}
exports.FlowBaseClient = FlowBaseClient;
//# sourceMappingURL=FlowBaseClient.js.map
export interface CacheEntry<T> {
    data: T;
    expiresAt: number;
    lastAccessed: number;
}
export declare class QueryCache {
    private cache;
    private readonly maxSize;
    get<T>(key: string): T | null;
    set<T>(key: string, data: T, ttlSeconds: number): void;
    clear(): void;
}
//# sourceMappingURL=QueryCache.d.ts.map
export interface CacheEntry<T> {
    data: T;
    expiresAt: number;
    lastAccessed: number;
}

export class QueryCache {
    private cache = new Map<string, CacheEntry<any>>();
    private readonly maxSize = 100;

    public get<T>(key: string): T | null {
        const entry = this.cache.get(key);
        if (!entry) return null;
        if (Date.now() > entry.expiresAt) {
            this.cache.delete(key);
            return null;
        }
        entry.lastAccessed = Date.now();
        return entry.data as T;
    }

    public set<T>(key: string, data: T, ttlSeconds: number): void {
        if (this.cache.size >= this.maxSize) {
            // Evict least recently used entry
            let oldestKey: string | null = null;
            let oldestTime = Infinity;
            for (const [k, entry] of this.cache.entries()) {
                if (entry.lastAccessed < oldestTime) {
                    oldestTime = entry.lastAccessed;
                    oldestKey = k;
                }
            }
            if (oldestKey) this.cache.delete(oldestKey);
        }
        this.cache.set(key, {
            data,
            expiresAt: Date.now() + (ttlSeconds * 1000),
            lastAccessed: Date.now()
        });
    }

    public clear(): void {
        this.cache.clear();
    }
}

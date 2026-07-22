"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.QueryCache = void 0;
class QueryCache {
    cache = new Map();
    maxSize = 100;
    get(key) {
        const entry = this.cache.get(key);
        if (!entry)
            return null;
        if (Date.now() > entry.expiresAt) {
            this.cache.delete(key);
            return null;
        }
        entry.lastAccessed = Date.now();
        return entry.data;
    }
    set(key, data, ttlSeconds) {
        if (this.cache.size >= this.maxSize) {
            // Evict least recently used entry
            let oldestKey = null;
            let oldestTime = Infinity;
            for (const [k, entry] of this.cache.entries()) {
                if (entry.lastAccessed < oldestTime) {
                    oldestTime = entry.lastAccessed;
                    oldestKey = k;
                }
            }
            if (oldestKey)
                this.cache.delete(oldestKey);
        }
        this.cache.set(key, {
            data,
            expiresAt: Date.now() + (ttlSeconds * 1000),
            lastAccessed: Date.now()
        });
    }
    clear() {
        this.cache.clear();
    }
}
exports.QueryCache = QueryCache;
//# sourceMappingURL=QueryCache.js.map
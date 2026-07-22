"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.QueryFieldClause = exports.QueryBuilder = void 0;
class QueryBuilder {
    collectionId;
    authManager;
    params = new URLSearchParams();
    limitCount = 20;
    offsetCount = 0;
    cacheTtl = 0;
    constructor(collectionId, authManager) {
        this.collectionId = collectionId;
        this.authManager = authManager;
    }
    cache(ttlSeconds) {
        this.cacheTtl = ttlSeconds;
        return this;
    }
    where(field) {
        return new QueryFieldClause(this, field);
    }
    whereEquals(field, value) {
        this.params.append(`${String(field)}[eq]`, String(value));
        return this;
    }
    whereGreaterThan(field, value) {
        this.params.append(`${String(field)}[gt]`, String(value));
        return this;
    }
    whereLessThan(field, value) {
        this.params.append(`${String(field)}[lt]`, String(value));
        return this;
    }
    whereIn(field, values) {
        this.params.append(`${String(field)}[in]`, values.map(v => String(v)).join(","));
        return this;
    }
    whereNotIn(field, values) {
        this.params.append(`${String(field)}[notin]`, values.map(v => String(v)).join(","));
        return this;
    }
    whereBetween(field, min, max) {
        this.params.append(`${String(field)}[gte]`, String(min));
        this.params.append(`${String(field)}[lte]`, String(max));
        return this;
    }
    whereStartsWith(field, value) {
        this.params.append(`${String(field)}[starts_with]`, value);
        return this;
    }
    whereEndsWith(field, value) {
        this.params.append(`${String(field)}[ends_with]`, value);
        return this;
    }
    whereContains(field, value) {
        this.params.append(`${String(field)}[contains]`, value);
        return this;
    }
    sortBy(field, direction = "asc") {
        this.params.set("_sort", direction === "desc" ? `-${String(field)}` : String(field));
        return this;
    }
    offset(offsetCount) {
        this.offsetCount = offsetCount;
        this.params.set("_offset", String(offsetCount));
        return this;
    }
    limit(pageSize) {
        this.limitCount = pageSize;
        this.params.set("_limit", String(pageSize));
        return this;
    }
    async nextPage() {
        this.offsetCount += this.limitCount;
        this.params.set("_offset", String(this.offsetCount));
        return await this.find();
    }
    async previousPage() {
        this.offsetCount = Math.max(0, this.offsetCount - this.limitCount);
        this.params.set("_offset", String(this.offsetCount));
        return await this.find();
    }
    search(query) {
        this.params.set("q", String(query));
        return this;
    }
    async count() {
        const docs = await this.find();
        return docs.length;
    }
    async sum(field) {
        const docs = await this.find();
        return docs.reduce((acc, doc) => {
            const val = doc.data?.[field];
            return acc + (Number(val) || 0);
        }, 0);
    }
    async avg(field) {
        const docs = await this.find();
        if (docs.length === 0)
            return 0;
        const total = docs.reduce((acc, doc) => {
            const val = doc.data?.[field];
            return acc + (Number(val) || 0);
        }, 0);
        return total / docs.length;
    }
    async min(field) {
        const docs = await this.find();
        if (docs.length === 0)
            return 0;
        const vals = docs.map(doc => Number(doc.data?.[field]) || 0);
        return Math.min(...vals);
    }
    async max(field) {
        const docs = await this.find();
        if (docs.length === 0)
            return 0;
        const vals = docs.map(doc => Number(doc.data?.[field]) || 0);
        return Math.max(...vals);
    }
    async find() {
        const queryString = this.params.toString();
        const uri = queryString
            ? `/v1/data/${this.collectionId}?${queryString}` : `/v1/data/${this.collectionId}`;
        if (this.cacheTtl > 0) {
            const client = this.authManager.client;
            if (client && client.queryCache) {
                const cached = client.queryCache.get(uri);
                if (cached)
                    return cached;
            }
        }
        const res = await this.authManager.fetch(uri);
        if (this.cacheTtl > 0) {
            const client = this.authManager.client;
            if (client && client.queryCache) {
                client.queryCache.set(uri, res, this.cacheTtl);
            }
        }
        return res;
    }
}
exports.QueryBuilder = QueryBuilder;
class QueryFieldClause {
    builder;
    field;
    constructor(builder, field) {
        this.builder = builder;
        this.field = field;
    }
    equals(value) {
        return this.builder.whereEquals(this.field, value);
    }
    greaterThan(value) {
        return this.builder.whereGreaterThan(this.field, value);
    }
    lessThan(value) {
        return this.builder.whereLessThan(this.field, value);
    }
}
exports.QueryFieldClause = QueryFieldClause;
//# sourceMappingURL=QueryBuilder.js.map
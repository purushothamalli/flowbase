import {AuthManager} from "./AuthManager";

export class QueryBuilder<T> {
    private params: URLSearchParams = new URLSearchParams();
    private limitCount: number = 20;
    private offsetCount: number = 0;
    private cacheTtl: number = 0;

    constructor(private readonly collectionId: string, private readonly authManager: AuthManager) {
    }

    cache(ttlSeconds: number): this {
        this.cacheTtl = ttlSeconds;
        return this;
    }

    where(field: keyof T): QueryFieldClause<T> {
        return new QueryFieldClause<T>(this, field);
    }

    whereEquals(field: keyof T, value: any): this {
        this.params.append(`${String(field)}[eq]`, String(value));
        return this;
    }

    whereGreaterThan(field: keyof T, value: any): this {
        this.params.append(`${String(field)}[gt]`, String(value));
        return this;
    }

    whereLessThan(field: keyof T, value: any): this {
        this.params.append(`${String(field)}[lt]`, String(value));
        return this;
    }

    whereIn(field: keyof T, values: any[]): this {
        this.params.append(`${String(field)}[in]`, values.map(v => String(v)).join(","));
        return this;
    }

    whereNotIn(field: keyof T, values: any[]): this {
        this.params.append(`${String(field)}[notin]`, values.map(v => String(v)).join(","));
        return this;
    }

    whereBetween(field: keyof T, min: any, max: any): this {
        this.params.append(`${String(field)}[gte]`, String(min));
        this.params.append(`${String(field)}[lte]`, String(max));
        return this;
    }

    whereStartsWith(field: keyof T, value: string): this {
        this.params.append(`${String(field)}[starts_with]`, value);
        return this;
    }

    whereEndsWith(field: keyof T, value: string): this {
        this.params.append(`${String(field)}[ends_with]`, value);
        return this;
    }

    whereContains(field: keyof T, value: string): this {
        this.params.append(`${String(field)}[contains]`, value);
        return this;
    }

    sortBy(field: keyof T, direction: "asc" | "desc" = "asc"): this {
        this.params.set("_sort", direction === "desc" ? `-${String(field)}` : String(field));
        return this;
    }

    offset(offsetCount: number): this {
        this.offsetCount = offsetCount;
        this.params.set("_offset", String(offsetCount));
        return this;
    }

    limit(pageSize: number): this {
        this.limitCount = pageSize;
        this.params.set("_limit", String(pageSize));
        return this;
    }

    public async nextPage(): Promise<T[]> {
        this.offsetCount += this.limitCount;
        this.params.set("_offset", String(this.offsetCount));
        return await this.find();
    }

    public async previousPage(): Promise<T[]> {
        this.offsetCount = Math.max(0, this.offsetCount - this.limitCount);
        this.params.set("_offset", String(this.offsetCount));
        return await this.find();
    }

    search(query: string): this {
        this.params.set("q", String(query));
        return this;
    }

    public async count(): Promise<number> {
        const docs = await this.find();
        return docs.length;
    }

    public async sum(field: keyof T): Promise<number> {
        const docs = await this.find();
        return docs.reduce((acc, doc) => {
            const val = (doc as any).data?.[field];
            return acc + (Number(val) || 0);
        }, 0);
    }

    public async avg(field: keyof T): Promise<number> {
        const docs = await this.find();
        if (docs.length === 0) return 0;
        const total = docs.reduce((acc, doc) => {
            const val = (doc as any).data?.[field];
            return acc + (Number(val) || 0);
        }, 0);
        return total / docs.length;
    }

    public async min(field: keyof T): Promise<number> {
        const docs = await this.find();
        if (docs.length === 0) return 0;
        const vals = docs.map(doc => Number((doc as any).data?.[field]) || 0);
        return Math.min(...vals);
    }

    public async max(field: keyof T): Promise<number> {
        const docs = await this.find();
        if (docs.length === 0) return 0;
        const vals = docs.map(doc => Number((doc as any).data?.[field]) || 0);
        return Math.max(...vals);
    }

    public async find(): Promise<T[]> {
        const queryString = this.params.toString();
        const uri = queryString
            ? `/v1/data/${this.collectionId}?${queryString}` : `/v1/data/${this.collectionId}`;
        
        if (this.cacheTtl > 0) {
            const client = (this.authManager as any).client;
            if (client && client.queryCache) {
                const cached = client.queryCache.get(uri);
                if (cached) return cached;
            }
        }

        const res = await this.authManager.fetch<T[]>(uri);

        if (this.cacheTtl > 0) {
            const client = (this.authManager as any).client;
            if (client && client.queryCache) {
                client.queryCache.set(uri, res, this.cacheTtl);
            }
        }

        return res;
    }
}

export class QueryFieldClause<T> {
    constructor(private builder: QueryBuilder<T>, private field: keyof T) {
    }

    equals(value: any): QueryBuilder<T> {
        return this.builder.whereEquals(this.field, value);
    }

    greaterThan(value: number): QueryBuilder<T> {
        return this.builder.whereGreaterThan(this.field, value);
    }

    lessThan(value: number): QueryBuilder<T> {
        return this.builder.whereLessThan(this.field, value);
    }
}
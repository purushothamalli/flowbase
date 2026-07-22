import { AuthManager } from "./AuthManager";
export declare class QueryBuilder<T> {
    private readonly collectionId;
    private readonly authManager;
    private params;
    private limitCount;
    private offsetCount;
    private cacheTtl;
    constructor(collectionId: string, authManager: AuthManager);
    cache(ttlSeconds: number): this;
    where(field: keyof T): QueryFieldClause<T>;
    whereEquals(field: keyof T, value: any): this;
    whereGreaterThan(field: keyof T, value: any): this;
    whereLessThan(field: keyof T, value: any): this;
    whereIn(field: keyof T, values: any[]): this;
    whereNotIn(field: keyof T, values: any[]): this;
    whereBetween(field: keyof T, min: any, max: any): this;
    whereStartsWith(field: keyof T, value: string): this;
    whereEndsWith(field: keyof T, value: string): this;
    whereContains(field: keyof T, value: string): this;
    sortBy(field: keyof T, direction?: "asc" | "desc"): this;
    offset(offsetCount: number): this;
    limit(pageSize: number): this;
    nextPage(): Promise<T[]>;
    previousPage(): Promise<T[]>;
    search(query: string): this;
    count(): Promise<number>;
    sum(field: keyof T): Promise<number>;
    avg(field: keyof T): Promise<number>;
    min(field: keyof T): Promise<number>;
    max(field: keyof T): Promise<number>;
    find(): Promise<T[]>;
}
export declare class QueryFieldClause<T> {
    private builder;
    private field;
    constructor(builder: QueryBuilder<T>, field: keyof T);
    equals(value: any): QueryBuilder<T>;
    greaterThan(value: number): QueryBuilder<T>;
    lessThan(value: number): QueryBuilder<T>;
}
//# sourceMappingURL=QueryBuilder.d.ts.map
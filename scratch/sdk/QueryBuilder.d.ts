import { AuthManager } from "./AuthManager";
export declare class QueryBuilder<T> {
    private readonly collectionId;
    private readonly authManager;
    private params;
    constructor(collectionId: string, authManager: AuthManager);
    where(field: keyof T): QueryFieldClause<T>;
    whereEquals(field: keyof T, value: any): this;
    whereGreaterThan(field: keyof T, value: any): this;
    whereLessThan(field: keyof T, value: any): this;
    sortBy(field: keyof T, direction?: "asc" | "desc"): this;
    offset(offsetCount: number): this;
    limit(pageSize: number): this;
    search(query: string): this;
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
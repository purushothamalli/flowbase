import {AuthManager} from "./AuthManager";

export class QueryBuilder<T> {
    private params: URLSearchParams = new URLSearchParams();

    constructor(private readonly collectionId: string, private readonly authManager: AuthManager) {
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

    sortBy(field: keyof T, direction: "asc" | "desc" = "asc"): this {
        this.params.append("_sort", direction === "desc" ? `-${String(field)}` : String(field));
        return this;
    }

    offset(offsetCount: number): this {
        this.params.append("_offset", String(offsetCount));
        return this;
    }

    limit(pageSize: number): this {
        this.params.append("_limit", String(pageSize));
        return this;
    }

    search(query: string): this {
        this.params.append("q", String(query));
        return this;
    }

    public async find(): Promise<T[]> {
        const queryString = this.params.toString();
        const uri = queryString
            ? `/v1/data/${this.collectionId}?${queryString}` : `/v1/data/${this.collectionId}`;
        return await this.authManager.fetch<T[]>(uri);
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
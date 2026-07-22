"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.QueryFieldClause = exports.QueryBuilder = void 0;
class QueryBuilder {
    collectionId;
    authManager;
    params = new URLSearchParams();
    constructor(collectionId, authManager) {
        this.collectionId = collectionId;
        this.authManager = authManager;
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
    sortBy(field, direction = "asc") {
        this.params.append("_sort", direction === "desc" ? `-${String(field)}` : String(field));
        return this;
    }
    offset(offsetCount) {
        this.params.append("_offset", String(offsetCount));
        return this;
    }
    limit(pageSize) {
        this.params.append("_limit", String(pageSize));
        return this;
    }
    search(query) {
        this.params.append("q", String(query));
        return this;
    }
    async find() {
        const queryString = this.params.toString();
        const uri = queryString
            ? `/v1/data/${this.collectionId}?${queryString}` : `/v1/data/${this.collectionId}`;
        return await this.authManager.fetch(uri);
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
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CollectionScope = void 0;
const QueryBuilder_1 = require("./QueryBuilder");
class CollectionScope {
    collectionId;
    authManager;
    realtime;
    constructor(collectionId, authManager, realtime) {
        this.collectionId = collectionId;
        this.authManager = authManager;
        this.realtime = realtime;
    }
    query() {
        return new QueryBuilder_1.QueryBuilder(this.collectionId, this.authManager);
    }
    async findById(id) {
        return await this.authManager.fetch(`/v1/data/${this.collectionId}/${id}`);
    }
    async insert(data) {
        return await this.authManager.fetch(`/v1/data/${this.collectionId}`, {
            method: "POST",
            body: JSON.stringify(data)
        });
    }
    async update(id, data) {
        return await this.authManager.fetch(`/v1/data/${this.collectionId}/${id}`, {
            method: "PATCH",
            body: JSON.stringify(data)
        });
    }
    async delete(id) {
        return this.authManager.fetch(`/v1/data/${this.collectionId}/${id}`, { method: "DELETE" });
    }
    subscribe(callback) {
        return this.realtime.subscribe(this.collectionId, callback);
    }
}
exports.CollectionScope = CollectionScope;
//# sourceMappingURL=CollectionScope.js.map
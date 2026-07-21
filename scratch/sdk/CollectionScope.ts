import {AuthManager} from "./AuthManager";
import {QueryBuilder} from "./QueryBuilder";
import {RealtimeEvent} from "./types";
import {RealtimeClient} from "./RealtimeClient";

export class CollectionScope<T> {
    constructor(private collectionId: string, private authManager: AuthManager, private realtime: RealtimeClient) {
    }

    query(): QueryBuilder<T> {
        return new QueryBuilder<T>(this.collectionId, this.authManager);
    }

    public async findById(id: string): Promise<T> {
        return await this.authManager.fetch<T>(`/v1/data/${this.collectionId}/${id}`);
    }

    public async insert(data: Partial<T>): Promise<T> {
        return await this.authManager.fetch<T>(`/v1/data/${this.collectionId}`, {
            method: "POST",
            body: JSON.stringify(data)
        });
    }

    public async update(id: string, data: Partial<T>): Promise<T> {
        return await this.authManager.fetch<T>(`/v1/data/${this.collectionId}/${id}`, {
            method: "PATCH",
            body: JSON.stringify(data)
        });
    }

    public async delete(id: string): Promise<void> {
        return this.authManager.fetch<void>(`/v1/data/${this.collectionId}/${id}`, {method: "DELETE"});
    }

    public subscribe(callback: (event: RealtimeEvent<T>) => void): () => void {
        return this.realtime.subscribe<T>(this.collectionId, callback);
    }
}
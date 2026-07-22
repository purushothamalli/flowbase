import { AuthManager } from "./AuthManager";
import { QueryBuilder } from "./QueryBuilder";
import { RealtimeEvent } from "./types";
import { RealtimeClient } from "./RealtimeClient";
export declare class CollectionScope<T> {
    private collectionId;
    private authManager;
    private realtime;
    constructor(collectionId: string, authManager: AuthManager, realtime: RealtimeClient);
    query(): QueryBuilder<T>;
    findById(id: string): Promise<T>;
    insert(data: Partial<T>): Promise<T>;
    insertMany(documents: Array<Partial<T>>): Promise<T[]>;
    update(id: string, data: Partial<T>): Promise<T>;
    delete(id: string): Promise<void>;
    subscribe(callback: (event: RealtimeEvent<T>) => void): () => void;
}
//# sourceMappingURL=CollectionScope.d.ts.map
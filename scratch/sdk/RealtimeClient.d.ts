import { FlowBaseClient } from "./FlowBaseClient";
import { RealtimeEvent } from "./types";
export declare class RealtimeClient {
    private client;
    private ws;
    private subscriptions;
    private reconnectAttempts;
    private maxReconnectionDelay;
    private isExplicitDisconnect;
    constructor(client: FlowBaseClient);
    private reconnect;
    connect(): void;
    subscribe<T>(collectionId: string, callback: (event: RealtimeEvent<T>) => void): () => void;
    unsubscribe(collectionId: string, callback: Function): void;
    disconnect(): void;
}
//# sourceMappingURL=RealtimeClient.d.ts.map
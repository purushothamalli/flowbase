"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.RealtimeClient = void 0;
class RealtimeClient {
    client;
    ws = null;
    subscriptions = new Map();
    reconnectAttempts = 0;
    maxReconnectionDelay = 30000;
    isExplicitDisconnect = false;
    constructor(client) {
        this.client = client;
    }
    reconnect() {
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts++), this.maxReconnectionDelay);
        setTimeout(this.connect, delay);
    }
    connect() {
        if (this.ws?.readyState === WebSocket.CONNECTING || this.ws?.readyState === WebSocket.OPEN)
            return;
        const wsUrl = this.client.baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/v1/realtime";
        this.ws = new WebSocket(wsUrl);
        this.ws.addEventListener("open", (event) => {
            this.reconnectAttempts = 0;
            if (this.client.auth.getToken() != null) {
                this.ws?.send(JSON.stringify({ action: "authenticate", token: this.client.auth.getToken() }));
            }
            this.subscriptions.keys().forEach(key => this.ws?.send(JSON.stringify({
                action: "subscribe",
                collectionId: key
            })));
        });
        this.ws.addEventListener("message", (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data?.event && data?.collectionId) {
                    this.subscriptions.keys().forEach(key => {
                        if (key === data.collectionId) {
                            this.subscriptions.get(data.collectionId)?.forEach(callback => {
                                const args = {
                                    event: data.event,
                                    collectionId: data.collectionId,
                                    data: data.data
                                };
                                callback(args);
                            });
                        }
                    });
                }
            }
            catch (err) {
                console.error("[Realtime Client] error parsing message: ", err);
            }
        });
        this.ws.addEventListener("close", (event) => {
            if (!this.isExplicitDisconnect) {
                this.reconnect();
            }
        });
    }
    subscribe(collectionId, callback) {
        this.connect();
        if (!this.subscriptions.has(collectionId)) {
            this.subscriptions.set(collectionId, new Set());
            if (this.ws?.readyState === WebSocket.OPEN) {
                this.ws.send(JSON.stringify({
                    action: "subscribe", collectionId: collectionId
                }));
            }
            this.subscriptions.get(collectionId)?.add(callback);
        }
        return () => this.unsubscribe(collectionId, callback);
    }
    unsubscribe(collectionId, callback) {
        const callbacks = this.subscriptions.get(collectionId);
        if (callbacks) {
            callbacks.delete(callback);
            if (callbacks.size === 0) {
                this.subscriptions.delete(collectionId);
                if (this.ws?.readyState === WebSocket.OPEN) {
                    this.ws.send(JSON.stringify({ action: "unsubscribe", collectionId }));
                }
            }
        }
    }
    disconnect() {
        this.isExplicitDisconnect = true;
        this.ws?.close();
        this.subscriptions.clear();
    }
}
exports.RealtimeClient = RealtimeClient;
//# sourceMappingURL=RealtimeClient.js.map
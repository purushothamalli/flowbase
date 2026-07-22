"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SessionStorageAdapter = exports.LocalStorageAdapter = exports.MemoryStorage = void 0;
class MemoryStorage {
    storage = new Map();
    getItem(key) {
        return this.storage.get(key) ?? null;
    }
    setItem(key, value) {
        this.storage.set(key, value);
    }
    removeItem(key) {
        this.storage.delete(key);
    }
}
exports.MemoryStorage = MemoryStorage;
class LocalStorageAdapter {
    getItem(key) {
        return (typeof window !== "undefined" && window.localStorage.getItem(key)) || null;
    }
    removeItem(key) {
        typeof window !== "undefined" && window.localStorage.removeItem(key);
    }
    setItem(key, value) {
        typeof window !== "undefined" && window.localStorage.setItem(key, value);
    }
}
exports.LocalStorageAdapter = LocalStorageAdapter;
class SessionStorageAdapter {
    getItem(key) {
        return (typeof window !== "undefined" && window.sessionStorage.getItem(key)) || null;
    }
    removeItem(key) {
        typeof window !== "undefined" && window.sessionStorage.removeItem(key);
    }
    setItem(key, value) {
        typeof window !== "undefined" && window.sessionStorage.setItem(key, value);
    }
}
exports.SessionStorageAdapter = SessionStorageAdapter;
//# sourceMappingURL=storage.js.map
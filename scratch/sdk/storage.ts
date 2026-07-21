import {TokenStorage} from "./types";

export class MemoryStorage implements TokenStorage {
    private readonly storage: Map<string, string> = new Map();

    getItem(key: string): string | null {
        return this.storage.get(key) ?? null;
    }

    setItem(key: string, value: string) {
        this.storage.set(key, value);
    }

    removeItem(key: string) {
        this.storage.delete(key);
    }
}

export class LocalStorageAdapter implements TokenStorage {
    getItem(key: string): string | null {
        return (typeof window !== "undefined" && window.localStorage.getItem(key)) || null;
    }

    removeItem(key: string): void {
        typeof window !== "undefined" && window.localStorage.removeItem(key);
    }

    setItem(key: string, value: string): void {
        typeof window !== "undefined" && window.localStorage.setItem(key, value);
    }
}

export class SessionStorageAdapter implements TokenStorage {
    getItem(key: string): string | null {
        return (typeof window !== "undefined" && window.sessionStorage.getItem(key)) || null;
    }

    removeItem(key: string): void {
        typeof window !== "undefined" && window.sessionStorage.removeItem(key);
    }

    setItem(key: string, value: string): void {
        typeof window !== "undefined" && window.sessionStorage.setItem(key, value);
    }
}
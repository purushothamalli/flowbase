import { TokenStorage } from "./types";
export declare class MemoryStorage implements TokenStorage {
    private readonly storage;
    getItem(key: string): string | null;
    setItem(key: string, value: string): void;
    removeItem(key: string): void;
}
export declare class LocalStorageAdapter implements TokenStorage {
    getItem(key: string): string | null;
    removeItem(key: string): void;
    setItem(key: string, value: string): void;
}
export declare class SessionStorageAdapter implements TokenStorage {
    getItem(key: string): string | null;
    removeItem(key: string): void;
    setItem(key: string, value: string): void;
}
//# sourceMappingURL=storage.d.ts.map
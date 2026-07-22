import { AuthManager } from "./AuthManager";
import { FileMetadataResponse } from "./types";
export declare class StorageScope {
    private auth;
    constructor(auth: AuthManager);
    upload(file: File | Blob | Buffer, filename?: string): Promise<FileMetadataResponse>;
    getMetadata(fileId: string): Promise<FileMetadataResponse>;
    download(fileId: string): Promise<Blob>;
    delete(fileId: string): Promise<void>;
}
//# sourceMappingURL=storageScope.d.ts.map
import {AuthManager} from "./AuthManager";
import {FileMetadataResponse} from "./types";

export class StorageScope {
    constructor(private auth: AuthManager) {
    }

    public async upload(file: File | Blob | Buffer, filename?: string): Promise<FileMetadataResponse> {
        const formData: FormData = new FormData();
        if (typeof Blob !== "undefined" && file instanceof Blob) {
            formData.append("file", file, filename || (file as File).name || "upload_file");
        } else {
            const blob = new Blob([file as any]);
            formData.append("file", blob, filename || "upload_file");
        }
        return await this.auth.fetch<FileMetadataResponse>("/v1/storage/upload", {
            method: "POST",
            body: formData,
            headers: {}
        });
    }

    public async getMetadata(fileId: string): Promise<FileMetadataResponse> {
        return await this.auth.fetch<FileMetadataResponse>(`/v1/storage/files/${fileId}`);
    }

    public async download(fileId: string): Promise<Blob> {
        const response = await this.auth.fetchRaw(`/v1/storage/files/${fileId}/download`);
        return response.blob();
    }

    public async delete(fileId: string): Promise<void> {
        return await this.auth.fetch<void>(`/v1/storage/files/${fileId}`, {method: "DELETE"});
    }
}
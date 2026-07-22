"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.StorageScope = void 0;
class StorageScope {
    auth;
    constructor(auth) {
        this.auth = auth;
    }
    async upload(file, filename) {
        const formData = new FormData();
        if (typeof Blob !== "undefined" && file instanceof Blob) {
            formData.append("file", file, filename || file.name || "upload_file");
        }
        else {
            const blob = new Blob([file]);
            formData.append("file", blob, filename || "upload_file");
        }
        return await this.auth.fetch("/v1/storage/upload", {
            method: "POST",
            body: formData,
            headers: {}
        });
    }
    async getMetadata(fileId) {
        return await this.auth.fetch(`/v1/storage/files/${fileId}`);
    }
    async download(fileId) {
        const response = await this.auth.fetchRaw(`/v1/storage/files/${fileId}/download`);
        return response.blob();
    }
    async delete(fileId) {
        return await this.auth.fetch(`/v1/storage/files/${fileId}`, { method: "DELETE" });
    }
}
exports.StorageScope = StorageScope;
//# sourceMappingURL=storageScope.js.map
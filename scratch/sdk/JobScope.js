"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JobScope = void 0;
class JobScope {
    auth;
    constructor(auth) {
        this.auth = auth;
    }
    async publish(eventType, payload) {
        return await this.auth.fetch("/v1/jobs/publish", {
            method: "POST",
            body: JSON.stringify({ eventType, payload })
        });
    }
    async listJobs() {
        return await this.auth.fetch("/v1/jobs");
    }
    async getJob(jobId) {
        return await this.auth.fetch(`/v1/jobs/${jobId}`);
    }
    async retry(jobId) {
        return await this.auth.fetch(`/v1/jobs/${jobId}/retry`, { method: "POST" });
    }
}
exports.JobScope = JobScope;
//# sourceMappingURL=JobScope.js.map
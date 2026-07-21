import {AuthManager} from "./AuthManager";
import {OutboxResponse} from "./types";

export class JobScope {
    constructor(private auth: AuthManager) {
    }

    public async publish(eventType: string, payload: any): Promise<OutboxResponse> {
        return await this.auth.fetch<OutboxResponse>("/v1/jobs/publish", {
            method: "POST",
            body: JSON.stringify({eventType, payload})
        });
    }

    public async listJobs(): Promise<OutboxResponse[]> {
        return await this.auth.fetch<OutboxResponse[]>("/v1/jobs");
    }

    public async getJob(jobId: string): Promise<OutboxResponse> {
        return await this.auth.fetch<OutboxResponse>(`/v1/jobs/${jobId}`);
    }

    public async retry(jobId: string): Promise<OutboxResponse> {
        return await this.auth.fetch<OutboxResponse>(`/v1/jobs/${jobId}/retry`, {method: "POST"});
    }
}
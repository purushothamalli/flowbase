import { AuthManager } from "./AuthManager";
import { OutboxResponse } from "./types";
export declare class JobScope {
    private auth;
    constructor(auth: AuthManager);
    publish(eventType: string, payload: any): Promise<OutboxResponse>;
    listJobs(): Promise<OutboxResponse[]>;
    getJob(jobId: string): Promise<OutboxResponse>;
    retry(jobId: string): Promise<OutboxResponse>;
}
//# sourceMappingURL=JobScope.d.ts.map
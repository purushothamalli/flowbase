import { FlowBaseClient } from "./FlowBaseClient";
import { RequestOptions } from "./types";
export declare class HttpClient {
    private client;
    constructor(client: FlowBaseClient);
    private getBaseHeaders;
    private sleep;
    request<T>(endpoint: string, options?: RequestOptions): Promise<T>;
    private executeRequest;
}
//# sourceMappingURL=HttpClient.d.ts.map
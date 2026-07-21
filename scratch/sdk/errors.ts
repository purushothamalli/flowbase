export class FlowBaseError extends Error {
    constructor(message: string) {
        super(message);
        this.name = "FlowBaseError";
        Object.setPrototypeOf(this, new.target.prototype);
    }
}

export class HttpError extends FlowBaseError {
    public readonly status: number;
    public readonly statusText: string;
    public readonly body: any;

    constructor(status: number, statusText: string, body: any) {
        super(`HTTP Error status: ${status}: ${statusText}`);
        this.name = "HttpError";
        this.status = status;
        this.statusText = statusText;
        this.body = body;
    }
}

export class NetworkError extends FlowBaseError {
    constructor(message: string) {
        super(message);
    }
}
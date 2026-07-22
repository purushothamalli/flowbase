"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.RateLimitError = exports.NetworkError = exports.HttpError = exports.FlowBaseError = void 0;
class FlowBaseError extends Error {
    constructor(message) {
        super(message);
        this.name = "FlowBaseError";
        Object.setPrototypeOf(this, new.target.prototype);
    }
}
exports.FlowBaseError = FlowBaseError;
class HttpError extends FlowBaseError {
    status;
    statusText;
    body;
    constructor(status, statusText, body) {
        super(`HTTP Error status: ${status}: ${statusText}`);
        this.name = "HttpError";
        this.status = status;
        this.statusText = statusText;
        this.body = body;
    }
}
exports.HttpError = HttpError;
class NetworkError extends FlowBaseError {
    constructor(message) {
        super(message);
    }
}
exports.NetworkError = NetworkError;
class RateLimitError extends HttpError {
    retryAfterSeconds;
    constructor(retryAfterSeconds, message, body) {
        super(429, message, body);
        this.retryAfterSeconds = retryAfterSeconds;
        this.name = "RateLimitError";
    }
}
exports.RateLimitError = RateLimitError;
//# sourceMappingURL=errors.js.map
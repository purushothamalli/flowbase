"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NoopLogger = exports.ConsoleLogger = void 0;
class ConsoleLogger {
    info(msg, ...args) { console.log(`[FlowBase] INFO: ${msg}`, ...args); }
    warn(msg, ...args) { console.warn(`[FlowBase] WARN: ${msg}`, ...args); }
    error(msg, ...args) { console.error(`[FlowBase] ERROR: ${msg}`, ...args); }
}
exports.ConsoleLogger = ConsoleLogger;
class NoopLogger {
    info() { }
    warn() { }
    error() { }
}
exports.NoopLogger = NoopLogger;
//# sourceMappingURL=Logger.js.map
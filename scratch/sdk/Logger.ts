export interface Logger {
    info(msg: string, ...args: any[]): void;
    warn(msg: string, ...args: any[]): void;
    error(msg: string, ...args: any[]): void;
}

export class ConsoleLogger implements Logger {
    info(msg: string, ...args: any[]) { console.log(`[FlowBase] INFO: ${msg}`, ...args); }
    warn(msg: string, ...args: any[]) { console.warn(`[FlowBase] WARN: ${msg}`, ...args); }
    error(msg: string, ...args: any[]) { console.error(`[FlowBase] ERROR: ${msg}`, ...args); }
}

export class NoopLogger implements Logger {
    info() {}
    warn() {}
    error() {}
}

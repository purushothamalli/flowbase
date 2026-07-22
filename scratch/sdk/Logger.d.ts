export interface Logger {
    info(msg: string, ...args: any[]): void;
    warn(msg: string, ...args: any[]): void;
    error(msg: string, ...args: any[]): void;
}
export declare class ConsoleLogger implements Logger {
    info(msg: string, ...args: any[]): void;
    warn(msg: string, ...args: any[]): void;
    error(msg: string, ...args: any[]): void;
}
export declare class NoopLogger implements Logger {
    info(): void;
    warn(): void;
    error(): void;
}
//# sourceMappingURL=Logger.d.ts.map
import { FlowBaseClient } from "./sdk";
import { Logger } from "./sdk/Logger";

async function testV157ExtensibilityE2E() {
    console.log("=========================================");
    console.log("   SPRINT V15.7 EXTENSIBILITY & TOOLING  ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Extensibility Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Setup custom logging mock
    const logsAccumulator: string[] = [];
    const customLogger: Logger = {
        info: (msg, ...args) => logsAccumulator.push(`INFO: ${msg}`),
        warn: (msg, ...args) => logsAccumulator.push(`WARN: ${msg}`),
        error: (msg, ...args) => logsAccumulator.push(`ERROR: ${msg}`)
    };

    // 3. Setup client with custom logger
    const client = new FlowBaseClient({
        apiKey: tenantData.apiKey,
        logger: customLogger
    } as any);

    // Verify custom logger is registered
    client.logger.info("Client initiated successfully");
    if (logsAccumulator.length === 0 || !logsAccumulator[0].includes("initiated")) {
        throw new Error("Custom pluggable logger failed to receive log events!");
    }
    console.log("✓ 2. Pluggable logging system verified successfully!");

    // 4. Test global event emitter
    console.log("▶ Testing Lifecycle event emitters (login / logout)...");
    let loginEventCaptured = false;
    let logoutEventCaptured = false;

    client.on("login", (data) => {
        loginEventCaptured = true;
        console.log("   - Captured 'login' event milestone!");
    });
    client.on("logout", () => {
        logoutEventCaptured = true;
        console.log("   - Captured 'logout' event milestone!");
    });

    const user = await client.auth.register(`ext_dev_${Date.now()}@flowbase.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    await client.auth.logout();

    if (!loginEventCaptured) {
        throw new Error("AuthManager login process failed to trigger global 'login' event!");
    }
    if (!logoutEventCaptured) {
        throw new Error("AuthManager logout process failed to trigger global 'logout' event!");
    }
    console.log("✓ 3. Lifecycle Event Emitter registry verified successfully!");

    // 5. Test Plugin loaders
    console.log("▶ Testing plugin extensible system...");
    let pluginInitialized = false;
    const testPlugin = {
        name: "test-plugin",
        initialize: (fbClient: FlowBaseClient) => {
            pluginInitialized = true;
            // Listen to login events from within the plugin
            fbClient.on("login", () => {
                fbClient.logger.info("Plugin intercepted login event");
            });
        }
    };

    client.registerPlugin(testPlugin);
    if (!pluginInitialized) {
        throw new Error("registerPlugin failed to call initialize hook on plugin!");
    }
    console.log("✓ 4. Extensible plugin registry verified successfully!");

    console.log("=========================================");
    console.log(" 🎉 SPRINT V15.7 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV157ExtensibilityE2E().catch(err => {
    console.error("❌ Test execution failed:", err);
    process.exit(1);
});

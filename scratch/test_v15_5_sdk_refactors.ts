import { FlowBaseClient } from "./sdk";

async function testV155SdkRefactorsE2E() {
    console.log("=========================================");
    console.log("    SPRINT V15.5 SDK REFACTORS TEST      ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "SDK Refactors Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate client and verify HttpClient setup
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    if (!client.httpClient) {
        throw new Error("HttpClient transport engine not instantiated on client!");
    }
    console.log("✓ 2. HttpClient Transport Layer initialized successfully!");

    // 3. Register custom middleware to check requests
    let interceptedUrl = "";
    let traceparentFound = false;
    client.use(async (request, next) => {
        interceptedUrl = request.url;
        if (request.options.headers) {
            const headers = request.options.headers as any;
            if (headers["traceparent"]) {
                traceparentFound = true;
            }
        }
        return await next();
    });

    // Register user using the client to trigger custom middleware
    console.log("▶ Registering user to trigger custom middleware execution...");
    const user = await client.auth.register(`sdk_dev_${Date.now()}@flowbase.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");

    console.log("   - Intercepted URL:", interceptedUrl);
    console.log("   - Traceparent injected in headers:", traceparentFound);

    if (!interceptedUrl.includes("/v1/auth/login")) {
        throw new Error("Middleware interceptor failed to capture request context!");
    }
    if (!traceparentFound) {
        throw new Error("Traceparent trace-logging middleware failed to execute!");
    }
    console.log("✓ 3. Decoupled Middleware Interceptor chain verified successfully!");

    // 4. Test request timeouts
    console.log("▶ Triggering request timeout handler...");
    try {
        await client.auth.fetch("/v1/jobs", { timeoutMs: 1 });
        throw new Error("Request did not timeout under 1ms timeoutMs limit!");
    } catch (err: any) {
        if (!err.message || !err.message.includes("timed out")) {
            throw new Error(`Unexpected timeout error: ${err.message}`);
        }
        console.log("   - Caught expected timeout network error:", err.message);
    }
    console.log("✓ 4. Connection Timeout and Abort signal verified successfully!");

    console.log("=========================================");
    console.log(" 🎉 SPRINT V15.5 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV155SdkRefactorsE2E().catch(err => {
    console.error("❌ Test execution failed:", err);
    process.exit(1);
});

import { FlowBaseClient } from "./sdk";

async function testTask4() {
    const client = new FlowBaseClient({
        apiKey: "fb_live_test_key",
        baseUrl: "http://localhost:8080",
        timeoutMs: 5000,
        retry: { maxRetries: 2 }
    });

    console.log("=== 1. Testing Request Timeout ===");
    try {
        await client.auth.fetch("/v1/collections", { timeoutMs: 1 });
        console.error("❌ Timeout test failed: Request did not time out!");
    } catch (err: any) {
        console.log("✓ Request timed out as expected:", err.message);
    }

    console.log("=== 2. Testing AbortController Cancellation ===");
    try {
        const controller = new AbortController();
        const promise = client.auth.fetch("/v1/collections", { signal: controller.signal });
        controller.abort("User navigated away");
        await promise;
        console.error("❌ AbortSignal test failed: Request was not aborted!");
    } catch (err: any) {
        console.log("✓ Request aborted as expected:", err.message);
    }

    console.log("=== 3. Testing Normal Request Execution ===");
    try {
        const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: "Retry Corp" })
        });
        const tenantData = await tenantRes.json();

        const validClient = new FlowBaseClient({ apiKey: tenantData.apiKey });
        const user = await validClient.auth.register(`dev_${Date.now()}@flowbase.io`, "ABCabc123!");
        await validClient.auth.login(user.email, "ABCabc123!");

        const collections = await validClient.auth.fetch("/v1/collections");
        console.log("✓ Normal request succeeded. Collections:", collections);
        await validClient.auth.logout();
    } catch (err: any) {
        console.error("❌ Normal request failed:", err);
    }

    console.log("=========================================");
    console.log("   TASK 4 VERIFICATION COMPLETED!       ");
    console.log("=========================================");
}

testTask4();

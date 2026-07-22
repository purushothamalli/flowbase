import { FlowBaseClient } from "./sdk";

async function testV14TelemetryE2E() {
    console.log("=========================================");
    console.log("    SPRINT V14 TELEMETRY & METRICS TEST  ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Observability Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate client and login user
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    const user = await client.auth.register(`telemetry_dev_${Date.now()}@flowbase.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Authenticated:", user.email);

    // 3. Verify W3C TraceContext Header Propagation
    console.log("▶ Verifying W3C traceparent header propagation in outbound response...");
    const rawRes = await client.auth.fetchRaw("/v1/jobs");
    const traceparentHeader = rawRes.headers.get("traceparent");
    console.log("   - Response Header 'traceparent':", traceparentHeader);
    if (!traceparentHeader || !traceparentHeader.startsWith("00-")) {
        throw new Error("W3C traceparent header context missing or invalid!");
    }
    console.log("✓ 3. W3C traceparent Propagated Successfully!");

    // 4. Trigger Telemetry Events: Publish background outbox jobs
    console.log("▶ Publishing background jobs to increment metrics...");
    await client.jobs.publish("NOTIFICATION_SEND", { msg: "Telemetry verification job #1" });
    await client.jobs.publish("NOTIFICATION_SEND", { msg: "Telemetry verification job #2" });
    console.log("⌛ Waiting 2.5 seconds for background outbox worker execution...");
    await new Promise(r => setTimeout(r, 2600));

    // 5. Query Actuator Prometheus Endpoint
    console.log("▶ Fetching metrics from /actuator/prometheus...");
    const metricsRes = await fetch("http://localhost:8080/actuator/prometheus");
    if (!metricsRes.ok) {
        const bodyText = await metricsRes.text().catch(() => "N/A");
        console.error(`❌ HTTP Error: ${metricsRes.status} ${metricsRes.statusText}`);
        console.error("❌ Response Body:", bodyText.substring(0, 500));
        throw new Error("Failed to retrieve actuator prometheus metrics");
    }
    const metricsText = await metricsRes.text();

    // 6. Assert Prometheus custom metrics existence
    console.log("▶ Verifying custom metrics in Prometheus export data...");
    
    // Check if JVM/Hikari metrics exist
    const hasJvmMetrics = metricsText.includes("jvm_memory_used_bytes") || metricsText.includes("hikaricp_connections");
    console.log("   - Base JVM & DB Pool Metrics Present:", hasJvmMetrics ? "PASS" : "FAIL");
    if (!hasJvmMetrics) throw new Error("JVM or Hikari connection pool metrics are missing!");

    // Check custom outbox worker metrics
    const hasJobMetrics = metricsText.includes("flowbase_jobs_processed_total");
    console.log("   - Custom Job Metrics Present:", hasJobMetrics ? "PASS" : "FAIL");
    if (hasJobMetrics) {
        // Parse successes count
        const match = metricsText.match(/flowbase_jobs_processed_total\{status="success"\}.* (\d+)/);
        if (match) {
            console.log("   - Successfully Processed Job Count:", match[1]);
        }
    } else {
        throw new Error("Custom outbox worker metric 'flowbase_jobs_processed_total' missing!");
    }

    await client.auth.logout();
    console.log("=========================================");
    console.log(" 🎉 SPRINT V14 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV14TelemetryE2E();

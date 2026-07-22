import { FlowBaseClient } from "./sdk";

async function testV154ResiliencyE2E() {
    console.log("=========================================");
    console.log("    SPRINT V15.4 RESILIENCY & DLQ TEST   ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Resiliency Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate client and login user
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    const user = await client.auth.register(`resiliency_dev_${Date.now()}@flowbase.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Authenticated:", user.email);

    // 3. Test Idempotency key tracking
    console.log("▶ Publishing job with idempotency key (Request #1)...");
    const idempotencyKey = `key_${Date.now()}`;
    const publishRes1 = await fetch("http://localhost:8080/v1/jobs/publish", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-FlowBase-API-Key": tenantData.apiKey,
            "Authorization": `Bearer ${client.auth.getToken()}`,
            "X-Idempotency-Key": idempotencyKey
        },
        body: JSON.stringify({
            eventType: "NOTIFICATION_SEND",
            payload: { msg: "Idempotence task" }
        })
    });
    if (!publishRes1.ok) throw new Error("Failed to publish first request");
    const job1 = await publishRes1.json();
    const cacheLookupHeader1 = publishRes1.headers.get("X-Cache-Lookup");
    console.log("   - Response #1 Job ID:", job1.id, "| X-Cache-Lookup:", cacheLookupHeader1);

    console.log("▶ Publishing job with same idempotency key (Request #2)...");
    const publishRes2 = await fetch("http://localhost:8080/v1/jobs/publish", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-FlowBase-API-Key": tenantData.apiKey,
            "Authorization": `Bearer ${client.auth.getToken()}`,
            "X-Idempotency-Key": idempotencyKey
        },
        body: JSON.stringify({
            eventType: "NOTIFICATION_SEND",
            payload: { msg: "Idempotence task" }
        })
    });
    if (!publishRes2.ok) throw new Error("Failed to publish second request");
    const job2 = await publishRes2.json();
    const cacheLookupHeader2 = publishRes2.headers.get("X-Cache-Lookup");
    console.log("   - Response #2 Job ID:", job2.id, "| X-Cache-Lookup:", cacheLookupHeader2);

    if (job1.id !== job2.id) {
        throw new Error("Idempotency tracking failed! Returned different Job IDs.");
    }
    if (cacheLookupHeader2 !== "HIT") {
        throw new Error("Idempotency lookup header mismatch!");
    }
    console.log("✓ 3. Idempotency Payload Reservation verified successfully!");

    // 4. Test Dead Letter Queue (DLQ) by sending a poison job with no handler
    console.log("▶ Publishing a poison job with an unregistered handler type to trigger DLQ...");
    const poisonJobRes = await fetch("http://localhost:8080/v1/jobs/publish", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-FlowBase-API-Key": tenantData.apiKey,
            "Authorization": `Bearer ${client.auth.getToken()}`
        },
        body: JSON.stringify({
            eventType: "UNREGISTERED_EVENT_TYPE_POISON",
            payload: { data: "poison" }
        })
    });
    const poisonJob = await poisonJobRes.json();
    console.log("   - Poison job created with ID:", poisonJob.id);

    console.log("⌛ Waiting 3.5 seconds for background outbox worker execution...");
    await new Promise(r => setTimeout(r, 3500));

    console.log("▶ Fetching poison job details to verify status...");
    const checkRes = await client.auth.fetch<any>(`/v1/jobs/${poisonJob.id}`);
    console.log("   - Job Status:", checkRes.status, "| Error Message:", checkRes.errorMessage);
    if (checkRes.status !== "FAILED") {
        throw new Error("Job processor failed to mark unregistered event handler as FAILED!");
    }
    console.log("✓ 4. DLQ Routing and Execution failure tracked successfully!");

    console.log("=========================================");
    console.log(" 🎉 SPRINT V15.4 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV154ResiliencyE2E().catch(err => {
    console.error("❌ Test execution failed:", err);
    process.exit(1);
});

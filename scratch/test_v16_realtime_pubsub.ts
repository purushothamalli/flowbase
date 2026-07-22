import { FlowBaseClient } from "./sdk";

// If WebSocket is not in global, map it to ws
if (typeof global.WebSocket === "undefined") {
    const ws = require("ws");
    global.WebSocket = ws;
}

async function testV16RealtimePubSubE2E() {
    console.log("=========================================");
    console.log("    SPRINT V16 SCALED REALTIME ENGINE    ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Scaled Realtime Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate client and login user
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    const user = await client.auth.register(`pubsub_dev_${Date.now()}@flowbase.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Authenticated:", user.email);

    // 3. Create Collection
    console.log("▶ Creating collection 'alerts'...");
    const collection = await client.auth.fetch<any>("/v1/collections", {
        method: "POST",
        body: JSON.stringify({
            name: "alerts",
            readRule: "true",
            writeRule: "true",
            cacheTtlSeconds: 3600,
            lockTtlSeconds: 5,
            fields: [
                { name: "message", type: "STRING", required: true, indexed: true, searchable: true },
                { name: "severity", type: "STRING", required: true, indexed: true, searchable: true }
            ]
        })
    });
    const alerts = client.collection(collection.id);
    console.log("✓ 3. Collection Provisioned! ID:", collection.id);

    // 4. Establish WebSocket subscription
    console.log("▶ Subscribing to realtime updates via SDK WebSocket...");
    let eventReceived: any = null;
    
    const unsubscribe = client.realtime.subscribe(collection.id, (event) => {
        console.log("   - [WS Message Received] Event:", event.event, "| Message:", (event.data as any).data.message);
        eventReceived = event;
    });

    // Wait a brief moment for WebSocket handshakes and subscription confirmations
    await new Promise(resolve => setTimeout(resolve, 1500));

    // 5. Trigger Document Insertion via REST API
    console.log("▶ Performing REST document insertion to trigger event publication...");
    const inserted = await alerts.insert({
        message: "Redis Pub/Sub scaling coordinates WebSocket delivery!",
        severity: "HIGH"
    });
    console.log("   - REST Insert Succeeded! ID:", inserted.id);

    // 6. Wait for the event to be dispatched back
    console.log("▶ Waiting for scaled realtime broadcast loop...");
    let timeout = 10;
    while (!eventReceived && timeout > 0) {
        await new Promise(resolve => setTimeout(resolve, 500));
        timeout--;
    }

    // Cleanup connection
    unsubscribe();
    client.realtime.disconnect();

    if (!eventReceived) {
        throw new Error("Timeout waiting for realtime WebSocket message broadcast from Redis Pub/Sub!");
    }

    if (eventReceived.event !== "insert" || (eventReceived.data as any).data.message !== "Redis Pub/Sub scaling coordinates WebSocket delivery!") {
        throw new Error("Received incorrect or malformed WebSocket data payload!");
    }

    console.log("✓ 4. Distributed Redis Pub/Sub WebSocket dispatch loop verified successfully!");
    console.log("=========================================");
    console.log(" 🎉 SPRINT V16 E2E TEST 100% PASSED!    ");
    console.log("=========================================");
}

testV16RealtimePubSubE2E().catch(err => {
    console.error("❌ Test execution failed:", err);
    process.exit(1);
});

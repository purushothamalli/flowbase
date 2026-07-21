import {FlowBaseClient, RateLimitError} from "./sdk";

async function testV13RateLimitingE2E() {
    console.log("=========================================");
    console.log("  SPRINT V13 RATE LIMITING E2E TEST      ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({name: "Rate Limiting Corp"})
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Register & Login User (Options set maxRetries: 0 so we catch initial HTTP 429 without auto-waiting)
    const client = new FlowBaseClient({apiKey: tenantData.apiKey, retry: {maxRetries: 0, retryStatusCodes: []}});
    const user = await client.auth.register(`rate_dev_${Date.now()}@flowbasecorp.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Logged In:", user.email);

    // 3. Fire Instant Parallel Burst Requests (12 requests fired in parallel)
    console.log("▶ Firing parallel burst of 12 requests (Capacity = 10 tokens)...");
    const requests = Array.from({length: 12}, (_, i) => i + 1);
    const results = await Promise.allSettled(requests.map(n => client.jobs.listJobs()));

    let allowedCount = 0;
    let rateLimitedCount = 0;
    let caughtRateLimitError: RateLimitError | null = null;

    results.forEach((res, idx) => {
        if (res.status === "fulfilled") {
            allowedCount++;
        } else {
            rateLimitedCount++;
            if (res.reason instanceof RateLimitError) {
                console.log(res);
                caughtRateLimitError = res.reason;
            }
        }
    });

    console.log(`   - Total Requests: 12`);
    console.log(`   - Allowed Requests: ${allowedCount}`);
    console.log(`   - Rate Limited Requests: ${rateLimitedCount}`);

    if (rateLimitedCount === 0 || !caughtRateLimitError) {
        throw new Error("❌ Rate limiting failed to reject excess burst requests!");
    }

    console.log("✓ 4. HTTP 429 RateLimitError Intercepted Cleanly!");
    console.log("   - Status Code:", caughtRateLimitError.status);
    console.log("   - Retry-After Seconds:", caughtRateLimitError.retryAfterSeconds);
    console.log("   - Error Message:", caughtRateLimitError.message);

    // 5. Wait 1.5 seconds for Redis Token Bucket Refill (Refill Rate = 2 tokens/sec)
    console.log("⌛ Waiting 1.5 seconds for Redis Token Bucket refill (2 tokens/sec)...");
    await new Promise(r => setTimeout(r, 1500));

    // 6. Fire Subsequent Request (Should Succeed after Refill!)
    console.log("▶ Firing request after token bucket refill...");
    const refilledJobs = await client.jobs.listJobs();
    console.log("✓ 5. Request Passed Successfully After Refill!");

    await client.auth.logout();
    console.log("=========================================");
    console.log(" 🎉 SPRINT V13 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV13RateLimitingE2E();

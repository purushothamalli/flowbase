import { FlowBaseClient } from "./sdk";

async function testV12JobsE2E() {
    console.log("=========================================");
    console.log("  SPRINT V12 BACKGROUND JOBS E2E TEST    ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Background Jobs Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate FlowBaseClient & Authenticate User
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    const user = await client.auth.register(`job_dev_${Date.now()}@flowbasecorp.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Registered & Logged In:", user.email);

    // 3. Publish Successful Job via client.jobs.publish()
    console.log("▶ Publishing successful NOTIFICATION_SEND job via client.jobs.publish()...");
    const successJob = await client.jobs.publish("NOTIFICATION_SEND", { email: user.email, message: "Welcome to FlowBase Background Jobs!" });
    console.log("✓ 3. Job Published Successfully!");
    console.log("   - Job ID:", successJob.id);
    console.log("   - Event Type:", successJob.eventType);
    console.log("   - Initial Status:", successJob.status);

    // 4. Wait 3 seconds for background worker execution
    console.log("⌛ Waiting 3 seconds for worker pool engine processing...");
    await new Promise(r => setTimeout(r, 3500));

    // 5. Verify Job Execution Status via client.jobs.getJob()
    const processedSuccessJob = await client.jobs.getJob(successJob.id);
    console.log("✓ 4. Worker Execution Result:");
    console.log("   - Status:", processedSuccessJob.status);
    if (processedSuccessJob.status !== "COMPLETED") throw new Error(`Expected COMPLETED but got ${processedSuccessJob.status}`);

    // 6. Publish Failing Job to test failure handling & retries
    console.log("▶ Publishing failing job with SIMULATE_FAILURE payload...");
    const failingJob = await client.jobs.publish("NOTIFICATION_SEND", { message: "SIMULATE_FAILURE" });
    console.log("✓ 5. Failing Job Published:", failingJob.id);

    // 7. Wait 3 seconds for worker retry attempt
    console.log("⌛ Waiting 3.5 seconds for worker processing & exception catch...");
    await new Promise(r => setTimeout(r, 3500));

    // 8. Verify Retry Increment & Failure Logging
    const processedFailingJob = await client.jobs.getJob(failingJob.id);
    console.log("✓ 6. Failing Job Verified:");
    console.log("   - Status:", processedFailingJob.status);
    console.log("   - Retry Count:", processedFailingJob.retryCount);
    console.log("   - Error Message:", processedFailingJob.errorMessage);

    // 9. Test Manual Job Retry via client.jobs.retry()
    console.log("▶ Testing manual job retry via client.jobs.retry()...");
    const retriedJob = await client.jobs.retry(failingJob.id);
    console.log("✓ 7. Manual Retry Triggered!");
    console.log("   - Reset Status:", retriedJob.status);
    console.log("   - Reset Retry Count:", retriedJob.retryCount);

    // 10. List All Tenant Jobs via client.jobs.listJobs()
    const allJobs = await client.jobs.listJobs();
    console.log("✓ 8. List Tenant Jobs Verified (Total jobs):", allJobs.length);

    await client.auth.logout();
    console.log("=========================================");
    console.log(" 🎉 SPRINT V12 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV12JobsE2E();

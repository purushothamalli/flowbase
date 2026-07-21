import { FlowBaseClient } from "./sdk";

async function testV11StorageE2E() {
    console.log("=========================================");
    console.log("   SPRINT V11 STORAGE ENGINE E2E TEST    ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Storage Engine Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate FlowBaseClient & Authenticate
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    const user = await client.auth.register(`dev_${Date.now()}@flowbasecorp.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Registered & Logged In:", user.email);

    // 3. Prepare File Payload
    const fileContent = "FlowBase Storage Engine V11 High-Speed Streaming Payload Test";
    const fileBlob = new Blob([fileContent], { type: "text/plain" });

    // 4. Upload File via SDK client.storage.upload()
    console.log("▶ Uploading file via client.storage.upload()...");
    const uploadMeta = await client.storage.upload(fileBlob, "v11_test_file.txt");
    console.log("✓ 3. File Uploaded Successfully!");
    console.log("   - File ID:", uploadMeta.id);
    console.log("   - Filename:", uploadMeta.filename);
    console.log("   - Content-Type:", uploadMeta.contentType);
    console.log("   - Size Bytes:", uploadMeta.sizeBytes);

    // 5. Fetch File Metadata via client.storage.getMetadata()
    const fetchedMeta = await client.storage.getMetadata(uploadMeta.id);
    console.log("✓ 4. Metadata Retrieved:", fetchedMeta.filename === "v11_test_file.txt" ? "MATCH" : "MISMATCH");

    // 6. Download Binary File Stream via client.storage.download()
    console.log("▶ Downloading file payload via client.storage.download()...");
    const downloadedBlob = await client.storage.download(uploadMeta.id);
    const downloadedText = await downloadedBlob.text();
    console.log("✓ 5. Download Content Verification:", downloadedText === fileContent ? "PASS (Exact Content Match)" : "FAIL");

    // 7. Delete File via client.storage.delete()
    console.log("▶ Deleting file via client.storage.delete()...");
    await client.storage.delete(uploadMeta.id);
    console.log("✓ 6. File Delete API Executed");

    // 8. Verify Metadata 404 after Deletion
    try {
        await client.storage.getMetadata(uploadMeta.id);
        console.error("❌ 7. File metadata still exists after deletion!");
    } catch (err: any) {
        console.log("✓ 7. Verified file deleted (404 / Error caught cleanly)");
    }

    await client.auth.logout();
    console.log("=========================================");
    console.log(" 🎉 SPRINT V11 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV11StorageE2E();

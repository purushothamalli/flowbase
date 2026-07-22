import { FlowBaseClient } from "./sdk";

async function testV156AdvancedQueryE2E() {
    console.log("=========================================");
    console.log("   SPRINT V15.6 ADVANCED QUERY ENGINE    ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Advanced Query Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate client and login user
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    const user = await client.auth.register(`adv_dev_${Date.now()}@flowbase.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Authenticated:", user.email);

    // 3. Create Collection
    console.log("▶ Creating collection 'inventory'...");
    const collection = await client.auth.fetch<any>("/v1/collections", {
        method: "POST",
        body: JSON.stringify({
            name: "inventory",
            readRule: "true",
            writeRule: "true",
            cacheTtlSeconds: 3600,
            lockTtlSeconds: 5,
            fields: [
                { name: "item", type: "STRING", required: true, indexed: true, searchable: true },
                { name: "qty", type: "NUMBER", required: true, indexed: true, searchable: true },
                { name: "location", type: "STRING", required: true, indexed: true, searchable: true }
            ]
        })
    });
    const inventory = client.collection(collection.id);
    console.log("✓ 3. Collection Provisioned! ID:", collection.id);

    // 4. Test insertMany bulk writes
    console.log("▶ Testing batch insertions (insertMany)...");
    const batchDocs = [
        { item: "laptop", qty: 10, location: "Warehouse A" },
        { item: "monitor", qty: 25, location: "Warehouse A" },
        { item: "keyboard", qty: 50, location: "Warehouse B" },
        { item: "mouse", qty: 100, location: "Warehouse B" },
        { item: "desk", qty: 5, location: "Warehouse C" }
    ];
    const insertedDocs = await inventory.insertMany(batchDocs);
    console.log("   - Inserted document count:", insertedDocs.length);
    if (insertedDocs.length !== 5) {
        throw new Error("insertMany failed to insert all requested documents!");
    }
    console.log("✓ 4. Batch insertions completed successfully!");

    // 5. Test Extended operators & Aggregates
    console.log("▶ Querying range with aggregates (whereBetween)...");
    const betweenQuery = inventory.query().whereBetween("qty", 10, 60);
    const count = await betweenQuery.count();
    const sum = await betweenQuery.sum("qty");
    const avg = await betweenQuery.avg("qty");
    const min = await betweenQuery.min("qty");
    const max = await betweenQuery.max("qty");

    console.log("   - Count:", count, "| Sum:", sum, "| Avg:", avg, "| Min:", min, "| Max:", max);
    if (count !== 3) throw new Error("Between range query count mismatch!");
    if (sum !== 85) throw new Error("Between range query sum mismatch!"); // 10 + 25 + 50
    if (avg !== 85 / 3) throw new Error("Between range query average mismatch!");
    if (min !== 10 || max !== 50) throw new Error("Between range query bounds mismatch!");
    console.log("✓ 5. Extended operators and client-side aggregates verified successfully!");

    // 6. Test Offset Paging Navigation
    console.log("▶ Testing paging navigation (nextPage / previousPage)...");
    const pageQuery = inventory.query().limit(2).sortBy("qty", "asc");
    
    const page1 = await pageQuery.find();
    console.log("   - Page 1 count:", page1.length, "| Lowest qty on Page 1 (alphabetical):", (page1[0] as any).data.qty);
    if (page1.length !== 2 || (page1[0] as any).data.qty !== 10) {
        throw new Error("Paging Page 1 returns invalid items!");
    }

    const page2 = await pageQuery.nextPage();
    console.log("   - Page 2 count:", page2.length, "| Lowest qty on Page 2 (alphabetical):", (page2[0] as any).data.qty);
    if (page2.length !== 2 || (page2[0] as any).data.qty !== 25) {
        throw new Error("Paging nextPage Page 2 returns invalid items!");
    }

    const prevPage = await pageQuery.previousPage();
    console.log("   - Previous Page count:", prevPage.length, "| Lowest qty on Page 1 retry (alphabetical):", (prevPage[0] as any).data.qty);
    if (prevPage.length !== 2 || (prevPage[0] as any).data.qty !== 10) {
        throw new Error("Paging previousPage Page 1 returns invalid items!");
    }
    console.log("✓ 6. Paged offset query navigation verified successfully!");

    // 7. Test Client-Side LRU caching
    console.log("▶ Testing client-side query caching...");
    const cacheQuery = inventory.query().whereEquals("location", "Warehouse A").cache(5);
    
    // First call (cache miss)
    const t0 = Date.now();
    const res1 = await cacheQuery.find();
    const d0 = Date.now() - t0;
    
    // Second call (cache hit)
    const t1 = Date.now();
    const res2 = await cacheQuery.find();
    const d1 = Date.now() - t1;

    console.log(`   - First fetch took ${d0}ms (miss) | Second fetch took ${d1}ms (hit)`);
    if (res1.length !== 2 || res2.length !== 2) {
        throw new Error("Cache query returned invalid payload document structures!");
    }
    if (d1 > d0 && d1 > 10) {
        throw new Error("Cache lookup failed to serve response from LRU cache store instantly!");
    }
    console.log("✓ 7. LRU query caching and cache hits verified successfully!");

    console.log("=========================================");
    console.log(" 🎉 SPRINT V15.6 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV156AdvancedQueryE2E().catch(err => {
    console.error("❌ Test execution failed:", err);
    process.exit(1);
});

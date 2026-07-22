import { FlowBaseClient } from "./sdk";

async function testV15AstQueryE2E() {
    console.log("=========================================");
    console.log("   SPRINT V15.3 AST & FTS ENGINE TEST    ");
    console.log("=========================================");

    // 1. Provision Tenant
    const tenantRes = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "AST Query Corp" })
    });
    if (!tenantRes.ok) throw new Error("Failed to provision tenant");
    const tenantData = await tenantRes.json();
    console.log("✓ 1. Tenant Provisioned:", tenantData.apiKey);

    // 2. Instantiate client and login user
    const client = new FlowBaseClient({ apiKey: tenantData.apiKey });
    const user = await client.auth.register(`query_dev_${Date.now()}@flowbase.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");
    console.log("✓ 2. User Authenticated:", user.email);

    // 3. Create Collection with schema using authenticated fetch wrapper
    console.log("▶ Creating collection 'products' with AST query parameters & searchable settings...");
    const collection = await client.auth.fetch<any>("/v1/collections", {
        method: "POST",
        body: JSON.stringify({
            name: "products",
            readRule: "true",
            writeRule: "true",
            cacheTtlSeconds: 10,  // Dynamic caching TTL
            lockTtlSeconds: 2,    // Dynamic locking TTL
            fields: [
                { name: "price", type: "NUMBER", required: true, indexed: true, searchable: true },
                { name: "status", type: "STRING", required: true, indexed: true, searchable: true },
                { name: "secret_code", type: "STRING", required: false, indexed: false, searchable: false }
            ]
        })
    });
    console.log("✓ 3. Collection Created successfully! ID:", collection.id);

    // 4. Insert documents
    console.log("▶ Inserting test documents into products collection...");
    const productsCollection = client.collection(collection.id);
    const doc1 = await productsCollection.insert({ price: 100, status: "available", secret_code: "sensitive1" });
    const doc2 = await productsCollection.insert({ price: 50, status: "available", secret_code: "sensitive2" });
    const doc3 = await productsCollection.insert({ price: 120, status: "sold", secret_code: "sensitive3" });
    console.log("✓ 4. Test documents inserted successfully!");

    // 5. Query AST test: price > 80 AND status == 'available'
    console.log("▶ Querying AST (price > 80 AND status == 'available')...");
    const query1Res = await productsCollection.query()
        .whereGreaterThan("price", 80)
        .whereEquals("status", "available")
        .find();
    console.log("   - Query returned document count:", query1Res.length);
    if (query1Res.length !== 1 || (query1Res[0] as any).data.price !== 100) {
        throw new Error("AST Compiler failed to execute price[gt]=80 AND status[eq]=available query!");
    }
    console.log("✓ 5. AST Filter Query (AND) verified successfully!");

    // 6. Query AST test: price < 60
    console.log("▶ Querying AST (price < 60)...");
    const query2Res = await productsCollection.query()
        .whereLessThan("price", 60)
        .find();
    console.log("   - Query returned document count:", query2Res.length);
    if (query2Res.length !== 1 || (query2Res[0] as any).data.price !== 50) {
        throw new Error("AST Compiler failed to execute price[lt]=60 query!");
    }
    console.log("✓ 6. AST Filter Query (Single) verified successfully!");

    // 7. Full-Text Search and Searchable Exclusions validation
    console.log("▶ Performing FTS query searching for 'available'...");
    const ftsSearchRes1 = await client.auth.fetch<any[]>(`/v1/data/${collection.id}/search?q=available`);
    console.log("   - Search 'available' returned document count:", ftsSearchRes1.length);
    if (ftsSearchRes1.length !== 2) {
        throw new Error("Full-text search failed to locate documents containing searchable keyword 'available'!");
    }

    console.log("▶ Performing FTS query searching for 'sensitive1' (searchable = false field)...");
    const ftsSearchRes2 = await client.auth.fetch<any[]>(`/v1/data/${collection.id}/search?q=sensitive1`);
    console.log("   - Search 'sensitive1' returned document count:", ftsSearchRes2.length);
    if (ftsSearchRes2.length !== 0) {
        throw new Error("Full-text search indexed non-searchable sensitive field value 'sensitive1'!");
    }
    console.log("✓ 7. FTS Exclusions & Trigger Search indexing verified successfully!");

    console.log("=========================================");
    console.log(" 🎉 SPRINT V15.3 E2E TEST 100% PASSED!   ");
    console.log("=========================================");
}

testV15AstQueryE2E().catch(err => {
    console.error("❌ Test execution failed:", err);
    process.exit(1);
});

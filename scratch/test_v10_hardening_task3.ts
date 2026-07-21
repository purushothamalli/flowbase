import {FlowBaseClient, MemoryStorage} from "./sdk";

async function hardening3() {
    const res = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({name: "Flowbase corp"})
    });
    if (!res.ok) return;
    const tenantData = await res.json();
    const apiKey = tenantData.apiKey;

    const client = new FlowBaseClient({apiKey});
    const user = await client.auth.register(`dev_${Date.now()}@flowbasecorp.io`, "ABCabc123!");
    await client.auth.login(user.email, "ABCabc123!");

    console.log("1. Valid token before corruption:", client.auth.getToken());
    client.storage.setItem("fb_access_token", "invalid_expired_token_123");
    client.auth["token"] = "invalid_expired_token_123";
    console.log("2. Corrupted token set:", client.auth.getToken());
    const collections = await client.auth.fetch("/v1/collections");
    console.log("3. Collections retrieved via auto-refreshed 401 retry:", collections);
    console.log("4. New valid token in storage:", client.auth.getToken());

    await client.auth.logout();
    console.log("5. Token after logout:", client.auth.getToken());
}

hardening3();
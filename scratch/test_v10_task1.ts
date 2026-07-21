import {FlowBaseClient} from "./sdk/FlowBaseClient";

// @ts-ignore
async function test() {
    const res = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({name: "Flowbase corp"})
    });
    if (!res.ok) return;
    let tenantData = await res.json();
    console.log(tenantData);
    const apiKey = tenantData.apiKey;
    const client = new FlowBaseClient({apiKey});
    console.log(client);
    const user = await client.auth.register(`dev_${Date.now()}_@flowbasecorp.io`, "ABCabc123!");
    console.log(user);
    const loggedIn = await client.auth.login(user.email, "ABCabc123!");
    console.log(client.auth.getToken(), client.auth.getCurrentUser(), loggedIn);
    try {
        await client.auth.fetch("/v1/collections");
        await client.auth.fetch("/v1/collections/123456");
    } catch (err) {
        console.error(err);
    }
    try {
        await client.auth.logout();
    } catch (e) {
        console.error(e);
        console.log(client.auth.getToken(), client.auth.getCurrentUser());
    }
}

test();
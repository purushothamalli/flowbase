import {FlowBaseClient} from "./sdk/FlowBaseClient";

interface Order {
    id?: string;
    total: number;
}

async function test3() {
    const res = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({name: "Flowbase corp"})
    });
    if (!res.ok) return;
    let tenantData = await res.json();
    // console.log(tenantData);
    const apiKey = tenantData.apiKey;
    const client = new FlowBaseClient({apiKey});
    // console.log(client);
    const user = await client.auth.register(`dev_${Date.now()}_@flowbasecorp.io`, "ABCabc123!");
    // console.log(user);
    const loggedIn = await client.auth.login(user.email, "ABCabc123!");
    // console.log(client.auth.getToken(), client.auth.getCurrentUser(), loggedIn);
    const response = await fetch("http://localhost:8080/v1/collections", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-FlowBase-API-Key": apiKey,
            Authorization: "Bearer " + client.auth.getToken()
        },
        body: JSON.stringify({
            "name": "orders",
            "fields": [
                {
                    "name": "total",
                    "type": "NUMBER",
                    "required": true
                }
            ]
        })
    });
    const col = await response.json();
    console.log("Subscribing to realtime with :", col.id);
    const unsubscribe = client.collection<Order>(col.id).subscribe((event) => {
        console.log("Received callback event: ", event);
    });
    await new Promise((r) => setTimeout(r, 1000));
    console.log("Inserting document to trigger event...");
    await client.collection(col.id).insert({total: 99.99});
    await new Promise((r) => setTimeout(r, 1000));
    unsubscribe();
    client.realtime.disconnect();
}

test3();
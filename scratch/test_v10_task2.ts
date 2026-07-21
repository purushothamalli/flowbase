import {FlowBaseClient} from "./sdk/FlowBaseClient";

interface Order {
    id?: string;
    total: number;
}

async function test2() {
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
    const data = await response.json();
    try {
        const orders = client.collection<Order>(data.id);
        let doc1 = await orders.insert({total: 49.99});
        let doc2 = await orders.insert({total: 149.99});
        let doc3 = await orders.insert({total: 249.99});
        console.log("Insert results:", doc1, doc2, doc3);
        const allOrders = await orders.query().find();
        console.log("All orders: ", allOrders);
        const highValueOrders = await orders.query().where("total").greaterThan(100).sortBy("id", "desc").find();
        console.log("High value orders: ", highValueOrders);
        const singleOrder = await orders.findById(doc3.id);
        console.log("Single order: ", singleOrder);
        const updatedDoc = await orders.update(doc1.id, {total: 349.99});
        console.log("Updated order: ", updatedDoc);
        const updatedOrders = await orders.query().find();
        console.log("Updated orders: ", updatedOrders);
        await orders.delete(doc1.id);
        const finalOrders = await orders.query().find();
        console.log("final orders: ", finalOrders);
    } catch (e) {
        console.error(e.body || e);
    }
}

test2();
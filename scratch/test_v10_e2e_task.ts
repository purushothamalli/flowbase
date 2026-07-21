import {FlowBaseClient} from "./sdk";

interface Product {
    id?: string;
    name: string,
    price: number,
    inStock: boolean
}

async function teste2e() {
    const response = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({name: "Flowbase corp"})
    });
    if (!response.ok) return;
    let tenantData = await response.json();
    const apiKey = tenantData.apiKey;
    const client = new FlowBaseClient({apiKey});
    const user = await client.auth.register(`dev_${Date.now()}_@flowbasecorp.io`, "ABCabc123!");
    const loggedIn = await client.auth.login(user.email, "ABCabc123!");
    const respons = await fetch("http://localhost:8080/v1/collections", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-FlowBase-API-Key": apiKey,
            Authorization: "Bearer " + client.auth.getToken()
        },
        body: JSON.stringify({
            "name": "e2e_products",
            "fields": [
                {
                    "name": "name",
                    "type": "STRING",
                    "required": true
                }, {
                    "name": "price",
                    "type": "NUMBER",
                    "required": true
                }, {
                    "name": "inStock",
                    "type": "BOOLEAN",
                    "required": true
                }
            ]
        })
    });
    const data = await respons.json();
    const products = client.collection<Product>(data.id);
    const unsubscribe = products.subscribe((event) => {
        console.log("Realtime events received:\n\t", event);
    });
    let res = await products.insert({name: "keyboard", price: 79, inStock: true});
    res = await products.insert({name: "mouse", price: 39, inStock: true});
    res = await products.insert({name: "monitor", price: 299, inStock: true});
    const prods = await products.query().where("price").greaterThan(50).sortBy("price", "desc").find();
    console.log("Products filtered: ", prods, "Product insert res: ", res);
    res = await products.update(prods[1].id, {price: 89});
    console.log("Updated product response :", res)
    res = await products.delete(prods[1].id);
    console.log("Deleted product response :", res)
    unsubscribe();
    client.realtime.disconnect();
    await client.auth.logout();
}

teste2e();
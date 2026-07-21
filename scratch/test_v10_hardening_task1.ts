import {FlowBaseClient} from "./sdk";

async function testHardening1() {
    const res = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({name: "Flowbase corp"})
    });
    if (!res.ok) return;
    let tenantData = await res.json();
    const apiKey = tenantData.apiKey;
    const client = new FlowBaseClient({apiKey});
    client.use(async (ctx, next) => {
        ctx.options.headers["X-Custom-Trace"] = "trace-123";
        return await next();
    }).use(async (ctx, next) => {
        console.log("REQ: ", ctx);
        const resCtx = await next();
        console.log("RES: ", resCtx.response);
        return resCtx;
    });
    const user = await client.auth.register(`dev_${Date.now()}_@flowbasecorp.io`, "ABCabc123!");
    const loggedIn = await client.auth.login(user.email, "ABCabc123!");
}

testHardening1();
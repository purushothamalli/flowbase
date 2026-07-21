import {FlowBaseClient, TokenStorage} from "./sdk";

class MockStorage implements TokenStorage {
    private tokens: Map<string, string> = new Map<string, string>();

    getItem(key: string): string | null {
        console.log(key);
        return this.tokens.get(key);
    }

    removeItem(key: string): void {
        this.tokens.delete(key);
    }

    setItem(key: string, value: string): void {
        this.tokens.set(key, value);
    }

}

async function hardening2() {
    const res = await fetch("http://localhost:8080/v1/tenants", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({name: "Flowbase corp"})
    });
    if (!res.ok) return;
    let tenantData = await res.json();
    const apiKey = tenantData.apiKey;

    const storage = new MockStorage();
    const client = new FlowBaseClient({apiKey, storage});

    const user = await client.auth.register(`dev_${Date.now()}_@flowbasecorp.io`, "ABCabc123!");
    console.log("1. Storage before login:", storage);

    const loggedIn = await client.auth.login(user.email, "ABCabc123!");
    console.log("2. Storage after login:", storage);
    console.log("✓ Token from storage:", client.auth.getToken());

    await client.auth.logout();
    console.log("3. Storage after logout:", storage);
}

hardening2();
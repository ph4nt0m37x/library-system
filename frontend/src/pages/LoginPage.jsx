import { useEffect, useRef } from "react";
import keycloak from "../keycloak";

function LoginPage() {
    const initialized = useRef(false);

    useEffect(() => {
        if (initialized.current) {
            return;
        }

        initialized.current = true;

        keycloak
            .init({
                onLoad: "login-required",
                pkceMethod: "S256",
            })
            .then((authenticated) => {
                console.log("Authenticated:", authenticated);
                console.log("Keycloak token:", keycloak.token);

                localStorage.setItem("token", keycloak.token);
            })
            .catch((error) => {
                console.error("Keycloak login failed:", error);
            });
    }, []);

    return (
        <div>
            <h1>Login</h1>
            <p>Redirecting to Keycloak...</p>
        </div>
    );
}

export default LoginPage;
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { initKeycloak } from "../keycloak";

function LoginPage() {
    const navigate = useNavigate();

    useEffect(() => {
        initKeycloak()
            .then(() => navigate("/dashboard", { replace: true }))
            .catch((error) => {
                console.error("Keycloak login failed:", error);
            });
    }, [navigate]);

    return (
        <div>
            <h1>Login</h1>
            <p>Checking your session...</p>
        </div>
    );
}

export default LoginPage;

import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: "http://localhost:8180",
    realm: "library-system",
    clientId: "library-frontend",
});

let initPromise;
let refreshTimer;

const storeToken = () => {
    if (keycloak.token) {
        localStorage.setItem("token", keycloak.token);
    }
};

export const initKeycloak = () => {
    if (!initPromise) {
        initPromise = keycloak
            .init({
                onLoad: "login-required",
                pkceMethod: "S256",
                checkLoginIframe: false,
            })
            .then((authenticated) => {
                storeToken();

                if (!refreshTimer) {
                    refreshTimer = window.setInterval(() => {
                        keycloak
                            .updateToken(60)
                            .then(storeToken)
                            .catch(() => keycloak.login());
                    }, 30000);
                }

                keycloak.onTokenExpired = () => {
                    keycloak
                        .updateToken(60)
                        .then(storeToken)
                        .catch(() => keycloak.login());
                };

                return authenticated;
            });
    }

    return initPromise;
};

export const getAuthToken = () => keycloak.token ?? localStorage.getItem("token");

export const logout = () => {
    localStorage.removeItem("token");
    return keycloak.logout({
        redirectUri: window.location.origin + "/login",
    });
};

export default keycloak;

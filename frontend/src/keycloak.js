import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: "http://localhost:8180",
    realm: "library-system",
    clientId: "library-frontend",
});

export default keycloak;
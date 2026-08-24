package com.izischool.auth.service;

import com.izischool.auth.domain.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class KeycloakAdminService {

    @Value("${izischool.keycloak.server-url:http://localhost:8180}")
    private String serverUrl;

    @Value("${izischool.keycloak.realm:iziSchool}")
    private String realm;

    @Value("${izischool.keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${izischool.keycloak.admin-password:admin}")
    private String adminPassword;

    private final RestClient restClient;

    public KeycloakAdminService() {
        this.restClient = RestClient.builder().build();
    }

    /**
     * Crée l'utilisateur dans Keycloak avec son mot de passe et son rôle, et retourne son Keycloak User ID (sub).
     */
    public String createKeycloakUser(String email, String password, String firstName, String lastName, UserRole role) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null || adminToken.isBlank()) {
                log.warn("Impossible d'obtenir le token Keycloak Admin. Utilisation d'un ID de secours en mode dév.");
                return "kc-" + UUID.randomUUID();
            }

            // 1. Création de l'utilisateur dans Keycloak
            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
            );

            Map<String, Object> userPayload = Map.of(
                    "username", email,
                    "email", email,
                    "firstName", firstName != null ? firstName : "",
                    "lastName", lastName != null ? lastName : "",
                    "enabled", true,
                    "emailVerified", true,
                    "credentials", List.of(credential)
            );

            String usersUrl = String.format("%s/admin/realms/%s/users", serverUrl, realm);

            var response = restClient.post()
                    .uri(usersUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userPayload)
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();
            String keycloakUserId = null;
            if (location != null) {
                String path = location.getPath();
                keycloakUserId = path.substring(path.lastIndexOf('/') + 1);
                log.info("Utilisateur créé dans Keycloak avec ID: {}", keycloakUserId);
            }

            if (keycloakUserId != null) {
                // S'assurer que le mot de passe est explicitement activé et permanent dans Keycloak
                updateUserPassword(keycloakUserId, password);

                if (role != null) {
                    assignRealmRole(adminToken, keycloakUserId, role.name());
                }
            }

            return keycloakUserId != null ? keycloakUserId : "kc-" + UUID.randomUUID();

        } catch (Exception e) {
            log.warn("Création Keycloak non aboutie (mode local / standalone) : {}. Génération d'un identifiant local.", e.getMessage());
            return "kc-" + UUID.randomUUID();
        }
    }

    private String getAdminAccessToken() {
        try {
            String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", serverUrl);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", "admin-cli");
            formData.add("grant_type", "password");
            formData.add("username", adminUsername);
            formData.add("password", adminPassword);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération du token admin Keycloak: {}", e.getMessage());
        }
        return null;
    }

    private void assignRealmRole(String adminToken, String userId, String roleName) {
        try {
            String roleWithPrefix = roleName.toUpperCase().startsWith("ROLE_") ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase();
            String roleWithoutPrefix = roleName.toUpperCase().startsWith("ROLE_") ? roleName.substring(5) : roleName.toUpperCase();

            List<Map<String, Object>> rolesToAssign = new java.util.ArrayList<>();

            Map<String, Object> rep1 = fetchOrCreateRole(adminToken, roleWithPrefix);
            if (rep1 != null) {
                rolesToAssign.add(rep1);
            }

            Map<String, Object> rep2 = fetchOrCreateRole(adminToken, roleWithoutPrefix);
            if (rep2 != null) {
                rolesToAssign.add(rep2);
            }

            if (!rolesToAssign.isEmpty()) {
                String mappingUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", serverUrl, realm, userId);
                restClient.post()
                        .uri(mappingUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(rolesToAssign)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Rôles Keycloak {} assignés avec succès à l'utilisateur [{}]",
                        rolesToAssign.stream().map(r -> r.get("name")).toList(), userId);
            }
        } catch (Exception e) {
            log.warn("Impossible d'assigner le rôle [{}] dans Keycloak: {}", roleName, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchOrCreateRole(String adminToken, String roleName) {
        String roleUrl = String.format("%s/admin/realms/%s/roles/%s", serverUrl, realm, roleName);
        try {
            return restClient.get()
                    .uri(roleUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            try {
                String createRoleUrl = String.format("%s/admin/realms/%s/roles", serverUrl, realm);
                restClient.post()
                        .uri(createRoleUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", roleName, "description", "Role " + roleName))
                        .retrieve()
                        .toBodilessEntity();
                return restClient.get()
                        .uri(roleUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .retrieve()
                        .body(Map.class);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Met à jour le mot de passe d'un utilisateur dans Keycloak via l'API Admin.
     */
    public void updateUserPassword(String userId, String newPassword) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null || adminToken.isBlank()) {
                log.warn("Impossible d'obtenir le token Keycloak Admin pour la mise à jour du mot de passe.");
                return;
            }

            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", newPassword,
                    "temporary", false
            );

            String resetUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", serverUrl, realm, userId);
            restClient.put()
                    .uri(resetUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credential)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Mot de passe Keycloak mis à jour avec succès pour l'utilisateur ID: {}", userId);
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du mot de passe dans Keycloak pour l'utilisateur {}: {}", userId, e.getMessage());
        }
    }
}

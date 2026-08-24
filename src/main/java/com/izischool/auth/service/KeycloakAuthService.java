package com.izischool.auth.service;

import com.izischool.auth.dto.KeycloakTokenResponse;
import com.izischool.common.exception.BusinessException;
import com.izischool.common.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class KeycloakAuthService {

    @Value("${izischool.keycloak.server-url:http://localhost:8180}")
    private String serverUrl;

    @Value("${izischool.keycloak.realm:iziSchool}")
    private String realm;

    @Value("${izischool.keycloak.auth-client-id:izischool-frontend}")
    private String authClientId;

    @Value("${izischool.keycloak.client-secret:}")
    private String clientSecret;

    private final RestClient restClient;
    private final KeycloakAdminService keycloakAdminService;

    public KeycloakAuthService(KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminService = keycloakAdminService;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Authentifie l'utilisateur auprès de Keycloak avec son email et mot de passe (flux Password Grant).
     */
    public KeycloakTokenResponse login(String email, String password) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", authClientId);
        formData.add("username", email);
        formData.add("password", password);

        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }

        try {
            return restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);
        } catch (HttpClientErrorException e) {
            log.warn("Échec d'authentification Keycloak pour l'utilisateur {} : status={} body={}",
                    email, e.getStatusCode(), e.getResponseBodyAsString());
            throw new UnauthorizedException("Email ou mot de passe invalide");
        } catch (RestClientException e) {
            log.error("Erreur de communication avec le serveur Keycloak à l'URL {}: {}", tokenUrl, e.getMessage());
            throw new BusinessException("Impossible de joindre le serveur d'authentification Keycloak");
        }
    }

    /**
     * Rafraîchit le token d'accès auprès de Keycloak à partir d'un refresh token valide.
     */
    public KeycloakTokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token manquant");
        }

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", authClientId);
        formData.add("refresh_token", refreshToken);

        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }

        try {
            return restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);
        } catch (HttpClientErrorException e) {
            log.warn("Échec de rafraîchissement du token Keycloak : status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new UnauthorizedException("Token de rafraîchissement invalide ou expiré");
        } catch (RestClientException e) {
            log.error("Erreur lors de la communication avec Keycloak pour le refresh: {}", e.getMessage());
            throw new BusinessException("Impossible de joindre le serveur d'authentification Keycloak");
        }
    }

    /**
     * Déconnecte l'utilisateur en révoquant sa session dans Keycloak.
     */
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", serverUrl, realm);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", authClientId);
        formData.add("refresh_token", refreshToken);

        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }

        try {
            restClient.post()
                    .uri(logoutUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Session Keycloak révoquée avec succès.");
        } catch (Exception e) {
            log.warn("Erreur lors de la révocation de la session Keycloak: {}", e.getMessage());
        }
    }

    /**
     * Modifie le mot de passe d'un utilisateur après vérification de son ancien mot de passe.
     */
    public void changePassword(String email, String currentPassword, String newPassword, String keycloakUserId) {
        // 1. Vérifier l'ancien mot de passe en tentant une connexion
        try {
            login(email, currentPassword);
        } catch (UnauthorizedException e) {
            throw new UnauthorizedException("Le mot de passe actuel est incorrect");
        }

        // 2. Mettre à jour avec le nouveau mot de passe via l'API Admin de Keycloak
        keycloakAdminService.updateUserPassword(keycloakUserId, newPassword);
    }
}

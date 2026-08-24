# ==============================================================================
# iziSchool — Commandes de développement local & DevOps
# ==============================================================================
# Copier .env.example -> .env avant utilisation : cp .env.example .env
#
# Note : .env n'est PAS chargé via `include` (GNU Make interprète les '$'
# ce qui peut poser problème avec certains caractères spéciaux).
# Les cibles qui nécessitent les variables les sourcent via le shell.

.DEFAULT_GOAL := help

.PHONY: help dev infra db build run test test-unit test-it migrate logs logs-kc logs-db stop down clean docker-build docker-up

help: ## Affiche cette aide détaillée
	@echo "\033[1;34m====================================================================\033[0m"
	@echo "\033[1;32m  iziSchool Backend — Commandes de développement disponibles\033[0m"
	@echo "\033[1;34m====================================================================\033[0m"
	@grep -hE '^[a-zA-Z_-]+:.*## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""

dev: ## Démarre l'infrastructure (PostgreSQL, Redis, Keycloak), compile et lance l'app Spring Boot
	@echo "\033[1;33m[1/3] Démarrage des conteneurs d'infrastructure...\033[0m"
	docker compose up -d postgres redis keycloak
	@echo "\033[1;33m[2/3] Attente de la disponibilité des services (PostgreSQL & Redis)...\033[0m"
	sleep 5
	@echo "\033[1;33m[3/3] Compilation et lancement du Backend iziSchool...\033[0m"
	mvn clean install -DskipTests
	set -a && [ -f ./.env ] && . ./.env; set +a && mvn spring-boot:run

infra: ## Démarre uniquement les services d'infrastructure (Postgres, Redis, Keycloak)
	@echo "\033[1;32mDémarrage de PostgreSQL, Redis et Keycloak...\033[0m"
	docker compose up -d postgres redis keycloak

db: infra ## Alias pour la commande infra

build: ## Compile le projet avec Maven (en ignorant les tests)
	@echo "\033[1;32mCompilation du projet iziSchool...\033[0m"
	mvn clean install -DskipTests

run: ## Lance l'application Spring Boot en local (nécessite `make infra` au préalable)
	@echo "\033[1;32mLancement de l'API iziSchool sur le port 8081...\033[0m"
	set -a && [ -f ./.env ] && . ./.env; set +a && mvn spring-boot:run

test: ## Exécute l'ensemble des tests du projet (unitaires & intégration)
	@echo "\033[1;32mExécution de la suite de tests...\033[0m"
	mvn test

test-unit: ## Exécute uniquement les tests unitaires des services et contrôleurs métier
	@echo "\033[1;32mExécution des tests unitaires métier...\033[0m"
	mvn test -Dtest=*ServiceTest,*ApiControllersTest,*ValidationServiceTest

test-it: ## Exécute uniquement les tests d'intégration
	@echo "\033[1;32mExécution des tests d'intégration...\033[0m"
	mvn test -Dtest=*IntegrationTest

migrate: ## Vérifie l'état et applique les migrations Flyway
	@echo "\033[1;32mApplication des migrations Flyway...\033[0m"
	set -a && [ -f ./.env ] && . ./.env; set +a && mvn flyway:info flyway:migrate -Dflyway.url="$$DB_URL" -Dflyway.user="$$DB_USERNAME" -Dflyway.password="$$DB_PASSWORD"

logs: ## Affiche et suit les logs de tous les conteneurs Docker en direct
	docker compose logs -f

logs-kc: ## Affiche et suit les logs du conteneur Keycloak
	docker compose logs -f keycloak

logs-db: ## Affiche et suit les logs de PostgreSQL
	docker compose logs -f postgres

stop: ## Arrête tous les conteneurs Docker (les données restent préservées)
	@echo "\033[1;33mArrêt des conteneurs...\033[0m"
	docker compose stop

down: ## Arrête et supprime les conteneurs + volumes (⚠️ efface les données locales)
	@echo "\033[1;31mSuppression des conteneurs et volumes Docker locaux...\033[0m"
	docker compose down -v

clean: ## Nettoie les artefacts compilés dans target/
	@echo "\033[1;33mNettoyage des fichiers compilés Maven...\033[0m"
	mvn clean

docker-build: ## Construit l'image Docker du backend iziSchool
	@echo "\033[1;32mConstruction de l'image Docker...\033[0m"
	docker compose build izischool-backend

docker-up: ## Lance tout l'écosystème en conteneurs (Backend + DB + Redis + Keycloak)
	@echo "\033[1;32mLancement de l'écosystème complet Docker...\033[0m"
	docker compose up -d

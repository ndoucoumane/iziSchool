# iziSchool — Référentiel Complet des Endpoints de l'API REST

Ce document répertorie et documente l'ensemble des endpoints de l'API REST de **iziSchool** à destination des équipes de développement Frontend.

---

## 📌 1. Conventions Générales

* **Base URL** : `http://localhost:8081/api/v1` (ou `${BACKEND_URL}/api/v1`)
* **Swagger UI & Spécification OpenAPI** : `http://localhost:8081/swagger-ui.html` (documentation OpenAPI : `http://localhost:8081/docs`)
* **Format des données** : `application/json` (sauf import fichier `multipart/form-data` et export PDF `application/pdf`)
* **Authentification** : Bearer Token JWT via Header HTTP :
  ```http
  Authorization: Bearer <JWT_ACCESS_TOKEN>
  ```
* **Multi-tenant** : Le tenant (`school_id`) est automatiquement extrait et validé à partir du `sub` (Keycloak ID) et du profil utilisateur en base.
* **Gestion des erreurs standard** :
  ```json
  {
    "timestamp": "2026-08-23T00:12:34.145Z",
    "status": 400,
    "code": "VALIDATION_FAILED",
    "message": "Validation failed for one or more fields",
    "path": "/api/v1/users",
    "validationErrors": {
      "password": "Le mot de passe initial est obligatoire"
    }
  }
  ```

### Structure standard des réponses paginées (`PageResponse<T>`)
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 145,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

---

## 👥 Matrice des 5 Rôles Utilisateurs

| Rôle | Portée | Description |
| :--- | :--- | :--- |
| **`SUPER_ADMIN`** | Plateforme SaaS | Administration globale : toutes les écoles, abonnements, statistiques et utilisateurs plateforme. |
| **`DIRECTOR`** | Établissement (Tenant) | Direction générale de son école : accès complet pédagogique, financier, effectifs, relances et rapports. |
| **`ADMIN`** | Établissement (Tenant) | Administration opérationnelle de son école : inscriptions, dossiers élèves, classes, annuaire des parents. |
| **`ACCOUNTANT`** | Établissement (Tenant) | Économat : gestion des frais, échéanciers, encaissements Mobile Money/comptoir, reçus PDF et impayés. |
| **`PARENT`** | Famille (Tenant) | Espace famille : consultation des frais de ses enfants, paiement direct Mobile Money et téléchargement des reçus. |

---

## 🔐 2. Authentification & Profil (`/api/v1/auth` & `/api/v1/users`)

### Endpoints Authentification

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Connexion avec email et mot de passe | Public |
| `POST` | `/api/v1/auth/refresh` | Rafraîchissement d'un token d'accès | Public |
| `POST` | `/api/v1/auth/logout` | Déconnexion de la session courante | Authentifié |
| `GET` | `/api/v1/auth/me` | Récupération du profil, rôle et école connectée | Authentifié |
| `POST` | `/api/v1/auth/change-password` | Modification du mot de passe | Authentifié |

#### Exemple Requête `POST /api/v1/auth/login`
```json
{
  "email": "directeur@ecolepilote.sn",
  "password": "Passer1234!"
}
```

#### Exemple Réponse `GET /api/v1/auth/me`
```json
{
  "id": "1fc21c18-103c-49d3-9eb1-48a37c19d1e8",
  "email": "directeur@ecolepilote.sn",
  "firstName": "Ousmane",
  "lastName": "Diop",
  "phone": "+221771234567",
  "role": "DIRECTOR",
  "status": "ACTIVE",
  "school": {
    "id": "22fb6719-380b-4e07-bad1-404224f09b05",
    "name": "École Pilote iziSchool",
    "code": "PILOTE-01",
    "currency": "XOF"
  }
}
```

### Endpoints Gestion des Utilisateurs (`/api/v1/users`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | Liste des utilisateurs (filtres optionnels : `role`, `page`, `size`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `POST` | `/api/v1/users/register` | **Création / Inscription d'un utilisateur** avec mot de passe et rôle | Public / `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `GET` | `/api/v1/users/{id}` | Détails d'un profil utilisateur | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |

#### Exemple Requête `POST /api/v1/users/register` (Formulaire de création d'un compte)
```json
{
  "firstName": "Mamadou",
  "lastName": "Ba",
  "email": "comptable@ecolepilote.sn",
  "phone": "+221773456789",
  "password": "MonMotDePasse123!",
  "role": "ACCOUNTANT",
  "schoolId": "22fb6719-380b-4e07-bad1-404224f09b05",
  "status": "ACTIVE"
}
```

---

## 🏫 3. Gestion des Établissements (`/api/v1/schools`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/schools` | Liste paginée des établissements (`page`, `size`) | `SUPER_ADMIN` |
| `POST` | `/api/v1/schools` | Création d'un nouvel établissement | `SUPER_ADMIN` |
| `GET` | `/api/v1/schools/{id}` | Détails d'un établissement | `SUPER_ADMIN`, `DIRECTOR` |
| `PUT` | `/api/v1/schools/{id}` | Mise à jour des informations de l'école | `SUPER_ADMIN`, `DIRECTOR` |
| `PATCH`| `/api/v1/schools/{id}/status`| Activation, désactivation ou suspension de l'école | `SUPER_ADMIN` |

#### Exemple Requête `POST /api/v1/schools`
```json
{
  "name": "Complexe Scolaire Excellence",
  "code": "EXCELLENCE-01",
  "email": "contact@excellence.sn",
  "phone": "+221338000000",
  "address": "Avenue Cheikh Anta Diop",
  "city": "Dakar",
  "country": "Sénégal",
  "currency": "XOF",
  "status": "ACTIVE"
}
```

---

## 📅 4. Années Scolaires (`/api/v1/academic-years`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/academic-years` | Liste des années scolaires de l'établissement | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `GET` | `/api/v1/academic-years/active` | **Année scolaire active en cours** | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `POST` | `/api/v1/academic-years` | Création d'une nouvelle année scolaire | `SUPER_ADMIN`, `DIRECTOR` |
| `GET` | `/api/v1/academic-years/{id}` | Consultation d'une année par son ID | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `PUT` | `/api/v1/academic-years/{id}` | Mise à jour du libellé et des dates d'une année scolaire | `SUPER_ADMIN`, `DIRECTOR` |
| `POST` | `/api/v1/academic-years/{id}/activate` | Activer comme année principale en cours | `SUPER_ADMIN`, `DIRECTOR` |
| `POST` | `/api/v1/academic-years/{id}/close` | Clôturer une année scolaire | `SUPER_ADMIN`, `DIRECTOR` |

#### Exemple Requête `POST /api/v1/academic-years`
```json
{
  "name": "2026-2027",
  "startDate": "2026-09-01",
  "endDate": "2027-06-30"
}
```

---

## 📚 5. Niveaux & Classes (`/api/v1/classes`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/classes` | Liste des classes (filtres optionnels : `academicYearId`, `name`, `level`, `status`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `POST` | `/api/v1/classes` | Création d'une classe (ex: *6ème A*, *Terminale S2*) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `GET` | `/api/v1/classes/{id}` | Détails d'une classe | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `PUT` | `/api/v1/classes/{id}` | Modification du nom, capacité ou statut | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `DELETE`| `/api/v1/classes/{id}` | Désactivation / archivage logique d'une classe | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |

#### Exemple Requête `POST /api/v1/classes`
```json
{
  "name": "Terminale S2",
  "code": "TS2",
  "capacity": 35,
  "academicYearId": "a524aab2-a837-4776-bfa5-c4e9bdbbf2c1",
  "level": "Terminale"
}
```

---

## 👨‍👩‍👧 6. Parents d'élèves (`/api/v1/parents`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/parents` | Liste paginée des parents (`page`, `size`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `POST` | `/api/v1/parents` | Création d'un profil parent/tuteur | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `GET` | `/api/v1/parents/{id}` | Détails d'un parent | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `PUT` | `/api/v1/parents/{id}` | Mise à jour des coordonnées | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `GET` | `/api/v1/parents/{id}/students` | Liste des enfants rattachés au parent | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `POST` | `/api/v1/parents/{parentId}/link-student/{studentId}` | Association parent-élève (params : `relationship`, `isFinancialContact`, `isEmergencyContact`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |

#### Exemple Requête `POST /api/v1/parents`
```json
{
  "firstName": "Ibrahima",
  "lastName": "Diallo",
  "phone": "+221771234567",
  "whatsappPhone": "+221771234567",
  "email": "ibrahima.diallo@example.com",
  "address": "Sacré Cœur 3, Dakar"
}
```

---

## 🎓 7. Élèves & Inscriptions (`/api/v1/students`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/students` | Liste paginée des élèves (filtres : `search`, `status`, `page`, `size`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `POST` | `/api/v1/students` | Création et inscription d'un élève (avec affectation classe/parent optionnelle) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `GET` | `/api/v1/students/{id}` | Fiche détaillée d'un élève | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `PUT` | `/api/v1/students/{id}` | Modification des données signalétiques | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `PATCH`| `/api/v1/students/{id}/status` | Changement de statut (`ACTIVE`, `SUSPENDED`, `TRANSFERRED`, `GRADUATED`, `DROPPED_OUT`, `WITHDRAWN`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `DELETE`| `/api/v1/students/{id}` | Suppression / désactivation logique d'un élève (`soft delete`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `GET` | `/api/v1/students/{id}/balance` | **Solde financier (total attendu, total payé, reste à payer)** | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `GET` | `/api/v1/students/{id}/schedules` | Échéanciers de paiement ordonnés de l'élève | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `GET` | `/api/v1/students/{id}/payments` | Historique des règlements enregistrés pour l'élève | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `GET` | `/api/v1/students/{id}/receipts` | Liste des reçus émis pour l'élève | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |

#### Exemple Requête `POST /api/v1/students` (Inscription avec scolarité et versement initial)
```json
{
  "firstName": "Mariama",
  "lastName": "Diop",
  "middleName": "Fatou",
  "dateOfBirth": "2010-05-14",
  "placeOfBirth": "Dakar",
  "gender": "FEMALE",
  "classId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "parentId": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
  "totalDue": 350000.00,
  "initialPayment": 150000.00,
  "paymentMethod": "CASH",
  "numberOfInstallments": 1
}
```

#### Exemple Réponse `POST /api/v1/students` (`201 Created`)
```json
{
  "id": "0948f39d-6ab4-4947-8cf1-bf09a7e4fa5d",
  "studentNumber": "PILOTE-STD-0001",
  "firstName": "Mariama",
  "lastName": "Diop",
  "middleName": "Fatou",
  "fullName": "Mariama Fatou Diop",
  "dateOfBirth": "2010-05-14",
  "placeOfBirth": "Dakar",
  "gender": "FEMALE",
  "status": "ACTIVE",
  "schoolId": "22fb6719-380b-4e07-bad1-404224f09b05",
  "classId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "className": "6ème A",
  "totalDue": 350000.00,
  "totalPaid": 150000.00,
  "remainingAmount": 200000.00,
  "paymentStatus": "PARTIALLY_PAID",
  "createdAt": "2026-08-23T22:30:00Z",
  "updatedAt": "2026-08-23T22:30:00Z"
}
```

#### Exemple Réponse `GET /api/v1/students/{id}/balance`
```json
{
  "studentId": "0948f39d-6ab4-4947-8cf1-bf09a7e4fa5d",
  "studentNumber": "PILOTE-STD-0001",
  "studentName": "Mariama Diop",
  "totalDue": 350000.00,
  "totalPaid": 150000.00,
  "totalOutstanding": 200000.00,
  "currency": "XOF"
}
```

---

## 📥 8. Import en masse d'Élèves (`/api/v1/students/import`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/students/import` | Upload d'un fichier CSV/Excel `multipart/form-data` (`file`) avec rapport d'analyse et validation | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `POST` | `/api/v1/students/import/confirm` | Confirmation et validation finale du lot importé | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |

---

## 💵 9. Frais Scolaires (`/api/v1/fees`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/fees` | Liste des frais (filtres optionnels : `academicYearId`, `type`, `status`) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `POST` | `/api/v1/fees` | Création d'une ligne tarifaire (scolarité, inscription, cantine...) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `GET` | `/api/v1/fees/{id}` | Détails d'un frais | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `PUT` | `/api/v1/fees/{id}` | Mise à jour du tarif, libellé ou type | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `PATCH`| `/api/v1/fees/{id}/status` | Activation / désactivation d'un frais | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |

#### Exemple Requête `POST /api/v1/fees`
```json
{
  "academicYearId": "a524aab2-a837-4776-bfa5-c4e9bdbbf2c1",
  "name": "Frais de scolarité annuelle",
  "code": "SCOL-ANNUELLE",
  "type": "TUITION",
  "amount": 450000.00,
  "currency": "XOF",
  "mandatory": true,
  "description": "Scolarité annuelle répartie en tranches"
}
```

---

## 🗓️ 10. Échéanciers de Paiement (`/api/v1/payment-schedules`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/payment-schedules` | Liste paginée des échéances (filtres : `studentId`, `status`, `dueDateFrom`, `dueDateTo`, `overdue`, `page`, `size`) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `POST` | `/api/v1/payment-schedules` | Création manuelle d'une échéance individuelle | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `POST` | `/api/v1/payment-schedules/generate` | **Génération automatique en N tranches mensuelles** | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `GET` | `/api/v1/payment-schedules/{id}` | Détails d'une échéance | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `PUT` | `/api/v1/payment-schedules/{id}` | Modification de la date d'échéance, montant dû ou description | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `POST` | `/api/v1/payment-schedules/{id}/cancel` | Annulation d'une échéance non réglée | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |

#### Exemple Requête `POST /api/v1/payment-schedules/generate`
```json
{
  "studentId": "0948f39d-6ab4-4947-8cf1-bf09a7e4fa5d",
  "feeId": "1fc21c18-103c-49d3-9eb1-48a37c19d1e8",
  "numberOfInstallments": 9,
  "totalAmount": 450000.00,
  "firstDueDate": "2026-10-05"
}
```

---

## 💳 11. Paiements & Mobile Money (`/api/v1/payments`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/payments` | Historique paginé des transactions financières (filtres : `studentId`, `parentId`, `status`, `paymentMethod`, `reference`, `page`, `size`) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `POST` | `/api/v1/payments` | **Enregistrement paiement manuel** (espèces, virement, chèque) avec ventilation automatique et émission du reçu | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `GET` | `/api/v1/payments/{id}` | Détails de la transaction et des ventilations d'échéances | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `POST` | `/api/v1/payments/{id}/cancel` | **Annulation d'un paiement** avec contrepassation comptable | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `POST` | `/api/v1/payments/mobile-money` | Initiation d'un paiement Mobile Money (Wave, Orange Money) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT`, `PARENT` |

#### Exemple Requête `POST /api/v1/payments` (Paiement comptoir/espèces)
```json
{
  "studentId": "0948f39d-6ab4-4947-8cf1-bf09a7e4fa5d",
  "parentId": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
  "amount": 50000.00,
  "paymentMethod": "CASH",
  "note": "Règlement scolarité tranche 1",
  "scheduleId": "6fa85f64-5717-4562-b3fc-2c963f66afa9"
}
```

#### Exemple Requête `POST /api/v1/payments/mobile-money`
```json
{
  "studentId": "0948f39d-6ab4-4947-8cf1-bf09a7e4fa5d",
  "parentId": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
  "amount": 45000.00,
  "provider": "WAVE"
}
```

#### Exemple Réponse `POST /api/v1/payments/mobile-money` (200 OK)
```json
{
  "paymentId": "5a137e28-2a2a-43cf-9f17-d5c2195f1f31",
  "status": "PENDING",
  "provider": "WAVE",
  "paymentUrl": "https://checkout.izischool.com/pay/EXCELLENCE-PAY-20260822-6347B3?provider=wave",
  "reference": "EXCELLENCE-PAY-20260822-6347B3"
}
```

---

## ⚡ 12. Webhooks Mobile Money (`/api/v1/webhooks`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/webhooks/wave` | Réception asynchrone des notifications Wave (**idempotent**) | Public (Signature Header) |
| `POST` | `/api/v1/webhooks/orange-money` | Réception asynchrone des notifications Orange Money | Public (Signature Header) |

---

## 📄 13. Reçus & Téléchargement PDF (`/api/v1/receipts`)

> [!TIP]
> Le paramètre `{idOrNumber}` accepte indifféremment l'**identifiant UUID** du reçu ou son **numéro de reçu** (ex: `PILOTE-REC-0001` ou `inv-101`).
> L'endpoint `/pdf` retourne un flux binaire `application/pdf` avec l'en-tête `Content-Disposition: inline`, permettant d'ouvrir directement le PDF dans le navigateur ou une modal iframe.

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/receipts` | Liste paginée de tous les reçus de paiement émis (`page`, `size`) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `GET` | `/api/v1/receipts/{idOrNumber}` | Données JSON d'un reçu par UUID ou numéro de reçu | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT`, `PARENT` |
| `GET` | `/api/v1/receipts/{idOrNumber}/pdf` | **Visualisation & téléchargement direct du PDF certifié** | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT`, `PARENT` |
| `GET` | `/api/v1/receipts/payment/{paymentId}` | Récupère le reçu JSON associé à une transaction de paiement | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT`, `PARENT` |
| `GET` | `/api/v1/receipts/payment/{paymentId}/pdf` | **Visualisation & téléchargement du PDF à partir du paiement** | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT`, `PARENT` |

#### Exemple Réponse `GET /api/v1/receipts/{idOrNumber}`
```json
{
  "id": "7fa85f64-5717-4562-b3fc-2c963f66afa1",
  "receiptNumber": "PILOTE-REC-0001",
  "paymentId": "5a137e28-2a2a-43cf-9f17-d5c2195f1f31",
  "paymentReference": "PILOTE-PAY-0001",
  "amount": 50000.00,
  "currency": "XOF",
  "issuedAt": "2026-08-23T00:15:00.000Z",
  "status": "GENERATED",
  "pdfUrl": null,
  "createdAt": "2026-08-23T00:15:00.000Z"
}
```

---

## 🔔 14. Notifications & Modèles (`/api/v1/notifications` & `/api/v1/notification-templates`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/notifications` | Historique paginé des messages expédiés (SMS, Email, WhatsApp) | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `POST` | `/api/v1/notifications/send` | Envoi d'une notification directe ou rappel d'échéance | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN`, `ACCOUNTANT` |
| `GET` | `/api/v1/notification-templates` | Liste des modèles de notification actifs | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `POST` | `/api/v1/notification-templates` | Création d'un modèle (SMS/Email/WhatsApp) avec variables | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |
| `PUT` | `/api/v1/notification-templates/{id}` | Mise à jour du sujet, corps ou disponibilité du modèle | `SUPER_ADMIN`, `DIRECTOR`, `ADMIN` |

#### Exemple Requête `POST /api/v1/notifications/send`
```json
{
  "studentId": "0948f39d-6ab4-4947-8cf1-bf09a7e4fa5d",
  "recipientPhone": "+221771234567",
  "recipientEmail": "parent@example.com",
  "type": "PAYMENT_REMINDER",
  "channel": "SMS",
  "title": "Rappel de paiement",
  "message": "Bonjour M. Diallo, nous vous rappelons que l'échéance pour Mariama arrive à terme le 05/10.",
  "variables": {
    "studentName": "Mariama Diop",
    "amount": "45 000 FCFA"
  }
}
```

---

## 📊 15. Dashboard & Indicateurs Financiers (`/api/v1/dashboard`)

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/dashboard/overview` | **KPIs globaux** (effectifs, total attendu, collecté, reste à payer, taux de recouvrement %, impayés) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `GET` | `/api/v1/dashboard/revenue-trend` | Série chronologique des revenus journaliers (paramètre `days=7` ou `30`) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |
| `GET` | `/api/v1/dashboard/arrears` | **Rapport d'ancienneté des créances** (tranches 0-7j, 8-30j, 30j+) | `SUPER_ADMIN`, `DIRECTOR`, `ACCOUNTANT` |

#### Exemple Réponse `GET /api/v1/dashboard/overview`
```json
{
  "students": 480,
  "totalExpected": 49000000.00,
  "totalCollected": 42850000.00,
  "totalOutstanding": 6150000.00,
  "collectionRate": 87.45,
  "todayPayments": 1250000.00,
  "overdueSchedules": 42,
  "currency": "XOF"
}
```

#### Exemple Réponse `GET /api/v1/dashboard/arrears`
```json
{
  "totalOverdue": 42,
  "overdueAmount": 6150000.00,
  "bucket0To7Days": 1200000.00,
  "bucket8To30Days": 2450000.00,
  "bucket30PlusDays": 2500000.00,
  "currency": "XOF"
}
```

---

## 📱 16. Portail Parent Dédié (`/api/v1/parent/me`)

Espace sécurisé permettant aux parents connectés de gérer la scolarité et les règlements de leurs enfants.

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/parent/me` | Profil du parent connecté | `PARENT` |
| `GET` | `/api/v1/parent/me/students` | Liste de ses enfants inscrits | `PARENT` |
| `GET` | `/api/v1/parent/me/students/{studentId}/balance` | Solde et reste à payer d'un enfant spécifique | `PARENT` |
| `GET` | `/api/v1/parent/me/students/{studentId}/schedules`| Calendrier des échéances de paiement de l'enfant | `PARENT` |
| `GET` | `/api/v1/parent/me/students/{studentId}/payments` | Historique des règlements effectués pour l'enfant | `PARENT` |
| `GET` | `/api/v1/parent/me/students/{studentId}/receipts` | Reçus émis pour l'enfant | `PARENT` |
| `POST` | `/api/v1/parent/me/payments/mobile-money` | Paiement direct d'une échéance par Mobile Money (Wave / OM) | `PARENT` |

---

## 👑 17. Plateforme & Administration Globale (SUPER_ADMIN) (`/api/v1/super-admin`)

Module exclusif réservé au rôle `SUPER_ADMIN` pour le pilotage global de la plateforme SaaS iziSchool (tenants, indicateurs multi-écoles, gestion des comptes directeurs/administrateurs).

| Méthode | Endpoint | Description | Rôles autorisés |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/super-admin/stats` | **Statistiques globales SaaS** (total écoles, actives, suspendues, total élèves, CA collecté, paiements) | `SUPER_ADMIN` |
| `GET` | `/api/v1/super-admin/schools` | Liste paginée de tous les établissements clients | `SUPER_ADMIN` |
| `POST` | `/api/v1/super-admin/schools` | **Création d'un établissement** avec création optionnelle du compte Directeur initial | `SUPER_ADMIN` |
| `GET` | `/api/v1/super-admin/schools/{id}` | Détails complets d'un établissement | `SUPER_ADMIN` |
| `PUT` | `/api/v1/super-admin/schools/{id}` | Mise à jour des informations d'un établissement | `SUPER_ADMIN` |
| `PATCH`| `/api/v1/super-admin/schools/{id}/status` | Changement de statut (`ACTIVE`, `SUSPENDED`, `INACTIVE`) | `SUPER_ADMIN` |
| `GET` | `/api/v1/super-admin/schools/{schoolId}/admins` | Liste des comptes administratifs / directeurs d'une école | `SUPER_ADMIN` |
| `POST` | `/api/v1/super-admin/schools/{schoolId}/admins` | Création d'un compte administrateur pour une école | `SUPER_ADMIN` |
| `POST` | `/api/v1/super-admin/users/{userId}/reset-password` | **Réinitialisation du mot de passe** d'un utilisateur sur Keycloak | `SUPER_ADMIN` |
| `PATCH`| `/api/v1/super-admin/users/{userId}/status` | Activation, suspension ou désactivation d'un compte utilisateur | `SUPER_ADMIN` |

#### Exemple Requête `POST /api/v1/super-admin/schools` (Création d'une école + Administrateur initial)
```json
{
  "name": "Groupe Scolaire SABEL",
  "code": "SABEL-DAK",
  "email": "contact@sabel.sn",
  "phone": "+221338001122",
  "address": "Point E, Dakar",
  "city": "Dakar",
  "country": "Sénégal",
  "currency": "XOF",
  "adminFirstName": "Cheikh",
  "adminLastName": "Tidiane",
  "adminEmail": "directeur@sabel.sn",
  "adminPassword": "MonMotDePasse123!",
  "adminPhone": "+221770001122"
}
```

#### Exemple Réponse `GET /api/v1/super-admin/stats`
```json
{
  "totalSchools": 12,
  "activeSchools": 11,
  "suspendedSchools": 1,
  "inactiveSchools": 0,
  "totalStudents": 3450,
  "totalCollectedRevenue": 145200000.00,
  "totalSuccessfulPayments": 2180,
  "totalUsers": 28
}
```

---

## 🎓 18. Dashboard Directeur & Pilotage Financier (`/api/v1/director/dashboard`)

Module complet de pilotage pédagogique et financier multi-tenant destiné aux Directeurs d'établissement, Administrateurs et Comptables (`DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN`).

### Matrice des 14 Endpoints

| # | Méthode | Endpoint | Description | Rôles autorisés |
| :- | :--- | :--- | :--- | :--- |
| 1 | `GET` | `/api/v1/director/dashboard` | **Synthèse globale tout-en-un** (effectifs, finances, mensualité du mois, classes, impayés, échéances à venir, graphiques) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 2 | `GET` | `/api/v1/director/dashboard/enrollment` | **Statistiques globales des effectifs** (inscrits, capacité totale, places libres, taux de remplissage %, préinscriptions) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 3 | `GET` | `/api/v1/director/dashboard/classes` | **Situation détaillée de toutes les classes** (capacités, inscrits, taux de remplissage, statut financier inscription & scolarité) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 4 | `GET` | `/api/v1/director/dashboard/classes/{classId}` | **Situation détaillée d'une classe spécifique** | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 5 | `GET` | `/api/v1/director/dashboard/payments/summary` | **Synthèse financière & recouvrement** (total attendu, collecté, reste, retards, taux de recouvrement %, compteurs d'élèves à jour / partiels / impayés / en retard) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 6 | `GET` | `/api/v1/director/dashboard/registrations` | **Situation dédiée aux frais d'inscription** (`REGISTRATION`) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 7 | `GET` | `/api/v1/director/dashboard/tuition` | **Situation dédiée aux mensualités / scolarité** (`TUITION`) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 8 | `GET` | `/api/v1/director/dashboard/schedules/monthly` | **Suivi des mensualités / échéances du mois** (mois & année en paramètre, échéances échues vs payées) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 9 | `GET` | `/api/v1/director/dashboard/students/financial-status` | **Situation financière individuelle par élève** (liste paginée, filtres classe, statut `UP_TO_DATE \| PARTIALLY_PAID \| UNPAID \| OVERDUE`, recherche) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 10 | `GET` | `/api/v1/director/dashboard/payments/by-category` | **Répartition par catégorie de frais** (montants attendus, collectés, % du total collecté, taux d'encaissement) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 11 | `GET` | `/api/v1/director/dashboard/payments/by-method` | **Répartition par moyen de paiement** (Mobile Money, Espèces, Virement bancaire, Autre) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 12 | `GET` | `/api/v1/director/dashboard/payments/evolution` | **Évolution chronologique des encaissements** (paramètre `groupBy=DAY\|WEEK\|MONTH`, `dateFrom`, `dateTo`) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 13 | `GET` | `/api/v1/director/dashboard/overdue` | **Suivi des impayés et retards** (liste paginée avec ancienneté `0-7j \| 8-30j \| 30j+`, jours de retard et contact parent financier) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |
| 14 | `GET` | `/api/v1/director/dashboard/upcoming-due-dates` | **Échéancier prévisionnel des paiements à venir** (paramètre `daysAhead=30`) | `DIRECTOR`, `ADMIN`, `ACCOUNTANT`, `SUPER_ADMIN` |

---

### Exemples de Réponses JSON

#### 1. `GET /api/v1/director/dashboard` (Synthèse tout-en-un)
```json
{
  "academicYearId": "e6a18d19-21b3-4f91-a1e4-8cf145229b12",
  "academicYearName": "2026-2027",
  "currency": "XOF",
  "enrollment": {
    "academicYearId": "e6a18d19-21b3-4f91-a1e4-8cf145229b12",
    "academicYearName": "2026-2027",
    "totalStudents": 480,
    "totalClasses": 16,
    "totalCapacity": 520,
    "totalEnrolledStudents": 480,
    "totalAvailableSeats": 40,
    "globalEnrollmentRate": 92.31,
    "pendingRegistrations": 12,
    "activeRegistrations": 465,
    "completedRegistrations": 3,
    "cancelledRegistrations": 0
  },
  "payments": {
    "totalExpected": 49000000.00,
    "totalCollected": 42850000.00,
    "totalRemaining": 6150000.00,
    "totalOverdue": 1850000.00,
    "collectionRate": 87.45,
    "todayCollected": 1250000.00,
    "thisMonthCollected": 8450000.00,
    "upToDateStudentsCount": 410,
    "partialPaymentStudentsCount": 42,
    "unpaidStudentsCount": 10,
    "overdueStudentsCount": 18,
    "totalStudentsCount": 480,
    "currency": "XOF"
  },
  "registrations": {
    "feeType": "REGISTRATION",
    "feeTypeName": "Frais d'inscription",
    "totalExpected": 9600000.00,
    "totalCollected": 9500000.00,
    "totalRemaining": 100000.00,
    "totalOverdue": 100000.00,
    "collectionRate": 98.96,
    "fullyPaidStudentsCount": 475,
    "partialStudentsCount": 3,
    "unpaidStudentsCount": 2,
    "overdueStudentsCount": 5,
    "currency": "XOF"
  },
  "tuition": {
    "feeType": "TUITION",
    "feeTypeName": "Frais de scolarité / Mensualités",
    "totalExpected": 39400000.00,
    "totalCollected": 33350000.00,
    "totalRemaining": 6050000.00,
    "totalOverdue": 1750000.00,
    "collectionRate": 84.64,
    "fullyPaidStudentsCount": 410,
    "partialStudentsCount": 42,
    "unpaidStudentsCount": 10,
    "overdueStudentsCount": 18,
    "currency": "XOF"
  },
  "currentMonthSchedules": {
    "month": 10,
    "year": 2026,
    "periodLabel": "Octobre 2026",
    "totalExpected": 4800000.00,
    "totalCollected": 3950000.00,
    "totalRemaining": 850000.00,
    "totalOverdue": 850000.00,
    "collectionRate": 82.29,
    "totalSchedulesCount": 480,
    "paidSchedulesCount": 395,
    "partialSchedulesCount": 20,
    "overdueSchedulesCount": 65,
    "pendingSchedulesCount": 0,
    "upToDateStudentsCount": 415,
    "overdueStudentsCount": 65,
    "currency": "XOF"
  },
  "classes": [
    {
      "classId": "8f3b2361-5b12-4217-b7cd-2591a27e77b1",
      "className": "6ème A",
      "classCode": "6A",
      "gradeLevelName": "6ème",
      "capacity": 35,
      "enrolledCount": 33,
      "availableSeats": 2,
      "occupancyRate": 94.29,
      "pendingCount": 1,
      "activeCount": 32,
      "totalExpected": 3300000.00,
      "totalCollected": 3100000.00,
      "totalRemaining": 200000.00,
      "collectionRate": 93.94,
      "currency": "XOF"
    }
  ],
  "feeCategories": [
    {
      "feeId": "1b933cf2-38b4-4b51-93e1-85e6ba292850",
      "feeName": "Frais d'inscription 2026-2027",
      "feeCode": "INSCR-26",
      "feeType": "REGISTRATION",
      "expectedAmount": 9600000.00,
      "collectedAmount": 9500000.00,
      "remainingAmount": 100000.00,
      "overdueAmount": 100000.00,
      "percentageOfTotalCollected": 22.17,
      "collectionRate": 98.96,
      "totalSchedulesCount": 480,
      "paidSchedulesCount": 475,
      "currency": "XOF"
    }
  ],
  "paymentMethods": [
    {
      "paymentMethod": "MOBILE_MONEY",
      "paymentMethodLabel": "Mobile Money (Wave / Orange Money)",
      "totalAmount": 29850000.00,
      "transactionCount": 850,
      "percentage": 69.66,
      "currency": "XOF"
    },
    {
      "paymentMethod": "CASH",
      "paymentMethodLabel": "Espèces",
      "totalAmount": 9000000.00,
      "transactionCount": 210,
      "percentage": 21.00,
      "currency": "XOF"
    },
    {
      "paymentMethod": "BANK_TRANSFER",
      "paymentMethodLabel": "Virement bancaire",
      "totalAmount": 4000000.00,
      "transactionCount": 45,
      "percentage": 9.33,
      "currency": "XOF"
    }
  ],
  "recentEvolution": [
    {
      "period": "2026-08-20",
      "collectedAmount": 450000.00,
      "expectedAmount": 450000.00,
      "transactionCount": 12
    },
    {
      "period": "2026-08-21",
      "collectedAmount": 650000.00,
      "expectedAmount": 650000.00,
      "transactionCount": 18
    },
    {
      "period": "2026-08-22",
      "collectedAmount": 1250000.00,
      "expectedAmount": 1250000.00,
      "transactionCount": 25
    }
  ],
  "recentOverdue": [
    {
      "studentId": "09f87421-2e6b-4ca2-8db4-f027e1654a11",
      "studentNumber": "PILOTE-STD-0042",
      "studentName": "Fatou Ndiaye",
      "classId": "8f3b2361-5b12-4217-b7cd-2591a27e77b1",
      "className": "6ème A",
      "overdueAmount": 60000.00,
      "overdueSchedulesCount": 2,
      "oldestDueDate": "2026-08-05",
      "daysOverdue": 18,
      "arrearsBucket": "8-30j",
      "parentName": "Modou Ndiaye",
      "parentPhone": "+221775551122",
      "currency": "XOF"
    }
  ],
  "upcomingDueDates": [
    {
      "dueDate": "2026-09-05",
      "feeName": "Mensualité Septembre",
      "feeType": "TUITION",
      "totalAmountExpected": 4800000.00,
      "totalAmountCollected": 1200000.00,
      "remainingAmount": 3600000.00,
      "totalStudentsCount": 480,
      "paidStudentsCount": 120,
      "pendingStudentsCount": 360,
      "isToday": false,
      "daysRemaining": 13,
      "currency": "XOF"
    }
  ]
}
```

#### 2. `GET /api/v1/director/dashboard/students/financial-status` (Liste paginée)
```json
{
  "content": [
    {
      "studentId": "09f87421-2e6b-4ca2-8db4-f027e1654a11",
      "studentNumber": "PILOTE-STD-0042",
      "studentName": "Fatou Ndiaye",
      "classId": "8f3b2361-5b12-4217-b7cd-2591a27e77b1",
      "className": "6ème A",
      "totalExpected": 350000.00,
      "totalPaid": 290000.00,
      "remainingAmount": 60000.00,
      "overdueAmount": 60000.00,
      "status": "OVERDUE",
      "lastPaymentDate": "2026-07-20",
      "lastPaymentAmount": 30000.00,
      "currency": "XOF"
    },
    {
      "studentId": "7a4d5218-12ab-4ef3-9c88-142270921bc4",
      "studentNumber": "PILOTE-STD-0043",
      "studentName": "Ibrahima Diallo",
      "classId": "8f3b2361-5b12-4217-b7cd-2591a27e77b1",
      "className": "6ème A",
      "totalExpected": 350000.00,
      "totalPaid": 350000.00,
      "remainingAmount": 0.00,
      "overdueAmount": 0.00,
      "status": "UP_TO_DATE",
      "lastPaymentDate": "2026-08-15",
      "lastPaymentAmount": 50000.00,
      "currency": "XOF"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 480,
  "totalPages": 24
}
```


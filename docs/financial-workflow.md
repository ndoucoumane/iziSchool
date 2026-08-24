# Workflow Financier & Gestion des Paiements — iziSchool

## 1. Cycle de Vie Financier d'une Inscription

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Direction / Économat
    actor Parent as Parent d'élève
    participant Sys as iziSchool Backend
    participant DB as PostgreSQL
    participant MobileMoney as Wave / Orange Money

    Admin->>Sys: Inscription élève (StudentEnrollment)
    Admin->>Sys: Sélection du PaymentPlan & Frais
    Sys->>DB: Génération des PaymentSchedules (Statut PENDING)
    
    Note over Sys,DB: Échéancier prêt (ex: 10 mensualités de 60 000 FCFA)

    Parent->>MobileMoney: Paiement frais de scolarité
    MobileMoney->>Sys: Webhook confirmation paiement (providerTransactionId)
    
    Sys->>Sys: Vérification Idempotence (providerTransactionId déjà traité ?)
    Sys->>DB: Création Payment (Statut SUCCESS)
    Sys->>DB: Création PaymentAllocation(s) sur les échéances
    Sys->>DB: Recalcul des montants restants et statuts PaymentSchedule
    Sys->>DB: Génération automatique du Receipt (Reçu officiel)
    Sys->>DB: Enregistrement AuditLog
    Sys->>Parent: Notification confirmation de paiement + Reçu (SMS / WhatsApp)
```

---

## 2. Règles de Recalcul des Échéances (`PaymentSchedule`)

Pour chaque échéance ciblée :

1. **`amountPaid`** = `amountPaid` + `allocatedAmount`
2. **`remainingAmount`** = `amountDue` - `amountPaid`
3. **Transition de statut** :
   - Si `remainingAmount == 0` : `status = PAID`
   - Si `amountPaid > 0` et `remainingAmount > 0` : `status = PARTIALLY_PAID`
   - Si `currentDate > dueDate` et `remainingAmount > 0` : `status = OVERDUE`
   - Sinon : `status = PENDING`

---

## 3. Gestion de l'Idempotence des Paiements

Les webhooks des fournisseurs de paiement (Wave, Orange Money, etc.) peuvent être émis plusieurs fois en cas de latence réseau.

Le modèle iziSchool garantit l'idempotence via :
- Une contrainte unique en base de données sur `(provider, provider_transaction_id)` ;
- Une vérification en amont dans `PaymentService` :
  ```text
  Si providerTransactionId existe déjà et est SUCCESS :
      -> Retourner le paiement existant immédiatement sans nouvelle allocation ni émission de reçu en double.
  ```

---

## 4. Ventilation & Paiements Multi-Échéances

Un paiement peut :
- Payer exactement une échéance ;
- Payer partiellement une échéance ;
- Couvrir plusieurs échéances échues et à venir (ventilation FIFO automatique dans l'ordre chronologique des `dueDate`).

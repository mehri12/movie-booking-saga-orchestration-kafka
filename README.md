# Réservation de places : saga orchestrée avec Kafka

Système de réservation de places de cinéma en microservices (Spring Boot + Apache Kafka).
Une **saga orchestrée** coordonne la réservation, le verrouillage des sièges et le paiement,
avec **compensation** automatique en cas d'échec.

## Architecture

```mermaid
sequenceDiagram
    participant C as Client
    participant B as Booking
    participant O as Orchestrateur
    participant S as Seat
    participant P as Payment

    C->>B: POST /booking-service/bookSeat
    B->>B: enregistre la réservation (PENDING)
    B->>O: BookingCreatedEvent
    O->>S: RESERVE_SEATS
    S->>O: SeatReservedEvent
    alt sièges verrouillés
        O->>P: PROCESS_PAYMENT
        P->>O: BookingPaymentEvent
        alt paiement accepté
            O->>B: CONFIRM_BOOKING
        else paiement refusé
            O->>S: RELEASE_SEATS
            O->>B: FAIL_BOOKING
        end
    else sièges indisponibles
        O->>B: FAIL_BOOKING
    end
```

Les services ne communiquent jamais entre eux : chacun reçoit des **ordres** de l'orchestrateur
et lui répond. Toute la logique de la saga est regroupée dans l'orchestrateur.

## Services

| Service | Rôle | Port |
|---|---|---|
| Booking | Enregistre les réservations, change leur statut | 9191 |
| Seat | Verrouille et libère les sièges | 9292 |
| Payment | Simule le paiement (refusé au-dessus de 2000) | 9393 |
| Orchestrateur | Pilote la saga | 9494 |
| Kafka UI | Visualise les topics et les messages | 8090 |

## Topics Kafka

| Topic | Sens |
|---|---|
| `movie-booking-events` | Booking → Orchestrateur |
| `seat-command-topic` | Orchestrateur → Seat |
| `seat-reserved-topic` | Seat → Orchestrateur |
| `payment-command-topic` | Orchestrateur → Payment |
| `payment-events` | Payment → Orchestrateur |
| `booking-command-topic` | Orchestrateur → Booking |

## Technologies

Java, Spring Boot, Spring for Apache Kafka, JPA / Hibernate, MySQL, Lombok, Docker, Docker Compose.

## Lancer le projet

Prérequis : Docker Desktop.

```bash
docker compose up --build
```

Kafka UI est disponible sur http://localhost:8090.
Pour repartir de zéro (base et topics vides) : `docker compose down`, puis relancer.

## Scénarios de test

La réponse HTTP renvoie `PENDING` : le statut final (`CONFIRMED` ou `FAILED`)
est écrit en base quelques instants plus tard, car la saga est asynchrone.

**1. Succès** (sièges libres, montant ≤ 2000)

```bash
curl -X POST http://localhost:9191/booking-service/bookSeat \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","showId":"SHOW_101","seatIds":["A1","A2"],"amount":1000}'
```

**2. Paiement refusé** (montant > 2000) : les sièges sont libérés, la réservation passe à `FAILED`

```bash
curl -X POST http://localhost:9191/booking-service/bookSeat \
  -H "Content-Type: application/json" \
  -d '{"userId":"user2","showId":"SHOW_101","seatIds":["A5"],"amount":2500}'
```

**3. Siège déjà pris** : la saga s'arrête avant le paiement, la réservation passe à `FAILED`

```bash
curl -X POST http://localhost:9191/booking-service/bookSeat \
  -H "Content-Type: application/json" \
  -d '{"userId":"user3","showId":"SHOW_101","seatIds":["A3"],"amount":500}'
```

## Ce que j'ai ajouté

- Un **service orchestrateur** qui remplace la chorégraphie initiale (les services s'écoutaient entre eux)
- Un protocole d'ordres et de réponses via Kafka, avec la compensation (libération des sièges) centralisée
- La réservation créée en `PENDING`, puis confirmée ou échouée sur ordre de l'orchestrateur
- La correction d'une double publication de l'événement de paiement réussi
- La conteneurisation : Dockerfiles multi-stage et `docker-compose.yml` pour tout lancer en une commande

## Limites connues et pistes d'amélioration

- L'orchestrateur ne conserve pas l'état des sagas (pas de reprise après un plantage)
- Pas de retries ni de Dead Letter Topic sur les consommateurs
- Les sièges restent `LOCKED` après un paiement réussi (jamais `RESERVED`)
- Pas encore de tests automatisés ni de CI


#!/usr/bin/env bash
# Test de bout en bout : les scénarios de la saga sur le système complet (docker compose)
set -uo pipefail

URL="http://localhost:9191/booking-service/bookSeat"
ECHECS=0

# Exécute une requête SQL dans le conteneur MySQL et renvoie le résultat
sql() {
  docker compose exec -T mysql mysql -uroot -proot -N -B -e "$1" 2>/dev/null | tr -d '\r\n'
}

# Attend que les listeners Kafka d'un service soient prêts
attendre_listeners() {
  local service="$1" attendu="$2" nb=0
  for _ in $(seq 1 40); do
    nb=$(docker compose logs "$service" 2>/dev/null | grep -c "partitions assigned" || true)
    if [ "${nb:-0}" -ge "$attendu" ]; then
      echo "OK    $service est prêt"
      return 0
    fi
    sleep 3
  done
  echo "ERREUR $service n'est pas prêt"
  return 1
}

# Attend qu'une requête SQL renvoie la valeur attendue (30 essais de 2 secondes)
attendre_valeur() {
  local description="$1" requete="$2" attendu="$3" valeur=""
  for _ in $(seq 1 30); do
    valeur=$(sql "$requete")
    if [ "$valeur" = "$attendu" ]; then
      echo "OK    $description : $valeur"
      return 0
    fi
    sleep 2
  done
  echo "ECHEC $description : attendu '$attendu', obtenu '$valeur'"
  ECHECS=$((ECHECS + 1))
  return 1
}

# Envoie une réservation et affiche le code de réservation reçu
reserver() {
  curl -s -X POST "$URL" -H "Content-Type: application/json" -d "$1" \
    | grep -o '"reservationId":"[^"]*"' | cut -d'"' -f4
}

echo "=== Attente du démarrage des services ==="
attendre_listeners booking-service 1 || exit 1
attendre_listeners seat-service 1 || exit 1
attendre_listeners payment-service 1 || exit 1
attendre_listeners orchestrator-service 3 || exit 1

echo
echo "=== Scénario 1 : tout réussit (A1, A2, 1000) ==="
ID=$(reserver '{"userId":"user1","showId":"SHOW_101","seatIds":["A1","A2"],"amount":1000}')
[ -n "$ID" ] || { echo "ECHEC aucun code de réservation reçu"; exit 1; }
attendre_valeur "réservation $ID" "SELECT status FROM mojtabahjava.Booking WHERE bookingCode='$ID';" "CONFIRMED"
attendre_valeur "siège A1" "SELECT status FROM mojtabahjava.seat_inventory WHERE showId='SHOW_101' AND seatNumber='A1';" "LOCKED"

echo
echo "=== Scénario 2 : paiement refusé (A5, 2500) ==="
ID=$(reserver '{"userId":"user2","showId":"SHOW_101","seatIds":["A5"],"amount":2500}')
[ -n "$ID" ] || { echo "ECHEC aucun code de réservation reçu"; exit 1; }
attendre_valeur "réservation $ID" "SELECT status FROM mojtabahjava.Booking WHERE bookingCode='$ID';" "FAILED"
attendre_valeur "siège A5 libéré" "SELECT status FROM mojtabahjava.seat_inventory WHERE showId='SHOW_101' AND seatNumber='A5';" "AVAILABLE"

echo
echo "=== Scénario 3 : siège déjà pris (A3, 500) ==="
ID=$(reserver '{"userId":"user3","showId":"SHOW_101","seatIds":["A3"],"amount":500}')
[ -n "$ID" ] || { echo "ECHEC aucun code de réservation reçu"; exit 1; }
attendre_valeur "réservation $ID" "SELECT status FROM mojtabahjava.Booking WHERE bookingCode='$ID';" "FAILED"
attendre_valeur "siège A3 inchangé" "SELECT status FROM mojtabahjava.seat_inventory WHERE showId='SHOW_101' AND seatNumber='A3';" "LOCKED"

echo
if [ "$ECHECS" -eq 0 ]; then
  echo "Tous les scénarios sont passés."
else
  echo "$ECHECS vérification(s) en échec."
  exit 1
fi
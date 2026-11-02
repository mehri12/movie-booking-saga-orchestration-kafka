#!/usr/bin/env bash
# Prépare l'infrastructure (MySQL, Kafka, Secrets) dans un cluster vide.
# Utilisé par la CI : les mots de passe sont aléatoires et n'existent que
# dans ce cluster éphémère.
set -euo pipefail

NS=saga

kubectl apply -f k8s/00-namespace.yml

ROOT_PW=$(openssl rand -base64 18 | tr -d '/+=')
APP_PW=$(openssl rand -base64 18 | tr -d '/+=')

# Les Secrets sont créés en ligne de commande, jamais depuis un fichier.
kubectl create secret generic mysql-secret -n "$NS" \
  --from-literal=MYSQL_ROOT_PASSWORD="$ROOT_PW"
kubectl create secret generic mysql-app-secret -n "$NS" \
  --from-literal=MYSQL_APP_PASSWORD="$APP_PW"

kubectl apply -f k8s/mysql-pvc.yml -f k8s/kafka-pvc.yml
kubectl apply -f k8s/10-mysql.yml -f k8s/20-kafka.yml

kubectl rollout status deployment/mysql -n "$NS" --timeout=300s
kubectl rollout status deployment/kafka -n "$NS" --timeout=300s

# Attendre que MySQL accepte les connexions réseau. Pendant son
# initialisation, MySQL ne répond que sur un socket local : tester en TCP
# (-h 127.0.0.1) évite de s'adresser au serveur temporaire.
echo "Attente de MySQL..."
for attempt in $(seq 1 60); do
  if kubectl exec -n "$NS" deploy/mysql -- sh -c \
    'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' \
    >/dev/null 2>&1; then
    break
  fi
  if [ "$attempt" -eq 60 ]; then
    echo "ECHEC MySQL n'est pas prêt après 3 minutes"
    exit 1
  fi
  sleep 3
done

# Les topics sont créés avant les services, avec 3 partitions.
kubectl apply -f k8s/21-kafka-topics-job.yml
kubectl wait --for=condition=complete job/kafka-init -n "$NS" --timeout=300s

# Compte MySQL dédié aux applications (comme à l'étape 2.2).
kubectl exec -i -n "$NS" deploy/mysql -- sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' <<SQL
CREATE USER IF NOT EXISTS 'saga_app'@'%' IDENTIFIED BY '$APP_PW';
GRANT ALL PRIVILEGES ON mojtabahjava.* TO 'saga_app'@'%';
FLUSH PRIVILEGES;
SQL

echo "Infrastructure prête."
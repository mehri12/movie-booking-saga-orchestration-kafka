# Étape 1 : construire l'application avec Node.
# Cette étape ne fait pas partie de l'image finale : Node et node_modules
# n'y seront pas.
FROM node:20-alpine AS build
WORKDIR /app

# Les dépendances d'abord : cette couche est réutilisée tant que
# package.json et package-lock.json ne changent pas.
COPY package.json package-lock.json ./
RUN npm ci

# Puis le code source, et la construction des fichiers statiques.
COPY . .
RUN npm run build

# Étape 2 : servir les fichiers avec nginx.
# Cette variante de nginx tourne sans droits root et écoute sur le port 8080.
FROM nginxinc/nginx-unprivileged:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 8080
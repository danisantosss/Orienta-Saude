# Etapa 1: gera o build estático com o Vite
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Vazio: o frontend chama /api no mesmo endereço e o nginx repassa ao backend
ENV VITE_API_URL=""
RUN npm run build

# Etapa 2: nginx serve os arquivos e faz o proxy de /api
FROM nginx:1.27-alpine

# O nginx substitui ${BACKEND_URL} no template ao iniciar o contêiner
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

ENV BACKEND_URL=http://backend:8080
EXPOSE 80

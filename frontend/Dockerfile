# Build stage: Node 20
FROM node:20 AS build
WORKDIR /app
# copy lockfiles and package.json first to cache install
COPY package*.json ./
RUN npm ci
COPY . .
# build production bundle (Angular configuration uses outputPath "dist/frontend")
RUN npm run build -- --configuration production

# Serve with nginx
FROM nginx:stable-alpine
# AQUÍ ESTÁ EL CAMBIO: Agregamos /browser al final
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
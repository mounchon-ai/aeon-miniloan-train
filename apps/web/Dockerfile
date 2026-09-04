FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
ARG API_URL=http://localhost:5000
RUN npm run build -- --configuration=production

FROM nginx:alpine AS final
COPY --from=build /app/dist/miniloan-web/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

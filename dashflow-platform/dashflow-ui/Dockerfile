# Multi-stage build for dashflow-ui (static + nginx /api proxy).
# Build from repo root:
#   docker build -f dashflow-ui/Dockerfile -t dashflow/ui:local .

FROM node:20-alpine AS build
WORKDIR /app

COPY dashflow-ui/package.json dashflow-ui/package-lock.json* ./
RUN npm ci

COPY dashflow-ui/ ./
# Production UI talks to API via nginx same-origin /api proxy (no VITE_API_BASE).
ENV VITE_ENABLE_MSW=false
RUN npm run build

FROM nginx:1.27-alpine
COPY dashflow-ui/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --omit=dev

COPY openapi.json ./
COPY sql ./sql
COPY src ./src

ENV HOST=0.0.0.0
ENV PORT=4010
ENV MYSQL_HOST=mysql
ENV MYSQL_PORT=3306
ENV MYSQL_USER=pipeline
ENV MYSQL_PASSWORD=pipeline
ENV MYSQL_DATABASE=petstore
ENV MYSQL_ROOT_USER=root
ENV MYSQL_ROOT_PASSWORD=root

EXPOSE 4010

CMD ["sh", "-c", "node src/migrate.js && node src/index.js"]

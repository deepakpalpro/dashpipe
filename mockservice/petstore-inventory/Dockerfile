FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --omit=dev

COPY data ./data
COPY src ./src

ENV HOST=0.0.0.0
ENV PORT=4011

EXPOSE 4011

CMD ["node", "src/index.js"]

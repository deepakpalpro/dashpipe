import 'dotenv/config'

const toInt = (value, fallback) => {
  const n = Number.parseInt(value ?? '', 10)
  return Number.isFinite(n) ? n : fallback
}

export const config = {
  port: toInt(process.env.PORT, 4010),
  host: process.env.HOST ?? '0.0.0.0',
  mysql: {
    host: process.env.MYSQL_HOST ?? '127.0.0.1',
    port: toInt(process.env.MYSQL_PORT, 3306),
    user: process.env.MYSQL_USER ?? 'pipeline',
    password: process.env.MYSQL_PASSWORD ?? 'pipeline',
    database: process.env.MYSQL_DATABASE ?? 'petstore',
    // Root is used only by db:migrate to create the database if missing.
    rootUser: process.env.MYSQL_ROOT_USER ?? 'root',
    rootPassword: process.env.MYSQL_ROOT_PASSWORD ?? 'root',
  },
}

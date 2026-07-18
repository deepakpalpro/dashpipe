-- Create dedicated petstore database on shared MySQL (first-time volume init).
CREATE DATABASE IF NOT EXISTS petstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON petstore.* TO 'pipeline'@'%';
FLUSH PRIVILEGES;

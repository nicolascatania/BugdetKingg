-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: budgetking
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `accounts`
--

DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accounts` (
  `id` char(36) NOT NULL,
  `balance` decimal(19,2) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `user_id` char(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(255) NOT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnjuop33mo69pd79ctplkck40n` (`user_id`),
  CONSTRAINT `FKnjuop33mo69pd79ctplkck40n` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accounts`
--

LOCK TABLES `accounts` WRITE;
/*!40000 ALTER TABLE `accounts` DISABLE KEYS */;
INSERT INTO `accounts` VALUES ('037f6cf8-259d-4ccb-a9f2-4d18009de96c',0.00,'aqwe','aaaaa','57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21',1374203.27,'Cuenta remunerada en pesos','MercadoPago','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-27 21:37:16','','nicolas20032401@gmail.com'),('71b0ad3a-c906-4f53-8696-199bde90cc49',123123.00,'123','Xd','57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('8b555df1-b8c0-45fa-a256-96adcf314ffa',246.00,'123123','XDDDDD123','57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:13:56','2026-01-19 20:26:48','','admin@mail.com'),('9b0fdd90-1b31-4677-9a7f-3ee64ed25a69',1234620.98,'Cuenta Remunerada en pesos','Supervielle','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-27 21:37:16','','nicolas20032401@gmail.com'),('9bb945d4-92b3-431d-a992-524ce89bdc68',9900.00,'$$$$','Cash','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:11:53','2026-01-27 21:34:52','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('b5cd0473-2e0e-4aee-b5ce-7059d71ce8e6',1230.00,'123','xd','57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:13:56','2026-01-19 20:26:02','','admin@mail.com'),('c91f5fed-9b53-42e8-a195-1c92bd7fe6b9',900000.00,'Aca solo pongo lo que voy transfiriendo','IOL','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-26 20:12:59','','nicolas20032401@gmail.com');
/*!40000 ALTER TABLE `accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` char(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `user_id` char(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(255) NOT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKghuylkwuedgl2qahxjt8g41kb` (`user_id`),
  CONSTRAINT `FKghuylkwuedgl2qahxjt8g41kb` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES ('238275dd-f092-499f-8beb-9a66abf230b6','Jueguitos','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('3bd0e008-28b4-4f68-9e0d-4c84d9b23862','Comida','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('3eb23a13-87c1-4072-b4a0-974b18dea4be','Sube','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:17:15','2026-01-26 20:17:15','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('44be5668-7761-4c54-9df2-187efcab8866','Salario','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('58fcba42-3927-4f16-900c-39e64ec4bc2c','Autoregalos','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('65579136-5f32-4d82-bf96-71c4ed9e19e8','XD','57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('a8777917-7c6b-404d-b491-e6ebc6250d13','Farmacia','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('aedd7623-74eb-43be-a107-32f818de2848','Musica','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('c40b2cf7-1d0d-443f-9b4f-76ab4987ad85','Remuneracion de cta.cte','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('e0702352-c8bc-4096-8c1f-460c21051462','Inversiones','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('f70acb37-eb01-485d-bc8b-553d5a4402f6','Electronico','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL);
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` char(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES ('cc37ca1e-47c5-4a27-bff7-765ca2da4478','ROLE_ADMIN'),('6172f328-b962-4d7a-8531-9eba70c0d383','ROLE_USER');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
  `id` char(36) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `counterparty` varchar(255) DEFAULT NULL,
  `date` datetime(6) NOT NULL,
  `description` varchar(255) NOT NULL,
  `type` enum('EXPENSE','INCOME','TRANSFER') NOT NULL,
  `account_id` char(36) NOT NULL,
  `category_id` char(36) DEFAULT NULL,
  `destination_account_id` char(36) DEFAULT NULL,
  `user_id` char(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(255) NOT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK20w7wsg13u9srbq3bd7chfxdh` (`account_id`),
  KEY `FKsqqi7sneo04kast0o138h19mv` (`category_id`),
  KEY `FK5598b948ilps8u4o3qvfo4j52` (`destination_account_id`),
  KEY `FKqwv7rmvc8va8rep7piikrojds` (`user_id`),
  CONSTRAINT `FK20w7wsg13u9srbq3bd7chfxdh` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `FK5598b948ilps8u4o3qvfo4j52` FOREIGN KEY (`destination_account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `FKqwv7rmvc8va8rep7piikrojds` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsqqi7sneo04kast0o138h19mv` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactions`
--

LOCK TABLES `transactions` WRITE;
/*!40000 ALTER TABLE `transactions` DISABLE KEYS */;
INSERT INTO `transactions` VALUES ('0b9b69f9-3fb1-4b89-b43d-139ac52db427',3000.00,'Niki','2026-01-16 22:28:00.000000','Tostado, transf a Niki','EXPENSE','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('129231f3-22c1-415f-b106-0f85ea4c7eb9',123.00,'123','2025-12-31 20:15:00.000000','123','INCOME','8b555df1-b8c0-45fa-a256-96adcf314ffa','65579136-5f32-4d82-bf96-71c4ed9e19e8',NULL,'57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:16:28','2026-01-19 20:16:28','admin@mail.com','admin@mail.com'),('1d0c1ef6-1c8b-44ec-a474-1a0ead3dfea3',1230.00,'123','2026-01-19 20:19:00.000000','123','TRANSFER','8b555df1-b8c0-45fa-a256-96adcf314ffa',NULL,'b5cd0473-2e0e-4aee-b5ce-7059d71ce8e6','57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:26:02','2026-01-19 20:26:02','admin@mail.com','admin@mail.com'),('24469f16-f5ca-4d57-bfc8-c24558e940e2',33719.70,'Central Oeste','2026-01-26 20:15:00.000000','Sup. Vitaminico','EXPENSE','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','a8777917-7c6b-404d-b491-e6ebc6250d13',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:15:24','2026-01-26 20:15:24','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('46c40772-acf6-4dbd-b38e-0e64f51c0512',64500.00,'Restaurante Raices','2026-01-23 23:16:00.000000','Comida salida','EXPENSE','9b0fdd90-1b31-4677-9a7f-3ee64ed25a69','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:16:37','2026-01-26 20:16:37','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('62645353-7dd7-4928-8a68-553cc696dd76',3000.00,'-','2026-01-16 22:28:00.000000','Test','INCOME','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('69cb5e81-f3e6-4878-8c4c-e1fe51856a59',100000.00,'Tia','2026-01-26 20:13:00.000000','Regalo cumpleanos tia','INCOME','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','58fcba42-3927-4f16-900c-39e64ec4bc2c',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:13:38','2026-01-26 20:13:38','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('6ce63e6d-2ae7-472a-8fd7-1d0095da7831',15000.00,'Martino, Gero, Bren','2026-01-26 20:14:00.000000','devolucion trasnf','INCOME','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:14:40','2026-01-26 20:14:40','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('776d2f99-ab33-4b37-8d04-9be6b11e6bc7',1100.00,'kiosko','2026-01-27 07:34:00.000000','mentoplus','EXPENSE','9bb945d4-92b3-431d-a992-524ce89bdc68','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-27 21:34:52','2026-01-27 21:34:52','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('8dedc09b-ff9b-4026-b08b-2f3354a37eb6',100000.00,'Yo','2026-01-15 20:12:00.000000','Inversion','TRANSFER','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21',NULL,'c91f5fed-9b53-42e8-a195-1c92bd7fe6b9','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:12:59','2026-01-26 20:12:59','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('92271e01-db6b-4600-a4a6-afabebedfdc4',12800.00,'Niki','2026-01-23 20:17:00.000000','Comida viernes','EXPENSE','9b0fdd90-1b31-4677-9a7f-3ee64ed25a69','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:18:01','2026-01-26 20:18:01','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('942273c4-5287-4d7d-b428-cd4809f906b4',77.53,'-','2026-01-16 22:30:00.000000','Fix de calculo','EXPENSE','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('9de31552-c00a-44cf-b64b-ff45b6e0b906',1322000.50,'-','2026-01-16 22:25:00.000000','Primera tsx','INCOME','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','44be5668-7761-4c54-9df2-187efcab8866',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('a007180b-e582-431b-96e0-1359154bda50',1230.00,'123','2026-01-19 20:26:00.000000','213121','INCOME','8b555df1-b8c0-45fa-a256-96adcf314ffa','65579136-5f32-4d82-bf96-71c4ed9e19e8',NULL,'57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:26:48','2026-01-19 20:26:48','admin@mail.com','admin@mail.com'),('a123ddea-7f97-4bea-b636-c0852dafbd40',11000.00,'Martino','2026-01-23 20:12:00.000000','Devolucion parcial de pago x transf','INCOME','9bb945d4-92b3-431d-a992-524ce89bdc68','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:12:29','2026-01-26 20:12:29','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('b08d56fa-fdb2-41fd-bff1-9b606ed63cad',123.00,'123123','2026-01-19 19:09:00.000000','12312','INCOME','8b555df1-b8c0-45fa-a256-96adcf314ffa','65579136-5f32-4d82-bf96-71c4ed9e19e8',NULL,'57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('bf4828b8-735d-4b9e-a0dc-3e8eeb145f86',123123.00,'23','2026-01-07 00:58:00.000000','123','INCOME','71b0ad3a-c906-4f53-8696-199bde90cc49','65579136-5f32-4d82-bf96-71c4ed9e19e8',NULL,'57cd0f38-cafe-4a4a-a418-54d363c04e3d','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('cdf4c2b9-c7ce-4654-af8e-392d3ee154e4',4000.00,'Sube','2026-01-23 20:17:00.000000','Carga sube','EXPENSE','9b0fdd90-1b31-4677-9a7f-3ee64ed25a69','3eb23a13-87c1-4072-b4a0-974b18dea4be',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-26 20:17:36','2026-01-26 20:17:36','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('e4d4b135-8424-4a0e-abf5-9c029a7ef9a2',100000.00,'Yo mismo','2026-01-27 21:36:00.000000','Reparacion por la transferencia erronea, transferi 100k desde mp a iol pero fue de sv a iol','TRANSFER','9b0fdd90-1b31-4677-9a7f-3ee64ed25a69',NULL,'2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-27 21:37:16','2026-01-27 21:37:16','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('ed8d82cf-a57a-4f97-b7d8-53cbbb60643e',29000.00,'empanadastpronto','2026-01-27 12:35:00.000000','Pizzas oficina','EXPENSE','2dcf3bfa-6a68-4657-a5a8-bec2aeffdc21','3bd0e008-28b4-4f68-9e0d-4c84d9b23862',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-27 21:35:34','2026-01-27 21:35:34','nicolas20032401@gmail.com','nicolas20032401@gmail.com'),('edb61018-d331-47ad-b013-b56163ef116e',1415920.98,'-','2026-01-16 22:26:00.000000','Primera tsx','INCOME','9b0fdd90-1b31-4677-9a7f-3ee64ed25a69','44be5668-7761-4c54-9df2-187efcab8866',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL),('f29ca579-b316-45f9-a328-1eb454832ad5',800000.00,'Yo mismo, inversion que ya tenia','2026-01-16 22:28:00.000000','IOL, , total valorizado - ganancia, masomenos lo que calculo que tendria','INCOME','c91f5fed-9b53-42e8-a195-1c92bd7fe6b9','e0702352-c8bc-4096-8c1f-460c21051462',NULL,'f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','2026-01-19 20:13:56','2026-01-19 20:13:56','',NULL);
/*!40000 ALTER TABLE `transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` char(36) NOT NULL,
  `role_id` char(36) NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES ('96a508a1-ae96-4cdf-8d98-acc28c04fd8c','6172f328-b962-4d7a-8531-9eba70c0d383'),('ec03236e-4f53-4584-95a8-26a7bf852f1d','6172f328-b962-4d7a-8531-9eba70c0d383'),('f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','6172f328-b962-4d7a-8531-9eba70c0d383'),('57cd0f38-cafe-4a4a-a418-54d363c04e3d','cc37ca1e-47c5-4a27-bff7-765ca2da4478');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` char(36) NOT NULL,
  `email` varchar(255) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('57cd0f38-cafe-4a4a-a418-54d363c04e3d','admin@mail.com',_binary '','admin','admin','$2a$10$Gmf4FID4ipJENeXOZq4EvukbmXvm/H46Znuh2e59Rx/Vl/Mp19DUe'),('96a508a1-ae96-4cdf-8d98-acc28c04fd8c','test123@mail.com',_binary '','testing','Test','$2a$10$ze4XFUhS0dvfAvC3WF8f1uTPNuiL8FrMPOIiClaTfgBlwl4Vd/54i'),('ec03236e-4f53-4584-95a8-26a7bf852f1d','t2@gmail.com',_binary '','Test2','Test2','$2a$10$h1cscxHLr91/BdchagA4qOP5LybwNHplqFi5msyezXctTa37v2CLy'),('f3de3dd7-710d-4af4-81ed-4ba6dc2e7d65','nicolas20032401@gmail.com',_binary '','Catania','Nicolas','$2a$10$bDqX7lZvYqgQ0AP78Dp7NOhpPliIbeAYs0CMS7qlxMhxwwt5AJ7Gu');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-29 21:07:12

CREATE TABLE `bal_log` (
	`id` INT(10) NOT NULL AUTO_INCREMENT,
	`player` VARCHAR(16) NOT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`uuid` VARCHAR(36) NOT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`amount` DOUBLE NOT NULL DEFAULT '0',
	`server` VARCHAR(16) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`deposit` TINYINT(1) NULL DEFAULT '1',
	`command` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',
	`date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `money_log_id_uuid_player_index` (`id`, `uuid`, `player`) USING BTREE
)
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=INNODB
;
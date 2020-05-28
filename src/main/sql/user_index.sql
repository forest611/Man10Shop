CREATE TABLE `user_index` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `uuid` VARCHAR(36) NOT NULL,
    `player` VARCHAR(16) NOT NULL,
    `profit` DOUBLE NOT NULL DEFAULT 0 COMMENT '利益',
    `received` TINYINT(1) NOT NULL DEFAULT 0,
    `date` DATETIME NOT NULL DEFAULT now(),
    INDEX `uuid` (`uuid`),
    PRIMARY KEY (`id`)
)
COLLATE='utf8mb4_0900_ai_ci'
;
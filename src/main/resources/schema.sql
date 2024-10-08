CREATE TABLE IF NOT EXISTS `domain` (
	`id` INT(10) NOT NULL AUTO_INCREMENT,
	`path` VARCHAR(128) NOT NULL,
	`name` VARCHAR(128) DEFAULT NULL,
	`comment` VARCHAR(512) DEFAULT NULL,
	`created` TIMESTAMP(3) NOT NULL,
	`lastmodified` TIMESTAMP(3) NOT NULL,
	`status` ENUM('CREATED','MODIFIED','DELETED') NOT NULL,
	PRIMARY KEY (`id`),
	INDEX `domain__key_path` (`path`) USING BTREE
);

CREATE TABLE IF NOT EXISTS `user` (
	`id` INT(10) NOT NULL AUTO_INCREMENT,
	`login` VARCHAR(32) NOT NULL,
	`password_sha` VARCHAR(128) NOT NULL,
	`password_reset_token` VARCHAR(128) NULL,
	`admin` BOOLEAN NOT NULL,
	`fullname` VARCHAR(128) NOT NULL,
	`affiliation` VARCHAR(128) NULL,
	`email` VARCHAR(128) NULL DEFAULT NULL,
	`comment` VARCHAR(512) NULL DEFAULT NULL,
	`created` TIMESTAMP(3) NOT NULL,
	`lastmodified` TIMESTAMP(3) NOT NULL,
	`status` ENUM('CREATED','MODIFIED','DELETED') NOT NULL,
	PRIMARY KEY (`id`),
	UNIQUE INDEX `user__idx_login` (`login`) USING BTREE
);

CREATE TABLE IF NOT EXISTS `domainuser` (
	`id` INT(10) NOT NULL AUTO_INCREMENT,
	`domain_id` INT(10) NOT NULL,
	`user_id` INT(10) NOT NULL,
	`can_create` BOOLEAN NOT NULL,
	`can_modify` BOOLEAN NOT NULL,
	PRIMARY KEY (`id`),
	INDEX `domainuser__key_domain_id` (`domain_id`) USING BTREE,
	INDEX `domainuser__key_user_id` (`user_id`) USING BTREE
);

CREATE TABLE IF NOT EXISTS `purl` (
	`id` INT(10) NOT NULL AUTO_INCREMENT,
	`path` VARCHAR(512) NOT NULL,
	`domain_id` INT(10) NOT NULL,
	`type` ENUM('REDIRECT_302','PARTIAL_302','GONE_410') NOT NULL,
	`target` VARCHAR(2048) NOT NULL,
	`created` TIMESTAMP(3) NOT NULL,
	`lastmodified` TIMESTAMP(3) NOT NULL,
	`status` ENUM('CREATED','MODIFIED','DELETED') NOT NULL,
	PRIMARY KEY (`id`),
	INDEX `purl__key_path` (`path`) USING BTREE
);

CREATE TABLE IF NOT EXISTS `purlhistory` (
	`id` INT(10) NOT NULL AUTO_INCREMENT,
	`purl_id` INT(10) NOT NULL,
	`user_id` INT(10) NOT NULL,
	`type` ENUM('REDIRECT_302','PARTIAL_302','GONE_410') NOT NULL,
	`target` VARCHAR(2048) NOT NULL,
	`modified` TIMESTAMP(3) NOT NULL,
	`status` ENUM('CREATED','MODIFIED','DELETED') NOT NULL,
	PRIMARY KEY (`id`)
);

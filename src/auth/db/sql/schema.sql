-- :name mysql-create-users-table
-- :command :execute
-- :result :raw
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `fullname` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255),
  `reset` boolean,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_user_username` (`username`)
) DEFAULT CHARSET=utf8;

-- :name mysql-create-tenant-table
-- :command :execute
-- :result :raw
CREATE TABLE `tenant` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `config` blob,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_tenant_name` (`name`)
) DEFAULT CHARSET=utf8;

-- :name mysql-create-tenant-user-table
-- :command :execute
-- :result :raw
CREATE TABLE `tenant_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tenant_id` int,
  `user_id` int,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_tenant_user_ids` (`tenant_id`, `user_id`)
) DEFAULT CHARSET=utf8;

-- :name mysql-create-role-table
-- :command :execute
-- :result :raw
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tenant_id` int,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_role_tenant_id_name` (`tenant_id`, `name`)
) DEFAULT CHARSET=utf8;

-- :name mysql-create-capability-table
-- :command :execute
-- :result :raw
CREATE TABLE `capability` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_capability_name` (`name`)
) DEFAULT CHARSET=utf8;

-- :name mysql-create-role-capability-table
-- :command :execute
-- :result :raw
CREATE TABLE `role_capability` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int,
  `capability_id` int,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_role_capability_ids` (`role_id`, `capability_id`)
) DEFAULT CHARSET=utf8;

-- :name mysql-create-tenant-user-role-table
-- :command :execute
-- :result :raw
CREATE TABLE `tenant_user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tenant_user_id` int,
  `role_id` int,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_tenant_user_role_ids` (`tenant_user_id`, `role_id`)
) DEFAULT CHARSET=utf8;

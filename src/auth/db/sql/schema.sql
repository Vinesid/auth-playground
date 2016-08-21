-- :name create-users-table
-- :command :execute
-- :result :raw
-- :doc Create users table
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `fullname` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255),
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) DEFAULT CHARSET=utf8;
DROP TABLE IF EXISTS `t_conf`;
CREATE TABLE `t_conf` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `conf_name` varchar(255) NOT NULL DEFAULT '',
  `conf_value` text,
  `conf_version` int unsigned not null default 0,
  PRIMARY KEY (`id`)
);

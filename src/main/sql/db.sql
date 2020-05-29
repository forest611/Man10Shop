create table admin_shop_list
(
	id int auto_increment,
	player varchar(16) null,
	uuid varchar(36) null,
	server varchar(16) null,
	world varchar(16) null,
	locX int null,
	locY int null,
	locZ int null,
	shop_container text null,
	create_date datetime default now() null,
	buy tinyint default 0 null comment '販売:1 買取:0',
    price double default 0 null,
	constraint admin_shop_list_pk
		primary key (id)
);

create index admin_shop_list_id_uuid_player_index
	on admin_shop_list (id, uuid, player);

create table log
(
	id int auto_increment,
	player varchar(16) null,
	uuid varchar(36) null,
	date datetime default now() null,
	server varchar(16) null,
	world varchar(16) null,
	locX double null,
	locY double null,
	locZ double null,
	note varchar(32) null,
	price double null,
	constraint log_pk
		primary key (id)
);

create index log_player_uuid_index
	on log (player, uuid);

create table merchant_shop_list
(
	id int auto_increment,
	create_player varchar(16) null,
	uuid varchar(36) null,
	server varchar(16) null,
	world varchar(16) null,
	locX int null,
	locY int null,
	locZ int null,
	shop_item text null,
	create_date datetime default now() null,
	constraint merchant_shop_list_pk
		primary key (id)
);

create index merchant_shop_list_id_index
	on merchant_shop_list (id);


create table op_log
(
	id int auto_increment,
	date datetime default now() null,
	player varchar(16) null,
	uuid varchar(36) null,
	server varchar(16) null,
	world varchar(16) null,
	locX double null,
	locY double null,
	locZ double null,
	shop_id int null,
	note varchar(32) null,
	constraint op_log_pk
		primary key (id)
);

create index op_log_id_uuid_player_index
	on op_log (id, uuid, player);

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

create table user_shop_list
(
	id int auto_increment,
	player varchar(16) null,
	uuid varchar(36) null,
	server varchar(16) null,
	world varchar(16) null,
	locX int null,
	locY int null,
	locZ int null,
	shop_container text null,
	create_date datetime default now() null,
	buy tinyint default 0 null comment '販売:1 買取:0',
	price double default 0 null,
	constraint user_shop_list_pk
		primary key (id)
);

create index user_shop_list_id_uuid_player_index
	on user_shop_list (id, uuid, player);


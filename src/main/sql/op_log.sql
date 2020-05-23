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


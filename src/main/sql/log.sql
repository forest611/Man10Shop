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
	logType varchar(16) null,
	note varchar(32) null,
	price double null,
	constraint log_pk
		primary key (id)
);

create index log_player_uuid_index
	on log (player, uuid);


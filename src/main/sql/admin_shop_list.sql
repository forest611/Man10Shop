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


create table money_shop_list
(
	id int auto_increment,
	owner_player varchar(16) null,
	owner_uuid varchar(36) null,
	create_date datetime default now() null,
	server varchar(16) null,
	world varchar(16) null,
	locX int null,
	locY int null,
	locZ int null,
	price double null,
	shop_item text null,
	type varchar(16) null comment 'Admin:Sell Admin:Buy User:Sell User:Buy',
	constraint money_shop_list_pk
		primary key (id)
);

create index money_shop_list_id_owner_uuid_owner_player_index
	on money_shop_list (id, owner_uuid, owner_player);
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



create table public.users
(
	id bigserial not null
		constraint users_pk
			primary key,
	name varchar(50) not null,
	active boolean default true not null
);

create table public.phones
(
	id bigserial not null
		constraint phones_pk
			primary key,
	phone varchar(100) not null,
	user_id bigint not null
);

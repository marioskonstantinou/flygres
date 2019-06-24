alter table public.phones
	add constraint phones_users_id_fk
		foreign key (user_id) references public.users (id);

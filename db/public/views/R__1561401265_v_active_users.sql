create or replace view public.v_active_users as
select users.id, users.name, p.phone
from public.users
         inner join public.phones p on users.id = p.user_id
where users.active = true;

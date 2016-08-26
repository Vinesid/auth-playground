-- :name select-tenant :? :1
select name, config from tenant
where name = :name

-- :name select-tenants :? :n
select name, config from tenant
order by name

-- :name insert-tenant :! :n
insert into tenant (name, config)
values (:name, :config)

-- :name rename-tenant :! :n
update tenant
set name = :new-name
where name = :name

-- :name update-tenant :! :n
update tenant
set name = :name, config = :config
where name = :name

-- :name delete-tenant :! :n
delete from tenant
where name = :name

-- :name select-users-by-tenant :? :n
select username, fullname, email from user u
inner join tenant_user tu on u.id = tu.user_id
inner join tenant t on tu.tenant_id = t.id
where t.name = :name
order by username



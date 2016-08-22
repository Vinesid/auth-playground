-- :name select-tenant :? :1
select name, config from tenant
where name = :name

-- :name select-tenants :? :n
select name, config from tenant
order by name

-- :name select-tenants-by-user :? :n
select t.name, t.config from tenant t
inner join tenant_user tu on t.id = tu.tenant_id
inner join user u on tu.user_id = u.id
where u.username = :username
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



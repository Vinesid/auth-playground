-- :name select-roles :? :n
select r.name, r.description from role r
inner join tenant t on r.tenant_id = t.id
where t.name = :name
order by r.name

-- :name tenant-id :? :1
select id from tenant
where name = :name

-- :name insert-role :! :n
insert into role (tenant_id, name, description)
values (:tenant-id, :name, :description)

-- :name rename-role :! :n
update role r
inner join tenant t on r.tenant_id = t.id
set r.name = :new-name
where r.name = :name and t.name = :tenant-name

-- :name update-description :! :n
update role r
inner join tenant t on r.tenant_id = t.id
set r.description = :new-name
where r.name = :name and t.name = :tenant-name

-- :name delete-role :! :n
delete r from role r
inner join tenant t on r.tenant_id = t.id
where r.name = :name and t.name = :tenant-name
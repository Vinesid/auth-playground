-- :name select-roles :? :n
select r.name from role r
inner join tenant t on r.tenant_id = t.id
where t.name = :name
order by r.name

-- :name select-role :? :1
select r.name, r.description from role r
inner join tenant t on r.tenant_id = t.id
where t.name = :tenant-name
and r.name = :name

-- :name tenant-id :? :1
select id from tenant
where name = :name

-- :name insert-role :! :n
insert into role (tenant_id, name, description)
values (:tenant-id, :name, :description)

-- :name rename-role :! :n
update role
set name = :new-name
where name = :name and tenant_id = :tenant-id

-- :name update-description :! :n
update role
set description = :description
where name = :name and tenant_id = :tenant-id

-- :name delete-role :! :n
delete from role
where name = :name and tenant_id = :tenant-id

-- :name select-capabilities :? :n
select name, description from capability

-- :name insert-capability :! :n
insert into capability (name, description)
values (:name, :description)

-- :name delete-capability :! :n
delete from capability
where name = :name

-- :name role-id :? :1
select r.id from role r
inner join tenant t on r.tenant_id = t.id
where t.name = :tenant-name
and r.name = :role-name

-- :name capability-id :? :1
select id from capability
where name = :name

-- :name insert-role-capability :! :n
insert into role_capability (role_id, capability_id)
values (:role-id, :capability-id)

-- :name delete-role-capability :! :n
delete from role_capability
where role_id = :role-id and capability_id = :capability-id

-- :name select-role-capabilities :? :n
select ri.name, ri.description from capability ri
inner join role_capability rr on ri.id = rr.capability_id
inner join role r on rr.role_id = r.id
inner join tenant t on r.tenant_id = t.id
where t.name = :tenant-name
and r.name = :role-name
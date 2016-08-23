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

-- :name select-rights :? :n
select name, description from `right`

-- :name insert-right :! :n
insert into `right` (name, description)
values (:name, :description)

-- :name delete-right :! :n
delete from `right`
where name = :name

-- :name role-id :? :1
select r.id from role r
inner join tenant t on r.tenant_id = t.id
where t.name = :tenant-name
and r.name = :role-name

-- :name right-id :? :1
select id from `right`
where name = :name

-- :name insert-role-right :! :n
insert into role_right (role_id, right_id)
values (:role-id, :right-id)

-- :name delete-role-right :! :n
delete from role_right
where role_id = :role-id and right_id = :right-id

-- :name select-role-rights :? :n
select ri.name, ri.description from `right` ri
inner join role_right rr on ri.id = rr.right_id
inner join role r on rr.role_id = r.id
inner join tenant t on r.tenant_id = t.id
where t.name = :tenant-name
and r.name = :role-name
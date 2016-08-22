-- :name insert-user :! :n
insert into user (username, fullname, email)
values (:username, :fullname, :email)

-- :name delete-user :! :n
delete from user
where username = :username

-- :name rename-user :! :n
update user
set username = :new-username
where username = :username

-- :name update-user :! :n
update user
set fullname = :fullname, email = :email
where username = :username

-- :name update-encrypted-password :! :n
update user
set password = :password
where username = :username

-- :name select-user :? :1
select username, fullname, email from user
where username = :username

-- :name select-user-with-password :? :1
select username, fullname, email, password from user
where username = :username
order by username

-- :name select-users :? :n
select username, fullname, email from user
order by username

-- :name select-users-by-tenant :? :n
select username, fullname, email, password from user u
inner join tenant_user tu on u.id = tu.user_id
inner join tenant t on tu.tenant_id = t.id
where t.name = :name
order by username

-- :name user-id :? :1
select id from user
where username = :username

-- :name tenant-id :? :1
select id from tenant
where name = :name

-- :name insert-tenant-user :! :n
insert into tenant_user (tenant_id, user_id)
values (:tenant-id, :user-id)

-- :name delete-tenant-user :! :n
delete from tenant_user
where tenant_id = :tenant-id
and user_id = :user-id

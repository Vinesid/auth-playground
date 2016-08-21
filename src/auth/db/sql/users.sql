-- :name insert-user :! :n
insert into users (username, fullname, email)
values (:username, :fullname, :email)

-- :name delete-user :! :n
delete from users
where username = :username

-- :name update-user :! :n
update users
set username = :username, fullname = :fullname, email = :email
where username = :username

-- :name update-encrypted-password :! :n
update users
set password = :password
where username = :username

-- :name select-user :? :1
select username, fullname, email from users
where username = :username

-- :name select-user-with-password :? :1
select username, fullname, email, password from users
where username = :username

-- :name select-users :? :n
select username, fullname, email from users
order by username
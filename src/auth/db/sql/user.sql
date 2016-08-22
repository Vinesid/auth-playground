-- :name insert-user :! :n
insert into user (username, fullname, email)
values (:username, :fullname, :email)

-- :name delete-user :! :n
delete from user
where username = :username

-- :name update-user :! :n
update user
set username = :username, fullname = :fullname, email = :email
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

-- :name select-users :? :n
select username, fullname, email from user
order by username
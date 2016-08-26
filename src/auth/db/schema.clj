(ns auth.db.schema
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]))

(sql/def-db-fns
  "auth/db/sql/schema.sql"
  {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)})

(defn mysql-create-tables [conn]
  (jdbc/atomic conn
    (mysql-create-users-table conn)
    (mysql-create-tenant-table conn)
    (mysql-create-tenant-user-table conn)
    (mysql-create-role-table conn)
    (mysql-create-capability-table conn)
    (mysql-create-role-right-table conn)
    (mysql-create-tenant-user-role-table conn)))

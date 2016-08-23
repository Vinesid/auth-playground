(ns auth.db.role
  (:require [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [jdbc.core :as jdbc]))

(def ^:private db-fns
  (sql/map-of-db-fns
    "auth/db/sql/role.sql"
    {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)}))

(defn- db-call [fn-name & args]
  (apply (get-in db-fns [fn-name :fn]) args))

(defn get-roles [conn {:keys [name] :as tenant}]
  (db-call :select-roles conn tenant))

(defn add-role [conn {:keys [name] :as tenant} {:keys [name description] :as role}]
  (let [tid (:id (db-call :tenant-id conn tenant))]
    (if tid
      (db-call :insert-role conn (assoc role :tenant-id tid))
      0)))

(defn rename-role [conn {:keys [name] :as tenant} {:keys [name new-name] :as naming}]
  (db-call :rename-role conn (assoc naming :tenant-name (:name tenant))))

(defn set-role-description [conn {:keys [name] :as tenant} {:keys [name description] :as role}]
  (db-call :rename-role conn (assoc role :tenant-name (:name tenant))))

(defn delete-role [conn {:keys [name] :as tenant} {:keys [name] :as role}]
  (db-call :delete-role conn (assoc role :tenant-name (:name tenant))))

(defn get-rights [conn]
  (db-call :select-rights conn))

(defn add-right [conn {:keys [name description] :as right}]
  (db-call :insert-right conn right))

(defn delete-right [conn {:keys [name] :as right}]
  (db-call :delete-right conn right))

(defn get-role-rights [conn tenant role]
  (db-call :select-role-rights conn {:role-name (:name role)
                                     :tenant-name (:name tenant)}))

(defn set-role-right [conn tenant role right]
  (jdbc/atomic
    conn
    (let [role-id (:id (db-call :role-id conn {:tenant-name (:name tenant)
                                               :role-name   (:name role)}))
          right-id (:id (db-call :right-id conn right))]
      (if (and role-id right-id)
        (db-call :insert-role-right conn {:role-id  role-id
                                          :right-id right-id})
        0))))

(defn unset-role-right [conn tenant role right]
  (jdbc/atomic
    conn
    (let [role-id (:id (db-call :role-id conn {:tenant-name (:name tenant)
                                               :role-name   (:name role)}))
          right-id (:id (db-call :right-id conn right))]
      (if (and role-id right-id)
        (db-call :delete-role-right conn {:role-id  role-id
                                          :right-id right-id})
        0))))





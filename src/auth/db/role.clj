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
  (jdbc/atomic
    conn
    (let [tid (:id (db-call :tenant-id conn tenant))]
      (db-call :rename-role conn (assoc naming :tenant-id tid)))))

(defn set-role-description [conn {:keys [name] :as tenant} {:keys [name description] :as role}]
  (jdbc/atomic
    conn
    (let [tid (:id (db-call :tenant-id conn tenant))]
      (db-call :update-description conn (assoc role :tenant-id tid)))))

(defn delete-role [conn {:keys [name] :as tenant} {:keys [name] :as role}]
  (jdbc/atomic
    conn
    (let [tid (:id (db-call :tenant-id conn tenant))]
      (db-call :delete-role conn (assoc role :tenant-id tid)))))

(defn get-capabilities [conn]
  (db-call :select-capabilities conn))

(defn add-capability [conn {:keys [name description] :as capability}]
  (db-call :insert-capability conn capability))

(defn delete-capability [conn {:keys [name] :as capability}]
  (db-call :delete-capability conn capability))

(defn get-role-capabilities [conn tenant role]
  (db-call :select-role-capabilities conn {:role-name (:name role)
                                           :tenant-name (:name tenant)}))

(defn set-role-capability [conn tenant role capability]
  (jdbc/atomic
    conn
    (let [role-id (:id (db-call :role-id conn {:tenant-name (:name tenant)
                                               :role-name   (:name role)}))
          capability-id (:id (db-call :capability-id conn capability))]
      (if (and role-id capability-id)
        (db-call :insert-role-capability conn {:role-id  role-id
                                               :capability-id capability-id})
        0))))

(defn unset-role-capability [conn tenant role capability]
  (jdbc/atomic
    conn
    (let [role-id (:id (db-call :role-id conn {:tenant-name (:name tenant)
                                               :role-name   (:name role)}))
          capability-id (:id (db-call :capability-id conn capability))]
      (if (and role-id capability-id)
        (db-call :delete-role-capability conn {:role-id  role-id
                                               :capability-id capability-id})
        0))))





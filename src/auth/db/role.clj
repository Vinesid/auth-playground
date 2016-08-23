(ns auth.db.role
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [clojure.edn :as edn]))

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
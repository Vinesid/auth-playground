(ns auth.db.tenant
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [clojure.edn :as edn]))

(def ^:private db-fns
  (sql/map-of-db-fns
    "auth/db/sql/tenant.sql"
    {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)}))

(defn- db-call [fn-name & args]
  (apply (get-in db-fns [fn-name :fn]) args))

(defn- ->tenant [rec]
  (update rec :config edn/read-string))

(defn get-tenant [conn {:keys [name]}]
  (some-> (db-call :select-tenant conn {:name name})
          ->tenant))

(defn get-tenants
  ([conn]
   (->> (db-call :select-tenants conn)
        (mapv ->tenant)))
  ([conn {:keys [username] :as user}]
   (->> (db-call :select-tenants-by-user conn user)
        (mapv ->tenant))))

(defn add-tenant [conn {:keys [name config]}]
  (db-call :insert-tenant conn {:name   name
                                :config (prn-str config)}))

(defn rename-tenant [conn {:keys [name new-name] :as naming}]
  (db-call :rename-tenant conn naming))

(defn set-tenant-config [conn {:keys [name config]}]
  (db-call :update-tenant conn {:name   name
                                :config (prn-str config)}))

(defn delete-tenant [conn {:keys [name]}]
  (db-call :delete-tenant conn {:name name}))



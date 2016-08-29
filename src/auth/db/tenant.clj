(ns auth.db.tenant
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [clojure.edn :as edn]
            [auth.db.role :as r])
  (:import [org.h2.jdbc JdbcBlob]))

(def ^:private db-fns
  (sql/map-of-db-fns
    "auth/db/sql/tenant.sql"
    {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)}))

(defn- db-call [fn-name & args]
  (apply (get-in db-fns [fn-name :fn]) args))

(defn blob->edn [b]
  (-> (if (= (type b) JdbcBlob)
        (-> (.getBinaryStream b)
            (slurp :encoding "UTF-8"))
        (String. b "UTF-8"))
      edn/read-string))

(defn ->tenant [conn rec]
  (-> rec
      (update :config blob->edn)
      (assoc :roles (r/get-roles conn {:name (:name rec)}))))

(defn get-tenant [conn {:keys [name]}]
  (jdbc/atomic
    conn
    (some->> (db-call :select-tenant conn {:name name})
             (->tenant conn))))

(defn get-tenant-users [conn {:keys [name] :as tenant}]
  (db-call :select-users-by-tenant conn tenant))

(defn get-tenants [conn]
  (->> (db-call :select-tenants conn)
       (mapv #(->tenant conn %))))

(defn edn->blob [e]
  (.getBytes (prn-str e) "UTF-8"))

(defn add-tenant [conn {:keys [name config]}]
  (db-call :insert-tenant conn {:name   name
                                :config (edn->blob config)}))

(defn rename-tenant [conn {:keys [name new-name] :as naming}]
  (db-call :rename-tenant conn naming))

(defn set-tenant-config [conn {:keys [name config]}]
  (db-call :update-tenant conn {:name   name
                                :config (edn->blob config)}))

(defn delete-tenant [conn {:keys [name]}]
  (db-call :delete-tenant conn {:name name}))



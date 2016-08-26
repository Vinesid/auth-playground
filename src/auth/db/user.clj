(ns auth.db.user
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [buddy.hashers :as hs]
            [buddy.core.hash :as hash]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :refer [minutes from-now]]))

(def ^:private db-fns
  (sql/map-of-db-fns
    "auth/db/sql/user.sql"
    {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)}))

(defn- db-call [fn-name & args]
  (apply (get-in db-fns [fn-name :fn]) args))

(defn get-user [conn {:keys [username]}]
  (db-call :select-user conn {:username username}))

(defn get-users
  ([conn]
   (db-call :select-users conn))
  ([conn {:keys [name] :as tenant}]
   (db-call :select-users-by-tenant conn tenant)))

(defn add-user [conn {:keys [username fullname email] :as user}]
  (db-call :insert-user conn user))

(defn rename-user [conn {:keys [username new-username] :as naming}]
  (db-call :rename-user conn naming))

(defn set-user-info [conn {:keys [username fullname email] :as user}]
  (db-call :update-user conn user))

(defn delete-user [conn {:keys [username]}]
  (db-call :delete-user conn {:username username}))

(defn set-password [conn {:keys [username password]}]
  (db-call :update-encrypted-password conn {:username username
                                            :password (hs/derive password)}))

(defn authenticate [conn {:keys [username password]}]
  (if-let [user (db-call :select-user-with-password conn {:username username})]
    (if (hs/check password
                  (:password user)
                  {:setter #(set-password conn {:username username :password %})})
      [true {:user (dissoc user :password)}]
      [false :invalid-password])
    [false :unknown-user]))

(defn- change-password [conn {:keys [username password new-password]}]
  (jdbc/atomic conn
    (let [[valid? auth-result] (authenticate conn {:username username :password password})]
      (if valid?
        (if (= 1 (set-password conn {:username username :password new-password}))
          [true]
          [false :failed])
        [false auth-result]))))

(def ^:private encryption
  {:alg :dir
   :enc :a128cbc-hs256})

(defn obtain-token [conn secret exp-minutes {:keys [username password] :as login}]
  (let [[valid? result] (authenticate conn login)]
    (if valid?
      (let [claim {:user (:user result)
                   :exp (-> exp-minutes minutes from-now)}
            secret-key (hash/sha256 secret)
            token (jwt/encrypt claim secret-key encryption)]
        [true (assoc result :token token)])
      [false result])))

(defn authenticate-token [secret token]
  (try
    [true (jwt/decrypt token (hash/sha256 secret) encryption)]
    (catch Exception e
      [false (ex-data e)])))

(defn assign-tenant [conn {:keys [username] :as user} {:keys [name] :as tenant}]
  (jdbc/atomic
    conn
    (let [user-id (:id (db-call :user-id conn user))
          tenant-id (:id (db-call :tenant-id conn tenant))]
      (if (and tenant-id user-id)
        (db-call :insert-tenant-user conn {:tenant-id tenant-id
                                           :user-id user-id})
        0))))

(defn unassign-tenant [conn {:keys [username] :as user} {:keys [name] :as tenant}]
  (jdbc/atomic
    conn
    (let [user-id (:id (db-call :user-id conn user))
          tenant-id (:id (db-call :tenant-id conn tenant))]
      (if (and tenant-id user-id)
        (db-call :delete-tenant-user conn {:tenant-id tenant-id
                                           :user-id user-id})
        0))))

(defn- role-params [conn user tenant role]
  (let [tenant-user-id (:id (db-call :tenant-user-id conn {:username (:username user)
                                                           :tenant-name (:name tenant)}))
        role-id (:id (db-call :role-id conn {:role-name (:name role)
                                             :tenant-name (:name tenant)}))]
    (when (and tenant-user-id role-id)
      {:tenant-user-id tenant-user-id
       :role-id        role-id})))

(defn assign-role [conn user tenant role]
  (jdbc/atomic
    conn
    (if-let [params (role-params conn user tenant role)]
      (db-call :insert-tenant-user-role conn params)
      0)))

(defn unassign-role [conn user tenant role]
  (jdbc/atomic
    conn
    (if-let [params (role-params conn user tenant role)]
      (db-call :delete-tenant-user-role conn params)
      0)))


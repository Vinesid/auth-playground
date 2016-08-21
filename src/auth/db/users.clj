(ns auth.db.users
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [buddy.hashers :as hs]))

(def ^:private db-fns
  (sql/map-of-db-fns
    "auth/db/sql/users.sql"
    {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)}))

(defn- db-call [fn-name & args]
  (apply (get-in db-fns [fn-name :fn]) args))

(defn get-user [conn {:keys [username]}]
  (db-call :select-user conn {:username username}))

(defn get-users [conn]
  (db-call :select-users conn))

(defn add-user [conn {:keys [username fullname email] :as user}]
  (db-call :insert-user conn user))

(defn update-user [conn {:keys [username fullname email] :as user}]
  (db-call :update-user conn user))

(defn delete-user [conn {:keys [username]}]
  (db-call :delete-user conn {:username username}))

(defn set-password [conn {:keys [username password]}]
  (db-call :update-encrypted-password conn {:username username
                                            :password (hs/derive password)}))

(defn auth [conn {:keys [username password]}]
  (if-let [user (db-call :select-user-with-password conn {:username username})]
    (if (hs/check password
                  (:password user)
                  {:setter #(set-password conn {:username username :password %})})
      [true (dissoc user :password)]
      [false :invalid-password])
    [false :unknown-user]))

(defn- change-password [conn {:keys [username password new-password]}]
  (jdbc/atomic conn
    (let [[valid? auth-result] (auth conn {:username username :password password})]
      (if valid?
        (if (= 1 (set-password conn {:username username :password new-password}))
          [true]
          [false :failed])
        [false auth-result]))))
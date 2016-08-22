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

(defn get-users [conn]
  (db-call :select-users conn))

(defn add-user [conn {:keys [username fullname email] :as user}]
  (db-call :insert-user conn user))

(defn rename-user [conn {:keys [username new-username] :as user}]
  (db-call :rename-user conn user))

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
      [true {:user (dissoc user :password)}]
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

(def ^:private encryption
  {:alg :dir
   :enc :a128cbc-hs256})

(defn get-token [conn secret exp-minutes {:keys [username password] :as login}]
  (let [[valid? result] (auth conn login)]
    (if valid?
      (let [claim {:user (:user result)
                   :exp (-> exp-minutes minutes from-now)}
            secret-key (hash/sha256 secret)
            token (jwt/encrypt claim secret-key encryption)]
        [true (assoc result :token token)])
      [false result])))

(defn auth-token [secret token]
  (try
    [true (jwt/decrypt token (hash/sha256 secret) encryption)]
    (catch Exception e
      [false (ex-data e)])))


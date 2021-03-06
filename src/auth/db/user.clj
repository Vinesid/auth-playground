(ns auth.db.user
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [buddy.hashers :as hs]
            [buddy.core.hash :as hash]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :refer [seconds from-now]]
            [auth.db.tenant :as t])
  (:import (java.util Date)))

(def ^:private db-fns
  (sql/map-of-db-fns
    "auth/db/sql/user.sql"
    {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)}))

(defn- db-call [fn-name & args]
  (apply (get-in db-fns [fn-name :fn]) args))

(defn- get-tenant-roles [conn {:keys [username] :as user}]
  (reduce (fn [tenants tenant]
            (let [tuid (:id (db-call :tenant-user-id conn {:username username
                                                           :tenant-name (:name tenant)}))
                  roles (->> (db-call :select-tenant-user-roles conn {:tenant-user-id tuid})
                             (map :name)
                             (sort)
                             (into []))]
              (assoc tenants (:name tenant) roles)))
          {}
          (db-call :select-tenants-by-user conn user)))

(defn- set-last-login [conn {:keys [username]} timestamp]
  (db-call :set-user-last-login conn {:username   username
                                      :last-login (Date. ^long timestamp)}))

(defn deactivate-user [conn {:keys [username] :as user}]
  (set-last-login conn user 1001))

(defn activate-user [conn {:keys [username] :as user}]
  (set-last-login conn user (System/currentTimeMillis)))

(defn active-user? [conn {:keys [username validity-period-in-ms] :as user}]
  (if validity-period-in-ms
    (let [last-login (.getTime ^Date (->> {:username username}
                                               (db-call :select-user-last-login conn)
                                               :last_login))]
      (> (+ last-login validity-period-in-ms)
         (System/currentTimeMillis)))
    true))

(defn expired-pw? [conn {:keys [username password-validity-period-in-ms] :as user}]
  (if password-validity-period-in-ms
    (let [last-change (.getTime ^Date (->> {:username username}
                                           (db-call :select-user-last-password-change conn)
                                           :last_password_change))]
      (< (+ last-change password-validity-period-in-ms)
         (System/currentTimeMillis)))
    false))

(defn get-user [conn {:keys [username] :as user}]
  (jdbc/atomic
    conn
    (when-let [user (db-call :select-user conn user)]
      (assoc user :tenant-roles (get-tenant-roles conn user)))))

(defn get-users
  ([conn]
   (get-users conn nil))
  ([conn {:keys [validity-period-in-ms] :as options}]
   (jdbc/atomic
     conn
     (mapv #(merge %
                   {:tenant-roles (get-tenant-roles conn %)}
                   (when validity-period-in-ms
                     {:active? (active-user? conn (merge % options))}))
           (db-call :select-users conn)))))

(defn add-user [conn {:keys [username fullname email] :as user}]
  (db-call :insert-user conn user))

(defn rename-user [conn {:keys [username new-username] :as naming}]
  (db-call :rename-user conn naming))

(defn set-user-info [conn {:keys [username fullname email] :as user}]
  (db-call :update-user conn user))

(defn delete-user [conn {:keys [username]}]
  (db-call :delete-user conn {:username username}))

(defn get-user-passwords [conn {:keys [username] :as user}]
  (db-call :select-user-last-passwords conn user))

(defn- validate-password [password validations]
  (not-empty
    (keep (fn [{:keys [validation prompt]}]
            (when-not (validation password)
              prompt))
          validations)))

(defn set-password [conn {:keys [username password validations]}]
  (if-let [failures (when validations (validate-password password validations))]
    {:status   :failed
     :cause    :infeasible-password
     :failures failures}
    (if (= 1 (db-call :update-encrypted-password conn {:username username
                                                       :password (hs/derive password)}))
      {:status :success}
      {:status :failed})))

(defn disable-password [conn {:keys [username]}]
  (db-call :update-encrypted-password conn {:username username
                                            :password ""}))

(defn- validate-user-login [conn {:keys [tenant username password
                                         validity-period-in-ms password-validity-period-in-ms] :as login}]
  (let [user (db-call :select-user-with-password conn {:username username})]
    (cond
       (not user)  {:status :failed
                    :cause  :unknown-user}

      (not (hs/check password
                     (:password user)
                     {:setter #(set-password conn {:username username :password %})}))
      {:status :failed
       :cause  :invalid-password}

      (not (active-user? conn login))
      {:status :failed
       :cause  :login-expired}

      (expired-pw? conn login)
      {:status :failed
       :cause  :password-expired}

       :else (do (activate-user conn {:username username})
                 {:status :success
                  :user   (-> user
                              (dissoc :password)
                              (assoc :tenant (select-keys (t/get-tenant conn {:name tenant})
                                                   [:name :config])))}))))

(defn authenticate [conn {:keys [tenant username password
                                 validity-period-in-ms password-validity-period-in-ms] :as login}]
  (let [user-auth (validate-user-login conn login)]
    (if (= (:status user-auth) :success)
      (if-let [tuid (:id (db-call :tenant-user-id conn {:username username :tenant-name tenant}))]
        (assoc-in user-auth [:user :capabilities]
                  (->> (db-call :select-user-capabilities conn {:tenant-user-id tuid})
                       (map :name)
                       set))
        {:status :failed
         :cause  :invalid-tenant})
      user-auth)))

(defn change-password [conn {:keys [username password new-password validations]}]
  (jdbc/atomic conn
    (let [auth-result (validate-user-login conn {:username username :password password})]
      (if (= (:status auth-result) :success)
        (set-password conn {:username username :password new-password :validations validations})
        auth-result))))

(def ^:private encryption
  {:alg :dir
   :enc :a128cbc-hs256})

(defn obtain-token [conn secret exp-seconds {:keys [tenant username password] :as login}]
  (let [auth-result (authenticate conn login)]
    (if (= (:status auth-result) :success)
      (let [claim {:user (:user auth-result)
                   :exp (-> exp-seconds seconds from-now)}
            secret-key (hash/sha256 secret)
            token (jwt/encrypt claim secret-key encryption)]
        (assoc auth-result :token token))
      auth-result)))

(defn authenticate-token [secret token]
  (try
    (let [decrypted-token (jwt/decrypt token (hash/sha256 secret) encryption)]
      (if (:user decrypted-token)
        (-> decrypted-token
            (update-in [:user :capabilities] set)
            (assoc :status :success))
        {:status :failed}))
    (catch Exception e
      (assoc (ex-data e) :status :failed))))

(defn obtain-reset-token [conn secret exp-seconds {:keys [username] :as user}]
  (if-let [user (get-user conn user)]
    (let [claim {:reset user
                 :exp   (-> exp-seconds seconds from-now)}
          secret-key (hash/sha256 secret)]
      (if (= 1 (db-call :set-user-reset conn {:username username :reset? true}))
        (jwt/encrypt claim secret-key encryption)
        {:status :failed}))
    {:status :failed
     :cause :unknown-user}))

(defn- user-reset?
  [conn user]
  (:reset (db-call :select-user-reset conn user)))

(defn authenticate-reset-token [conn secret token]
  (try
    (let [decrypted-token (jwt/decrypt token (hash/sha256 secret) encryption)
          user (:reset decrypted-token) ]
      (if (and user (user-reset? conn user))
        (-> decrypted-token
            (assoc :status :success))
        {:status :failed}))
    (catch Exception e
      (assoc (ex-data e) :status :failed))))

(defn reset-password [conn secret {:keys [token new-password validations]}]
  (let [result (authenticate-reset-token conn secret token)]
    (if (= (:status result) :success)
      (if (user-reset? conn (:reset result))
        (set-password conn {:username    (get-in result [:reset :username])
                            :password    new-password
                            :validations validations})
        {:status :failed
         :cause  :not-reset})
      result)))

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


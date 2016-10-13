(ns auth.db-test
  (:require [clojure.test :refer :all]
            [hikari-cp.core :as cp]
            [jdbc.core :as jdbc]
            [auth.db.schema :as schema]
            [auth.db.user :as u]
            [auth.db.tenant :as t]
            [auth.db.role :as r]))

(deftest auth-db-test-crud

  (let [ds (cp/make-datasource {:adapter  "h2"
                                :url      "jdbc:h2:mem:authtest"
                                :username "sa"
                                :password ""})
        conn (jdbc/connection ds)]

    (try

      (testing "Create tables"
        (is (= (schema/h2-create-tables conn)
               0)))

      (testing "User Management"

        (is (= (u/add-user conn {:username "u1" :fullname "u1fn" :email "u1@email.com"})
               1))

        (is (= (u/add-user conn {:username "u2" :fullname "u2fn" :email "u2@email.com"})
               1))

        (is (= (u/get-user conn {:username "u1"})
               {:username "u1" :fullname "u1fn" :email "u1@email.com" :tenant-roles {}}))

        (is (= (u/get-users conn)
               [{:username "u1" :fullname "u1fn" :email "u1@email.com" :tenant-roles {}}
                {:username "u2" :fullname "u2fn" :email "u2@email.com" :tenant-roles {}}]))

        (is (= (u/rename-user conn {:username "u2" :new-username "u2n"})
               1))

        (is (= (u/get-user conn {:username "u2n"})
               {:username "u2n" :fullname "u2fn" :email "u2@email.com" :tenant-roles {}}))

        (is (= (u/set-user-info conn {:username "u2n" :fullname "u2nfn" :email "u2n@email.com"})
               1))

        (is (= (u/get-user conn {:username "u2n"})
               {:username "u2n" :fullname "u2nfn" :email "u2n@email.com" :tenant-roles {}}))

        (is (= (u/delete-user conn {:username "u2n"})
               1))

        (is (not (u/get-user conn {:username "u2n"})))

        (is (= (u/get-users conn)
               [{:username "u1" :fullname "u1fn" :email "u1@email.com" :tenant-roles {}}])))

      (testing "Tenant Management"

        (is (= (t/add-tenant conn {:name "t1" :config {:k1 :v1 :k2 :v2}})
               1))

        (is (= (t/add-tenant conn {:name "t2" :config {:k1 :v1 :k2 :v2}})
               1))

        (is (= (t/get-tenant conn {:name "t1"})
               {:name "t1" :config {:k1 :v1 :k2 :v2} :roles []}))

        (is (= (t/get-tenants conn)
               [{:name "t1" :config {:k1 :v1 :k2 :v2} :roles []}
                {:name "t2" :config {:k1 :v1 :k2 :v2} :roles []}]))

        (is (= (t/rename-tenant conn {:name "t2" :new-name "t2n"})
               1))

        (is (not (t/get-tenant conn {:name "t2"})))

        (is (= (t/get-tenant conn {:name "t2n"})
               {:name "t2n" :config {:k1 :v1 :k2 :v2} :roles []}))

        (is (= (t/set-tenant-config conn {:name "t2n" :config {:nk1 :v1 :nk2 :v2}})
               1))

        (is (= (t/get-tenant conn {:name "t2n"})
               {:name "t2n" :config {:nk1 :v1 :nk2 :v2} :roles []}))

        (is (= (t/delete-tenant conn {:name "t2n"})
               1))

        (is (not (t/get-tenant conn {:name "t2"}))))

      (testing "Tenant User Management"

        (is (= (u/assign-tenant conn {:username "u1"} {:name "t1"})
               1))

        (is (= (t/get-tenant-users conn {:name "t1"})
               [{:username "u1" :fullname "u1fn" :email "u1@email.com"}]))

        (is (= (t/add-tenant conn {:name "t2" :config {:k1 :v1 :k2 :v2}})
               1))

        (is (= (u/assign-tenant conn {:username "u1"} {:name "t2"})
               1))

        (is (= (t/get-tenant-users conn {:name "t2"})
               [{:username "u1" :fullname "u1fn" :email "u1@email.com"}]))

        (is (= (u/unassign-tenant conn {:username "u1"} {:name "t2"})
               1))

        (is (= (t/get-tenant-users conn {:name "t2"})
               []))

        (is (= (t/get-tenant-users conn {:name "t1"})
               [{:username "u1" :fullname "u1fn" :email "u1@email.com"}]))

        (is (= (u/add-user conn {:username "u2" :fullname "u2fn" :email "u2@email.com"})
               1))

        (is (= (u/assign-tenant conn {:username "u2"} {:name "t1"})
               1))

        (is (= (t/get-tenant-users conn {:name "t1"})
               [{:username "u1" :fullname "u1fn" :email "u1@email.com"}
                {:username "u2" :fullname "u2fn" :email "u2@email.com"}])))

      (testing "Role Management"

        (is (= (r/add-role conn {:name "t1"} {:name "r1" :description "t1 role r1"})
               1))

        (is (= (r/add-role conn {:name "t1"} {:name "r2" :description "t1 role r2"})
               1))

        (is (= (r/add-role conn {:name "t2"} {:name "r1" :description "t2 role r1"})
               1))

        (is (= (r/get-role conn {:name "t1"} {:name "r1"})
               {:name "r1"
                :description "t1 role r1"
                :capabilities []}))

        (is (= (r/get-roles conn {:name "t1"})
               [{:name "r1" :description "t1 role r1" :capabilities []}
                {:name "r2" :description "t1 role r2" :capabilities []}]))

        (is (= (r/get-roles conn {:name "t2"})
               [{:name "r1" :description "t2 role r1" :capabilities []}]))

        (is (= (r/rename-role conn {:name "t1"} {:name "r1" :new-name "r1n"})
               1))

        (is (= (r/get-roles conn {:name "t1"})
               [{:name "r1n" :description "t1 role r1" :capabilities []}
                {:name "r2" :description "t1 role r2" :capabilities []}]))

        (is (= (r/set-role-description conn {:name "t1"} {:name "r1n" :description "t1 role r1n"})
               1))

        (is (= (r/get-roles conn {:name "t1"})
               [{:name "r1n" :description "t1 role r1n" :capabilities []}
                {:name "r2" :description "t1 role r2" :capabilities []}]))

        (is (= (r/delete-role conn {:name "t1"} {:name "r1n"})
               1))

        (is (= (r/get-roles conn {:name "t1"})
               [{:name "r2" :description "t1 role r2" :capabilities []}])))

      (testing "Capability Management"

        (is (= (r/add-capability conn {:name "cap1" :description "cap1 desc"})
               1))

        (is (= (r/add-capability conn {:name "cap2" :description "cap2 desc"})
               1))

        (is (= (r/add-capability conn {:name "cap3" :description "cap3 desc"})
               1))

        (is (= (r/get-capabilities conn)
               [{:name "cap1" :description "cap1 desc"}
                {:name "cap2" :description "cap2 desc"}
                {:name "cap3" :description "cap3 desc"}]))

        (is (= (r/delete-capability conn {:name "cap1"})
               1))

        (is (= (r/get-capabilities conn)
               [{:name "cap2" :description "cap2 desc"}
                {:name "cap3" :description "cap3 desc"}])))

      (testing "Role Capability Management"

        (is (= (r/assign-capability conn {:name "t1"} {:name "r2"} {:name "cap2"})
               1))

        (is (= (r/assign-capability conn {:name "t1"} {:name "r2"} {:name "cap3"})
               1))

        (is (= (r/get-role-capabilities conn {:name "t1"} {:name "r2"})
               [{:name "cap2" :description "cap2 desc"}
                {:name "cap3" :description "cap3 desc"}]))

        (is (= (r/unassign-capability conn {:name "t1"} {:name "r2"} {:name "cap3"})
               1))

        (is (= (r/get-role-capabilities conn {:name "t1"} {:name "r2"})
               [{:name "cap2" :description "cap2 desc"}]))

        (is (= (r/get-role conn {:name "t1"} {:name "r2"})
               {:name "r2"
                :description "t1 role r2"
                :capabilities [{:name "cap2" :description "cap2 desc"}]}))

        (is (= (r/get-roles conn {:name "t1"})
               [{:name "r2"
                 :description "t1 role r2"
                 :capabilities [{:name "cap2", :description "cap2 desc"}]}]))

        (is (= (t/get-tenant conn {:name "t1"})
               {:name "t1" :config {:k1 :v1 :k2 :v2}
                :roles [{:name "r2"
                         :description "t1 role r2"
                         :capabilities [{:name "cap2"
                                         :description "cap2 desc"}]}]})))

      (testing "User Role Management"

        (is (= (u/assign-role conn {:username "u1"} {:name "t1"} {:name "r2"})
               1))

        (is (= (u/get-user conn {:username "u1"})
               {:username "u1"
                :fullname "u1fn"
                :email "u1@email.com"
                :tenant-roles {"t1" ["r2"]}}))

        (is (= (r/add-role conn {:name "t1"} {:name "r1" :description "t1 role r1"})
               1))

        (is (= (u/assign-role conn {:username "u1"} {:name "t1"} {:name "r1"})
               1))

        (is (= (u/get-user conn {:username "u1"})
               {:username "u1"
                :fullname "u1fn"
                :email "u1@email.com"
                :tenant-roles {"t1" ["r1" "r2"]}})))

      (catch Exception e
        (throw e))

      (finally
        (.close conn)
        (cp/close-datasource ds)))

    ))

(deftest auth-db-test-scenario

  (let [ds (cp/make-datasource {:adapter  "h2"
                                :url      "jdbc:h2:mem:authtest"
                                :username "sa"
                                :password ""})
        conn (jdbc/connection ds)]

    (try

      (testing "Create tables"
        (is (= (schema/h2-create-tables conn)
               0)))

      (testing "Scenario: Tenants, Roles, Capabilities"

        ;; Capabilities
        (is (= (+ (r/add-capability conn {:name        "edit_article"
                                          :description "Edit existings articles."})
                  (r/add-capability conn {:name        "create_article"
                                          :description "Create new articles."})
                  (r/add-capability conn {:name        "delete_article"
                                          :description "Delete articles."})
                  (r/add-capability conn {:name        "review_article"
                                          :description "Review and comment articles."})
                  (r/add-capability conn {:name        "publish_article"
                                          :description "Publish article."})

                  (r/add-capability conn {:name        "manage_users"
                                          :description "Create, edit, delete users."})
                  (r/add-capability conn {:name        "manage_roles"
                                          :description "Create, edit, delete roles and capabilities."}))
               7))

        ;; Tenant: The New Company
        (is
          (= (+ (t/add-tenant conn {:name "The News Company" :config {:some "config"}})
                ;; Editor
                (r/add-role conn {:name "The News Company"} {:name "editor" :description "Write articles."})
                (r/assign-capability conn {:name "The News Company"} {:name "editor"} {:name "edit_article"})
                (r/assign-capability conn {:name "The News Company"} {:name "editor"} {:name "create_article"})
                ;; Chief Editor
                (r/add-role conn {:name "The News Company"} {:name "chief editor" :description "Control publishing."})
                (r/assign-capability conn {:name "The News Company"} {:name "chief editor"} {:name "publish_article"})
                (r/assign-capability conn {:name "The News Company"} {:name "chief editor"} {:name "delete_article"})
                ;; Reviewer
                (r/add-role conn {:name "The News Company"} {:name "reviewer" :description "Articles QA."})
                (r/assign-capability conn {:name "The News Company"} {:name "reviewer"} {:name "review_article"})
                ;; Manager
                (r/add-role conn {:name "The News Company"} {:name "manager" :description "Manage users"})
                (r/assign-capability conn {:name "The News Company"} {:name "manager"} {:name "manage_users"})
                (r/assign-capability conn {:name "The News Company"} {:name "manager"} {:name "manage_roles"}))
             12))

        ;; Tenant: My Personal Blog
        (is
          (= (+ (t/add-tenant conn {:name "My Personal Blog" :config {:k "v"}})
                (r/add-role conn {:name "My Personal Blog"} {:name "blogger" :description "Blog articles."})
                (r/assign-capability conn {:name "My Personal Blog"} {:name "blogger"} {:name "edit_article"})
                (r/assign-capability conn {:name "My Personal Blog"} {:name "blogger"} {:name "create_article"})
                (r/assign-capability conn {:name "My Personal Blog"} {:name "blogger"} {:name "publish_article"})
                (r/assign-capability conn {:name "My Personal Blog"} {:name "blogger"} {:name "delete_article"})
                (r/assign-capability conn {:name "My Personal Blog"} {:name "blogger"} {:name "review_article"})
                (r/assign-capability conn {:name "My Personal Blog"} {:name "blogger"} {:name "manage_users"})
                (r/assign-capability conn {:name "My Personal Blog"} {:name "blogger"} {:name "manage_roles"}))
             9))

        ;; User John
        (is
          (= (+ (u/add-user conn {:username "jd" :fullname "John Doe" :email "jd@news.com"})
                (u/assign-tenant conn {:username "jd"} {:name "The News Company"})
                (u/assign-role conn {:username "jd"} {:name "The News Company"} {:name "editor"})
                (u/assign-role conn {:username "jd"} {:name "The News Company"} {:name "reviewer"})
                (u/assign-tenant conn {:username "jd"} {:name "My Personal Blog"})
                (u/assign-role conn {:username "jd"} {:name "My Personal Blog"} {:name "blogger"}))
             6))

        (is (= (u/set-password conn {:username "jd" :password "pass"})
               1)))

      (testing "User Authentication"

        ;; Login procedure

        (is (= (u/authenticate conn {:tenant "The News Company" :username "ux" :password "p1"})
               {:status :failed
                :cause  :unknown-user}))

        (is (= (u/authenticate conn {:tenant "The News Compay" :username "jd" :password "px"})
               {:status :failed
                :cause  :invalid-password}))

        (is (= (u/authenticate conn {:tenant "The Bad Company" :username "jd" :password "pass"})
               {:status :failed
                :cause  :invalid-tenant}))

        (is (= (u/authenticate conn {:tenant "The News Company" :username "jd" :password "pass"})
               {:status :success
                :user   {:username     "jd"
                         :fullname     "John Doe"
                         :email        "jd@news.com"
                         :tenant       {:name   "The News Company"
                                        :config {:some "config"}}
                         :capabilities #{"create_article" "edit_article" "review_article"}}}))

        (is (= (u/authenticate conn {:tenant "My Personal Blog" :username "jd" :password "pass"})
               {:status :success
                :user {:username "jd"
                       :fullname "John Doe"
                       :email "jd@news.com"
                       :tenant {:name   "My Personal Blog"
                                :config {:k "v"}}
                       :capabilities #{"create_article" "publish_article" "edit_article" "manage_users" "review_article" "manage_roles" "delete_article"}}}))

        (is (= (u/change-password conn {:username "jd" :password "pass" :new-password "pn"})
               {:status :success}))

        (is (= (u/authenticate conn {:tenant "The News Company" :username "jd" :password "pn"})
               {:status :success
                :user   {:username     "jd"
                         :fullname     "John Doe"
                         :email        "jd@news.com"
                         :tenant       {:name   "The News Company"
                                        :config {:some "config"}}
                         :capabilities #{"create_article" "edit_article" "review_article"}}}))

        ;; Login-expiration

        (let [username "user"
              password "password"
              user {:username username :fullname "User Name" :email "user@tenant.com"}
              tenant-name "Tenant 1"
              tenant {:name tenant-name :config {:k "v"}}
              authentication-params {:tenant tenant-name :username username :password password :validity-period-in-ms 500000}
              valid-user (assoc user
                           :tenant tenant
                           :capabilities #{})]

          (is (= (+ (u/add-user conn user)
                    (t/add-tenant conn tenant)
                    (u/assign-tenant conn user tenant)
                    (u/set-password conn (assoc user :password password)))
                 4))

          (Thread/sleep 2)

          (is (= (u/authenticate conn {:tenant tenant-name :username username :password password :validity-period-in-ms 1})
                 {:status :failed
                  :cause  :login-expired}))

          (is (= (u/authenticate conn authentication-params)
                 {:status :success
                  :user   valid-user}))

          (is (= (u/deactivate-user conn user)
                 1))

          (is (not (u/active-user? conn authentication-params)))

          (is (= (u/activate-user conn user)
                 1))

          (is (u/active-user? conn authentication-params))

          (is (= (u/delete-user conn user)
                 1))

          (is (= (u/get-user conn user)
                 nil)))

        ;; Tokens

        (let [expire-seconds 1
              secret "server-hmac-secret"
              result (u/obtain-token conn secret expire-seconds {:tenant "The News Company" :username "jd" :password "pn"})]

          (is (= (dissoc result :token)
                 {:status :success
                  :user   {:username     "jd"
                           :fullname     "John Doe"
                           :email        "jd@news.com"
                           :tenant       {:name   "The News Company"
                                          :config {:some "config"}}
                           :capabilities #{"create_article" "edit_article" "review_article"}}}))

          (let [result (u/authenticate-token secret (:token result))]

            (is (= (dissoc result :exp)
                   {:status :success
                    :user   {:username     "jd"
                             :fullname     "John Doe"
                             :email        "jd@news.com"
                             :tenant       {:name   "The News Company"
                                            :config {:some "config"}}
                             :capabilities #{"create_article" "edit_article" "review_article"}}})))

          (let [result (do
                         (Thread/sleep (* expire-seconds 1000 1.2))
                         (u/authenticate-token secret (:token result)))]

            (is (= result
                   {:status :failed
                    :type   :validation
                    :cause  :exp}))))

        (let [expire-seconds 2
              reset-token (u/obtain-reset-token conn "secret" expire-seconds {:username "jd"})
              reset (u/reset-password conn "secret" {:new-password "np" :token reset-token})
              snd-reset (u/reset-password conn "secret" {:new-password "np" :token reset-token})
              exp-reset (do
                          (Thread/sleep (* expire-seconds 1000 1.2))
                          (u/reset-password conn "secret" {:new-password "np" :token reset-token}))]
          (is (= (:status reset) :success))
          (is (= snd-reset {:status :failed}))
          (is (= exp-reset {:status :failed
                            :type   :validation
                            :cause  :exp}))))

      (catch Exception e
        (throw e))

      (finally
        (.close conn)
        (cp/close-datasource ds)))

    ))



(defproject auth-playground "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [funcool/clojure.jdbc "0.9.0"]
                 [hikari-cp "1.7.3"]
                 [mysql/mysql-connector-java "5.1.39"]
                 [com.h2database/h2 "1.4.192"]
                 [com.layerware/hugsql-core "0.4.7"]
                 [com.layerware/hugsql-adapter-clojure-jdbc "0.4.7"]
                 [buddy/buddy-hashers "0.14.0"]
                 [buddy/buddy-sign "1.1.0"]
                 [clj-time "0.12.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

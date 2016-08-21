(ns auth.db.schema
  (:require [jdbc.core :as jdbc]
            [hugsql.core :as sql]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]))

(sql/def-db-fns
  "auth/db/sql/schema.sql"
  {:adapter (cj-adapter/hugsql-adapter-clojure-jdbc)})

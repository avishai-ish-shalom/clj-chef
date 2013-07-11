(ns clj-chef.api-client
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as logging]
            [clj-chef.rest :refer [defrest chef-rest]])
  (:import [org.bouncycastle.openssl PEMReader])
)

; maybe move this to utils?
(defn load-key
  "Loads an RSA private key from file, will return RSAPrivateKey"
  [keyname] (-> keyname
              io/reader
              PEMReader.
              .readObject
              .getPrivate))


(def ^:dynamic *chef-config* nil)

(defmacro with-config [config & forms]
  `(binding [*chef-config* ~config]
     ~@forms))

; defrest is in clj-chef.rest namespace
(defrest :node "/nodes/:id" [:get :delete :put])
(defrest :client "/clients/:id" [:get :delete :put])
(defrest :cookbook "/cookbooks/:id" [:get :delete :put])
(defrest :role "/roles/:id" [:get :delete :put])
(defrest :environment "/environments/:id" [:get :delete :put])
(defrest :environment-node "/environments/:id/nodes" [:get])
(defrest :data-bag "/data/:id" [:get :delete :put])
(defrest :data-bag-item "/data/:bag/:item" [:get :delete :put])
(defrest :cookbook-version "/cookbooks/:name/:version" [:get])

(defn search
  [index-name query]
  (-> (chef-rest
        *chef-config*
        :get
        (str "/search/" (name index-name))
        {:query-params {"q" query}})
    :body
    json/parse-string
    (get "rows")
  ))

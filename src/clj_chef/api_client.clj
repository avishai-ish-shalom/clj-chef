; Copyright (c) Avishai Ish-Shalom. All rights reserved.
;
; This file is part of the clj-chef library.
; clj-chef is free software: you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;
; Authors: Avishai Ish-Shalom

(ns clj-chef.api-client
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as logging]
            [clj-chef.rest :refer [defrest chef-rest *chef-config*]])
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

(defmacro with-config [config & forms]
  `(binding [*chef-config* ~config]
     ~@forms))

; defrest is in clj-chef.rest namespace
(defrest :node "/nodes/:id" [:get :delete :put])
(defrest :client "/clients/:id" [:get :delete :put])
(defrest :role "/roles/:id" [:get :delete :put])
(defrest :environment "/environments/:id" [:get :delete :put])
(defrest :environment-node "/environments/:id/nodes" [:get])
(defrest :environment-recipe "/environments/:id/recipes" [:get])
(defrest :environment-cookbook "/environments/:environment/cookbooks/:id" [:get])
(defrest :data-bag "/data/:id" [:get :delete :put])
(defrest :data-bag-item "/data/:bag/:item" [:get :delete :put])
(defrest :cookbook "/cookbooks/:id" [:get])
(defrest :cookbook-version "/cookbooks/:name/:version" [:get :delete])

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

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
; GNU Lesser General Public License for more details.
;
; You should have received a copy of the GNU Lesser General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;
; Authors: Avishai Ish-Shalom

(ns clj-chef.rest
	(:require [clj-chef.utils :refer :all]
			[clj-http.client :as http-client]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clj-time.format :as time-format]
            [clj-time.core :as t]
            [clojure.tools.logging :as logging]))

(def chef-api-version "11.4.4")
(def ^:private iso8601 (time-format/formatters :date-time-no-ms))
(def ^:private default-http-opts {:accept :json :content-type :json})
(def ^:dynamic *chef-config* {})

; auth-* functions are in clj-chef.utils namespace
(defn chef-rest
  "Perform an authenticated REST call to a Chef server"
  ([config method resource] (chef-rest config method resource nil))
  ([{:keys [client-name client-key server-url auth-version organization] :as config :or {auth-version "1.0"}}
    method
    resource
    opts] ; end of arguments list
  (let [ resource-path (if organization (string/join "/" ["organizations" organization (str-ltrim-char resource \/)]) resource)
         url-obj (java.net.URL. (java.net.URL. server-url) resource-path)
         url (.toString url-obj)
         timestamp (time-format/unparse iso8601 (t/now))
         headers (auth-headers method (.getPath url-obj) (get opts :body) timestamp client-name client-key auth-version chef-api-version)
         opts (merge default-http-opts {:headers headers } opts)
         ]
    ; do the http call and return result
    (logging/spy opts)
    (logging/spy url)
    (http-client/request (merge opts {:url url :method method}))
    )))

(defn- parse-object [obj-type response]
  (json/parse-string (:body response)))

(defn- split-path-spec [path-spec]
  (let [spec
        (for [part (string/split path-spec #"/") :when (not (empty? part))]
          (if (.startsWith part ":")
            (keyword (string/replace-first part ":" ""))
            part
        ))]
    [(cons "" spec) (map (comp symbol name) (filter keyword? spec))]))


(def ^:private vec-butlast (comp vec butlast))

(defn zip-args [path-parts args]
	(if (empty? path-parts) []
		(let [	[p & more-parts] path-parts
				[a & more-args] args]
			(if (keyword? p)
				(cons a (zip-args more-parts more-args))
				(cons p (zip-args more-parts args))))))

(defn chef-rest-call
	([method path-spec path-args] (chef-rest-call method path-spec path-args nil))
	([method path-spec path-args data]
		(let [resource-path (string/join "/" (zip-args path-spec path-args))
			  opts (when data {:body data})
			]
			(json/parse-string
				(:body (chef-rest *chef-config* method resource-path opts)
	)))))

(defn make-obj-list [model path-components arguments parser]
	(let [fname ((comp symbol str) (name model) "-list")]
        `(defn ~fname ~arguments
        	(~parser (chef-rest-call :get ~path-components ~arguments)))))

;  the book says generating multiple defs from one macro is bad.
;  let's do it anyway (until i figure out how to do it the proper way and still have nice syntax)
;  TODO: improve this horrible macro
;  TODO: define multiple function versions with different arity (support optional opts argument)
(defmacro defrest
  "Define rest functions for a model. Functions will be generated for :get, :put, :delet HTTP methods and will be named `model`-show,create,delete.
  In addition, if the path-spec end with :id (see below) an additional function `model`-list will be generated. E.g. if the model is \"node\" and path spec is \"/node/:id\" the following functions will be generated:
  (node-list []), (node-create [id obj]), (node-show [id]), (node-delete [id])
  Function arguments for generated methods will be based on the path-spec: keyword like components will become function arguments and the path-spec will be a template for the URI of the model such that keyword components will be subtituted by function arguments. E.g. path-spec of \"/environments/:environment/cookbooks/:id\" for the env-cookbook model will generate a show method:
  (env-cookbook-show [environment id])

  This macro accepts additional options: :list-parser and :obj-parser
  :list-parser - parse the result of object listing REST call
  :obj-parser - parse the result of object retrieval
  These accept function which will be called on responses as appropriate.
  "
  [model path-spec methods & opts]
  (assert (even? (count opts)))
  (let [methods-func-names {:get "show" :put "create" :delete "delete"}
        [path-components arguments] (map vec (split-path-spec path-spec))
        opts (apply hash-map opts)
        coerce-fn (get opts :obj-parser identity)
       ]
    (concat `(do)
      ; define *-list functions
      (when (= (last path-components) :id)
      	(let [path-components (vec-butlast path-components)
              args (vec-butlast arguments)]
        	(list (make-obj-list model path-components args (get opts :list-parser first)))))

      (for [method methods :let [fname ((comp symbol str) (name model) "-" (method methods-func-names))]]
        (case method
          :put (let [obj-sym (gensym 'obj) args (conj arguments obj-sym)]
          	`(defn ~fname ~args
                   (~coerce-fn (chef-rest-call ~method ~path-components ~arguments
                                    (json/generate-string ~obj-sym)))))
          ; default
          `(defn ~fname ~arguments
            (~coerce-fn (chef-rest-call ~method ~path-components ~arguments)))
    )))))

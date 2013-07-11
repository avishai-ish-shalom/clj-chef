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
         headers (auth-headers method (.getPath url-obj) (get :body opts) timestamp client-name client-key auth-version chef-api-version)
         opts (merge default-http-opts {:headers headers } opts)
         ]
    ; do the http call and return result
    (logging/spy opts)
    (logging/spy url)
    (http-client/request (merge opts {:url url :method method}))
    )))

(defn- parse-object [obj-type response]
  (json/parse-string (:body response)))

(defn split-path-spec [path-spec]
  (let [spec
        (for [part (string/split path-spec #"/") :when (not (empty? part))]
          (if (.startsWith part ":")
            (symbol (string/replace-first part ":" ""))
            part
        ))]
    [spec (filter symbol? spec)]))


(def ^:private vec-butlast (comp vec butlast))

;  the book says generating multiple defs from one macro is bad.
;  let's do it anyway (until i figure out how to do it the proper way and still have nice syntax)
;  TODO: improve this horrible macro
;  TODO: define multiple function versions with different arity (support optional opts argument)
(defmacro defrest [model path-spec methods]
  (let [methods-func-names {:get "show" :put "create" :delete "delete"}
        [path-components arguments] (map vec (split-path-spec path-spec))
       ]
    (concat `(do)
      ; define *-list functions
      (when (= (last path-components) 'id)
        (list `(defn ~((comp symbol str) (name model) "-list")
                 ~(vec-butlast arguments)
          ; needed to use ~'*chef-config* instead of *chef-config* to make the symbol namespace relative
          (json/parse-string (:body (chef-rest ~'*chef-config* :get (string/join "/" ~(vec-butlast path-components))))))))
      (for [method methods :let [fname ((comp symbol str) (name model) "-" (method methods-func-names))]]
        (case method
          :put (let [obj-sym (gensym 'obj)] `(defn ~fname ~(conj arguments obj-sym)
                  (json/parse-string (:body (chef-rest ~'*chef-config* ~method
                                             (string/join "/" ~path-components)
                                             {:body (json/generate-string ~obj-sym)})))))
          ; default
          `(defn ~fname ~arguments
            (json/parse-string
              (:body (chef-rest ~'*chef-config* ~method (string/join "/" ~path-components)))))))
    )))

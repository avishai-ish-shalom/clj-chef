(ns clj-chef.api-client
  (:require [clj-http.client :as http-client]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clj-time.format :as time-format]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [clojure.tools.logging :as logging])
  (:import [java.security MessageDigest KeyFactory Security]
           [javax.crypto Cipher]
           [java.security.interfaces RSAPrivateKey]
           [org.bouncycastle.jce.provider BouncyCastleProvider]
           [org.bouncycastle.openssl PEMReader]
           [org.apache.commons.codec.binary Base64]))

(def chef-api-version "11.4.4")
(def ^:private iso8601 (time-format/formatters :date-time-no-ms))
(def ^:private default-http-opts {:accept :json :content-type :json})
(def bytes-type (Class/forName "[B"))
(Security/addProvider (BouncyCastleProvider.))

(defn- base64-decode
  "Decode a base64 encoded string"
  [s] (String. (.decode (Base64.) s)))

(defmulti ^{:doc "Encode a string to base64 string" :private true}
  base64-encode type)

(defmethod base64-encode java.lang.String
  [s] (base64-encode (.getBytes s)))

(defmethod base64-encode bytes-type
  [s] (String. (.encode (Base64.) s)))

(defn- is-base64? [s] (Base64/isBase64 s))

(defn- digest-sha1 [msg]
  (base64-encode
    (let [digest-obj (MessageDigest/getInstance "sha1")]
      (.digest digest-obj (.getBytes msg)))))

;(defn- rsa-sign [key msg]
;  (->> msg
;    (crypto/sign key)
;    base64-encode))

(defn- rsa-sign [private-key data]
  (base64-encode
    (let [cipher (Cipher/getInstance "RSA")]
      (.init cipher Cipher/ENCRYPT_MODE private-key)
      (.doFinal cipher (.getBytes data)))))

(defn- canonicalize-user-name [username proto-version]
  (case proto-version
    "1.1" (digest-sha1 username)
    "1.0" username
    username))

(defn str-ltrim-char [s c]
  (if (.startsWith s (str c)) (string/replace-first s c nil)
    s))

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
         hashed-path (digest-sha1 (.getPath url-obj))
         hashed-body (digest-sha1 (if-let [data (get opts :body)] data ""))
         canonical-request (string/join "\n"
                             (map (partial string/join ":")
                               [
                                 ["Method" (string/upper-case (name method))]
                                 ["Hashed Path" hashed-path]
                                 ["X-Ops-Content-Hash" hashed-body]
                                 ["X-Ops-Timestamp" timestamp]
                                 ["X-Ops-UserId" (canonicalize-user-name client-name auth-version)]
                                 ]))
         signature (rsa-sign client-key canonical-request)
         signature-parts (map (partial apply str) (partition 64 64 nil signature))
         signature-headers (reduce conj {} (map-indexed (fn [i part] [(str "X-Ops-Authorization-" (inc i)) part]) signature-parts))
         headers (merge
                   {
                     "X-Ops-Timestamp" timestamp
                     "X-Ops-Userid" client-name
                     "X-Chef-Version" chef-api-version
                     "X-Ops-Content-Hash" hashed-body
                     "X-Ops-Sign" (str "algorithm=sha1;" "version=" auth-version ";")
                   } signature-headers)
         opts (merge default-http-opts {:headers headers } opts)
         ]
    ; do the http call and return result
    (logging/spy canonical-request)
    (logging/spy signature)
    (logging/spy opts)
    (logging/spy url)
    (http-client/request (merge opts {:url url :method method}))
    )))

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
          (json/parse-string (:body (chef-rest *chef-config* :get (string/join "/" ~(vec-butlast path-components))))))))
      (for [method methods :let [fname ((comp symbol str) (name model) "-" (method methods-func-names))]]
        (case method
          :put (let [obj-sym (gensym 'obj)] `(defn ~fname ~(conj arguments obj-sym)
                  (json/parse-string (:body (chef-rest *chef-config* ~method
                                             (string/join "/" ~path-components)
                                             {:body (json/generate-string ~obj-sym)})))))
          ; default
          `(defn ~fname ~arguments
            (json/parse-string
              (:body (chef-rest *chef-config* ~method (string/join "/" ~path-components)))))))
    )))

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

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

(ns clj-chef.utils
  (:require [clj-http.client :as http-client]
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

(def ^:private bytes-type (Class/forName "[B"))
(Security/addProvider (BouncyCastleProvider.))

(defn str-ltrim-char [s c]
  (if (.startsWith s (str c)) (string/replace-first s c nil)
    s))

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

(defn digest-sha1 [msg]
  (base64-encode
    (let [digest-obj (MessageDigest/getInstance "sha1")]
      (.digest digest-obj (.getBytes msg)))))

(defn rsa-sign [private-key data]
  (base64-encode
    (let [cipher (Cipher/getInstance "RSA")]
      (.init cipher Cipher/ENCRYPT_MODE private-key)
      (.doFinal cipher (.getBytes data)))))

(defn- auth-canonical-user-name [username proto-version]
  (case proto-version
    "1.1" (digest-sha1 username)
    "1.0" username
    username))

(defn- auth-canonical-request [method url-path hashed-body timestamp client-name auth-version]
  (let [upcase-method (string/upper-case (name method))]
    (string/join "\n"
      (map (partial string/join ":")
       [
         ["Method" upcase-method]
         ["Hashed Path" (digest-sha1 url-path)]
         ["X-Ops-Content-Hash" hashed-body]
         ["X-Ops-Timestamp" timestamp]
         ["X-Ops-UserId" (auth-canonical-user-name client-name auth-version)]
       ]))))


(defn auth-headers [method url-path body timestamp client-name client-key auth-version chef-api-version]
  (let [body (if body body "")
        hashed-body (digest-sha1 body)
        canonical-request (auth-canonical-request method url-path hashed-body timestamp client-name auth-version)
        signature (rsa-sign client-key canonical-request)
        signature-parts (map (partial apply str) (partition 64 64 nil signature))
        signature-headers (reduce conj {}
          (map-indexed (fn [i part] [(str "X-Ops-Authorization-" (inc i)) part]) signature-parts))
        ]
    (merge
         {
           "X-Ops-Timestamp" timestamp
           "X-Ops-Userid" client-name
           "X-Chef-Version" chef-api-version
           "X-Ops-Content-Hash" hashed-body
           "X-Ops-Sign" (str "algorithm=sha1;" "version=" auth-version ";")
          }
          signature-headers)))

(ns clj-chef.t-utils
  (:use midje.sweet
        [midje.util :only [testable-privates]]
        [clj-chef.api-client]
        ))

(defn- is-type [t] (fn [result] (= (type result) t)))

(testable-privates clj-chef.utils auth-canonical-user-name base64-encode base64-decode)

(facts "about `canonical-user-name`"
  (fact "should return username for protocol 1.0"
    (auth-canonical-user-name "test-user" "1.0") => "test-user")
  (fact "should return base64 encoded hash of uername for protocol 1.1"
    (auth-canonical-user-name "test-user" "1.1") => "xWSG+LY49j4EJR0MirC0+/7o4Gs=")
)

(facts "about `base64-encode`"
  (fact "should base64 encode sample string"
    (base64-encode "Sample String") => "U2FtcGxlIFN0cmluZw==")
  (fact "should accept both string and byte array"
    (base64-encode "test") => (is-type String)
    (base64-encode (.getBytes "test")) => (is-type String)))

(fact "`base64-decode` should decode base64 sample string"
  (base64-decode "U2FtcGxlIFN0cmluZw==") => "Sample String")
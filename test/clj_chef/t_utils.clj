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

(ns clj-chef.t-utils
  (:use midje.sweet
        [midje.util :only [testable-privates]]
        [clj-chef.utils]
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

(facts "about `str-ltrim-char`"
  (fact "should remove all occurenecs of c at the beggining of s"
    (str-ltrim-char "/////test1" \/) => "test1"
    (str-ltrim-char "/test2" \/) => "test2")
  (fact "shouldn't remove occurenecs in the middle of a string"
    (str-ltrim-char "test//test/test" \/) => "test//test/test")
  (fact "throws exception if c isn't a java.lang.Character"
    (str-ltrim-char "test/test" "te") => (throws java.lang.AssertionError))
  )
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

(defproject clj-chef "0.1.0-SNAPSHOT"
  :description "Chef API client library"
  :license "LGPL v3"
  :url "https://github.com/avishai-ish-shalom/clj-chef"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.5.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-http "2.1.0"]
                 [clj-time "0.11.0"]
                 [org.bouncycastle/bcprov-jdk16 "1.46"]
                 ]
  :profiles {:dev {
                   :dependencies [[midje "1.8.3"]
                                  [log4j/log4j "1.2.17"]
                                  ]
                   :plugins [[lein-midje "3.2"]]
                   }})

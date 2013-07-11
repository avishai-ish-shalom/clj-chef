(defproject clj-chef "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.2.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-http "0.7.2"]
                 [clj-time "0.5.1"]
                 [org.bouncycastle/bcprov-jdk16 "1.46"]
                 [log4j/log4j "1.2.17"]
                 ]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]}})
  

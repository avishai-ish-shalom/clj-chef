(ns clj-chef.core
  (:require [clojure.edn :as edn]
            [clj-chef.api-client :refer [load-key]]
            ))

(defn read-config [filename]
  (let [{client-key :client-key :as config}
    (edn/read-string (slurp filename))]
    (assoc config :client-key (load-key client-key))))
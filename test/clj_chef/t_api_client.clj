(ns clj-chef.t-api-client
  (:require [clj-chef.rest :refer [chef-rest]]
            [cheshire.core :as json])
  (:use midje.sweet
        [midje.util :only [testable-privates]]
        [clj-chef.api-client]
        ))

(def node-id "test-node")
(def role-id "test-role")
(def cookbook-id "test-cookbook")
(def environment-id "test-env")

(background (around :facts (with-config ...config... ?form)))

; Description of the library interface. we should never ever break this.
; I chose not to generate these tests (using macro or doseq) because they serve as spec docs
; Right now, negative tests are missing and the exceptions are not speced. this should be fixed later on
(facts "`defrest` generated functions for node"
  (let [node-path (str "/nodes/" node-id)]
    (fact (node-list) => (just {})
      (provided (chef-rest ...config... :get "/nodes") => {:body "{}"}))
    (fact (node-show node-id) => {"id" node-id}
      (provided 
        (chef-rest ...config... :get node-path) => {:body (json/generate-string {"id" node-id})}))
     (fact (node-delete node-id) => {"id" node-id}
      (provided 
        (chef-rest ...config... :delete node-path) => {:body (json/generate-string {"id" node-id})}))
     (fact "node-create"
      (node-create node-id ...node-data...) => {"id" node-id}
      (provided
        (json/generate-string ...node-data...) => ...node-text-data...
        (chef-rest ...config... :put node-path {:body ...node-text-data...}) => {:body (json/generate-string {"id" node-id})}))
     ))

(facts "`defrest` generated functions for role"
  (let [role-path (str "/roles/" role-id)]
    (fact (role-list) => (just {})
      (provided (chef-rest ...config... :get "/roles") => {:body "{}"}))
    (fact (role-show role-id) => {"id" role-id}
      (provided 
        (chef-rest ...config... :get role-path) => {:body (json/generate-string {"id" role-id})}))
     (fact (role-delete role-id) => {"id" role-id}
      (provided 
        (chef-rest ...config... :delete role-path) => {:body (json/generate-string {"id" role-id})}))
     (fact "role-create"
      (role-create role-id ...role-data...) => {"id" role-id}
      (provided
        (json/generate-string ...role-data...) => ...role-text-data...
        (chef-rest ...config... :put role-path {:body ...role-text-data...}) => {:body (json/generate-string {"id" role-id})}))
     ))

(facts "`defrest` generated functions for cookbook"
  (let [cookbook-path (str "/cookbooks/" cookbook-id)]
    (fact (cookbook-list) => (just {})
      (provided (chef-rest ...config... :get "/cookbooks") => {:body "{}"}))
    (fact (cookbook-show cookbook-id) => {"id" cookbook-id}
      (provided 
        (chef-rest ...config... :get cookbook-path) => {:body (json/generate-string {"id" cookbook-id})}))
     (fact (cookbook-delete cookbook-id) => {"id" cookbook-id}
      (provided 
        (chef-rest ...config... :delete cookbook-path) => {:body (json/generate-string {"id" cookbook-id})}))
     (fact "cookbook-create"
      (cookbook-create cookbook-id ...cookbook-data...) => {"id" cookbook-id}
      (provided
        (json/generate-string ...cookbook-data...) => ...cookbook-text-data...
        (chef-rest ...config... :put cookbook-path {:body ...cookbook-text-data...}) => {:body (json/generate-string {"id" cookbook-id})}))
     ))


(facts "`defrest` generated functions for environment"
  (let [environment-path (str "/environments/" environment-id)]
    (fact (environment-list) => (just {})
      (provided (chef-rest ...config... :get "/environments") => {:body "{}"}))
    (fact (environment-show environment-id) => {"id" environment-id}
      (provided 
        (chef-rest ...config... :get environment-path) => {:body (json/generate-string {"id" environment-id})}))
     (fact (environment-delete environment-id) => {"id" environment-id}
      (provided 
        (chef-rest ...config... :delete environment-path) => {:body (json/generate-string {"id" environment-id})}))
     (fact "environment-create"
      (environment-create environment-id ...environment-data...) => {"id" environment-id}
      (provided
        (json/generate-string ...environment-data...) => ...environment-text-data...
        (chef-rest ...config... :put environment-path {:body ...environment-text-data...}) => {:body (json/generate-string {"id" environment-id})}))
     ))

(fact "`defrest` generated function for search"
  (let [search-path "/search/db"]
    (search :db ...query...) => []
    (provided
      (chef-rest ...config... :get search-path {:query-params {"q" ...query...}}) => {:body (json/generate-string {"rows" []})})
  ))
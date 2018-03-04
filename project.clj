(defproject re-play "0.1.0-SNAPSHOT"
  :description "Debug re-frame apps by replaying events"
  :url "https://github.com/DaveWM/re-play"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.908" :scope "provided"]
                 [org.clojure/core.async  "0.3.443"]
                 [re-frame "0.10.1"]]
  :plugins [[lein-codox "0.10.3"]]
  :codox {:language :clojurescript})

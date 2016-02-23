(defproject kpar "0.1.0-SNAPSHOT"
  :description "Parallel combinators for Clojure"
  :url "https://github.com/kikofernandez/ParT"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.nrepl "0.2.12"]]
  :profiles {:dev {:plugins [[cider/cider-nrepl "0.11.0-SNAPSHOT"]]}}
  )

(defproject org.clojars.tooooolong/clojure-cobertura-coverage "0.1.0"
  :description "Cloverage custom reporter that generates Cobertura XML coverage reports"
  :url "https://github.com/tooooolong/clojure-cobertura-coverage"
  :license {:name "Eclipse Public License 2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.xml "0.2.0-alpha10"]
                 [cloverage "1.2.4"]]
  :profiles {:dev {:source-paths ["dev"]
                   :plugins [[lein-cloverage "1.2.4"]]}}
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                     :username      :env/clojars_username
                                     :password      :env/clojars_password
                                     :sign-releases false}]]
  :cloverage {:ns-regex [#"^example\..*"]
              :output   "target/coverage"})

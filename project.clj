(defproject tooooolong/clojure-cobertura-coverage "0.1.0"
  :description "Cloverage custom reporter that generates Cobertura XML coverage reports"
  :url "https://github.com/tooooolong/clojure-cobertura-coverage"
  :license {:name "Eclipse Public License 2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cloverage "1.2.4"]]
  :profiles {:dev {:plugins [[lein-cloverage "1.2.4"]]}}
  ;; Run coverage only on the example namespace to demonstrate the reporter.
  ;; Remove or adjust :ns-regex when using this reporter in your own project.
  :cloverage {:ns-regex ["^example\\..*"]
              :output   "target/coverage"})

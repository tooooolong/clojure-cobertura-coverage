(ns cloverage.coverage.cobertura-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [clojure.xml :as xml]
            [cloverage.coverage.cobertura :as cobertura]))

(def ^:private sample-forms
  [{:file "exchange/api/restful/accounts.clj"
    :lib 'exchange.api.restful.accounts
    :line 42
    :text "(accounts)"
    :tracked true
    :covered true
    :hits 1}])

(defn- report-xml
  [src-ns-path]
  (let [output-dir (.toFile (java.nio.file.Files/createTempDirectory "cobertura-report"
                                                                    (make-array java.nio.file.attribute.FileAttribute 0)))]
    (cobertura/report {:output (.getAbsolutePath output-dir)
                       :forms sample-forms
                       :args {:src-ns-path src-ns-path}})
    (with-open [in (io/input-stream (io/file output-dir "cobertura.xml"))]
      (xml/parse in))))

(defn- xml-elements
  [doc tag]
  (filter #(= tag (:tag %))
          (tree-seq map? :content doc)))

(defn- xml-text
  [element]
  (apply str (filter string? (:content element))))

(deftest report-uses-src-ns-path-for-sources
  (let [doc (report-xml ["src/main/clojure"])
        sources (map xml-text (xml-elements doc :source))
        class-node (first (xml-elements doc :class))]
    (is (= ["src/main/clojure"] sources))
    (is (= "exchange/api/restful/accounts.clj"
           (get-in class-node [:attrs :filename])))))

(deftest report-supports-multiple-source-roots
  (let [doc (report-xml ["src/main/clojure" "src/shared/clojure"])
        sources (map xml-text (xml-elements doc :source))]
    (is (= ["src/main/clojure" "src/shared/clojure"] sources))))

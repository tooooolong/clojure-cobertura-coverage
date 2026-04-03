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

(def ^:private partial-line-forms
  [{:file "exchange/api/restful/accounts.clj"
    :lib 'exchange.api.restful.accounts
    :line 10
    :text "(first-branch)"
    :tracked true
    :covered true
    :hits 1}
   {:file "exchange/api/restful/accounts.clj"
    :lib 'exchange.api.restful.accounts
    :line 10
    :text "(second-branch)"
    :tracked true
    :covered false
    :hits 0}
   {:file "exchange/api/restful/accounts.clj"
    :lib 'exchange.api.restful.accounts
    :line 11
    :text "(fully-covered)"
    :tracked true
    :covered true
    :hits 1}])

(defn- report-xml
  ([src-ns-path]
   (report-xml src-ns-path sample-forms))
  ([src-ns-path forms]
  (let [output-dir (.toFile (java.nio.file.Files/createTempDirectory "cobertura-report"
                                                                    (make-array java.nio.file.attribute.FileAttribute 0)))]
    (cobertura/report {:output (.getAbsolutePath output-dir)
                       :forms forms
                       :args {:src-ns-path src-ns-path}})
    (with-open [in (io/input-stream (io/file output-dir "cobertura.xml"))]
      (xml/parse in)))))

(defn- xml-elements
  [doc tag]
  (filter #(= tag (:tag %))
          (tree-seq map? :content doc)))

(defn- xml-text
  [element]
  (apply str (filter string? (:content element))))

(defn- xml-line-summary
  [doc]
  (let [lines (xml-elements doc :line)
        covered (count (filter #(pos? (Long/parseLong (get-in % [:attrs :hits] "0"))) lines))]
    {:covered covered
     :valid   (count lines)
     :rate    (if (pos? (count lines))
                (/ (double covered) (count lines))
                1.0)}))

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

(deftest report-uses-cobertura-line-coverage-for-aggregates
  (let [doc (report-xml ["src/main/clojure"] partial-line-forms)
        {:keys [covered valid rate]} (xml-line-summary doc)
        root (first (xml-elements doc :coverage))
        package (first (xml-elements doc :package))
        class-node (first (xml-elements doc :class))]
    (is (= "2" (get-in root [:attrs :lines-covered])))
    (is (= "2" (get-in root [:attrs :lines-valid])))
    (is (= "1.0000" (get-in root [:attrs :line-rate])))
    (is (= "1.0000" (get-in package [:attrs :line-rate])))
    (is (= "1.0000" (get-in class-node [:attrs :line-rate])))
    (is (= covered (Long/parseLong (get-in root [:attrs :lines-covered]))))
    (is (= valid (Long/parseLong (get-in root [:attrs :lines-valid]))))
    (is (= rate (Double/parseDouble (get-in root [:attrs :line-rate]))))))

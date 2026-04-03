(ns cloverage.coverage.cobertura
  "Cloverage custom reporter that generates Cobertura XML coverage reports.

  Usage (CLI):
    lein cloverage --custom-report cloverage.coverage.cobertura/report

  Usage (project.clj):
    :cloverage {:custom-report cloverage.coverage.cobertura/report}

  Writes 'cobertura.xml' to the cloverage output directory (default: target/coverage/).

  The Cobertura XML schema maps Clojure namespaces to packages and classes:
    - Package: all but the last segment of the namespace (e.g. 'example.util' -> 'example')
    - Class:   the full namespace string (e.g. 'example.util')
    - File:    the source path relative to the project root"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.data.xml :as xml]
   [cloverage.report :refer [file-stats line-stats]]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- safe-double [n]
  (double (or n 0)))

(defn- line-rate
  "Returns a coverage ratio in [0.0, 1.0]. Returns 1.0 when nothing is
  instrumented (nothing to cover means fully covered by convention)."
  [covered total]
  (if (pos? total)
    (/ (safe-double covered) (safe-double total))
    1.0))

(defn- fmt-rate
  "Formats a coverage ratio as a 4-decimal-place string for XML attributes."
  [r]
  (format "%.4f" (double r)))

(defn- ns->package
  "Extracts the package portion from a Clojure namespace name.
  'example.util.strings' -> 'example.util'
  'example.core'         -> 'example'
  'core'                 -> '(default)'"
  [lib]
  (let [parts (str/split (str lib) #"\.")
        pkg   (butlast parts)]
    (if (seq pkg)
      (str/join "." pkg)
      "(default)")))

(defn- normalize-path
  "Normalises a file path to use forward slashes (Cobertura convention)."
  [file]
  (str/replace (str file) java.io.File/separator "/"))

(defn- source-roots
  "Returns Cobertura source roots from cloverage args, preserving order."
  [args]
  (let [roots (->> (:src-ns-path args)
                   (map normalize-path)
                   (map #(str/replace % #"/+$" ""))
                   (remove str/blank?)
                   distinct)]
    (if (seq roots)
      roots
      ["src"])))

;; ---------------------------------------------------------------------------
;; XML sexp builders
;; ---------------------------------------------------------------------------

(defn- line-sexps
  "Returns a seq of [:line ...] sexps for all instrumented lines in a file."
  [file-forms]
  (->> (line-stats file-forms)
       (filter :instrumented?)
       (map (fn [{:keys [line hit]}]
              [:line {:number (str line)
                      :hits   (str (max 0 (or hit 0)))
                      :branch "false"}]))))

(defn- class-sexp
  "Builds the [:class ...] sexp for a single source file."
  [fstat file-forms]
  (let [{:keys [file lib covered-lines instrd-lines]} fstat
        rate (line-rate (or covered-lines 0) (or instrd-lines 0))]
    [:class {:name        (str lib)
             :filename    (normalize-path file)
             :line-rate   (fmt-rate rate)
             :branch-rate "0.0"
             :complexity  "0"}
     [:methods]
     (into [:lines] (line-sexps file-forms))]))

(defn- package-sexp
  "Builds the [:package ...] sexp, aggregating coverage across all classes."
  [pkg-name entries]
  (let [pkg-covered (reduce + 0 (map #(or (:covered-lines %) 0) entries))
        pkg-instrd  (reduce + 0 (map #(or (:instrd-lines %) 0) entries))]
    [:package {:name        pkg-name
               :line-rate   (fmt-rate (line-rate pkg-covered pkg-instrd))
               :branch-rate "0.0"
               :complexity  "0"}
     (into [:classes] (map :sexp entries))]))

;; ---------------------------------------------------------------------------
;; Entry point
;; ---------------------------------------------------------------------------

(defn report
  "Generate a Cobertura XML coverage report.

  Invoked by cloverage at the end of a coverage run via:
    --custom-report cloverage.coverage.cobertura/report

  Receives a map with:
    :output  - output directory path (String)
    :forms   - raw coverage forms collection
    :args    - parsed cloverage CLI options
    :project - Leiningen project map"
  [{:keys [output forms args]}]
  (let [output-file    (io/file output "cobertura.xml")
        stats          (doall (file-stats forms))
        forms-by-file  (group-by :file forms)
        sources        (source-roots args)
        total-covered  (reduce + 0 (map #(or (:covered-lines %) 0) stats))
        total-instrd   (reduce + 0 (map #(or (:instrd-lines %) 0) stats))
        timestamp      (quot (System/currentTimeMillis) 1000)

        ;; Enrich each per-file stat with its package name and class sexp
        enriched (map (fn [fstat]
                        (let [file-forms (get forms-by-file (:file fstat) [])]
                          (assoc fstat
                                 :pkg  (ns->package (:lib fstat))
                                 :sexp (class-sexp fstat file-forms))))
                      stats)

        ;; Group classes by package
        by-pkg (group-by :pkg enriched)

        pkg-sexps (map (fn [[pkg entries]]
                         (package-sexp pkg entries))
                       by-pkg)

        coverage-sexp [:coverage
                       {:line-rate        (fmt-rate (line-rate total-covered total-instrd))
                        :branch-rate      "0.0"
                        :lines-covered    (str total-covered)
                        :lines-valid      (str total-instrd)
                        :branches-covered "0"
                        :branches-valid   "0"
                        :complexity       "0"
                        :version          "1"
                        :timestamp        (str timestamp)}
                       (into [:sources] (map (fn [source] [:source source]) sources))
                       (into [:packages] pkg-sexps)]]

    (io/make-parents output-file)
    (println "Writing Cobertura XML report to:" (.getAbsolutePath output-file))
    (with-open [writer (io/writer output-file)]
      (xml/emit (xml/sexp-as-element coverage-sexp) writer
                :doctype "<!DOCTYPE coverage SYSTEM \"http://cobertura.sourceforge.net/xml/coverage-04.dtd\">"))
    (println (format "Coverage: %.1f%% (%d/%d instrumented lines)"
                     (* 100.0 (line-rate total-covered total-instrd))
                     total-covered
                     total-instrd))))

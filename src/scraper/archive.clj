(ns scraper.archive
  (:require [clojure.java.io :as io]))

(def default-dir "data/raw/events")

(defn event-file
  [dir id]
  (io/file dir (str id ".json")))

(defn read-json
  [dir id]
  (let [file (event-file dir id)]
    (when (.exists file)
      (slurp file))))

(defn write-json!
  [dir id json]
  (let [file (event-file dir id)]
    (io/make-parents file)
    (spit file json)))

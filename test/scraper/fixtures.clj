(ns scraper.fixtures
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(defn read-fixture
  [file]
  (json/parse-string (slurp (io/resource (str "fixtures/" file))) true))

(def event
  (read-fixture "event-1336.json"))

(def fights-without-winner
  (read-fixture "fights-without-winner.json"))

(defn with-temp-dir
  [f]
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "scraper-test-" (random-uuid)))]
    (try
      (f (str dir))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file true))))))

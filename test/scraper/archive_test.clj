(ns scraper.archive-test
  (:require [clojure.test :refer [deftest is testing]]
            [scraper.archive :as archive]
            [scraper.fixtures :refer [with-temp-dir]]))

(deftest archive-test
  (with-temp-dir
    (fn [dir]
      (testing "an event that was never archived"
        (is (nil? (archive/read-json dir 1336))))

      (testing "returns exactly what was written"
        (archive/write-json! dir 1336 "{\"Name\":\"Jiří Procházka\"}")
        (is (= "{\"Name\":\"Jiří Procházka\"}" (archive/read-json dir 1336)))))))

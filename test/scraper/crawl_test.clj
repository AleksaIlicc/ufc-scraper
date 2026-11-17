(ns scraper.crawl-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [scraper.api :as api]
            [scraper.archive :as archive]
            [scraper.crawl :as crawl]
            [scraper.fixtures :refer [event with-temp-dir]]))

(def event-json (json/generate-string event))

(def missing-json "{\"LiveEventDetail\":{}}")

(deftest load-event-test
  (with-temp-dir
    (fn [dir]
      (let [requests (atom [])]
        (with-redefs [api/fetch-event-json (fn [id]
                                             (swap! requests conj id)
                                             (if (= 2 id) missing-json event-json))]
          (testing "an event that is not archived yet is downloaded and archived as is"
            (is (= event (crawl/load-event! dir 1)))
            (is (= event-json (archive/read-json dir 1)))
            (is (= [1] @requests)))

          (testing "an archived event is read from the archive without a request"
            (reset! requests [])
            (is (= event (crawl/load-event! dir 1)))
            (is (= [] @requests)))

          (testing "a missing event is not archived"
            (is (= {:LiveEventDetail {}} (crawl/load-event! dir 2)))
            (is (nil? (archive/read-json dir 2)))))))))

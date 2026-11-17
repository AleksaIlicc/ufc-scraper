(ns scraper.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [scraper.api :as api]
            [scraper.core :as core]
            [scraper.fixtures :refer [event]]))

(deftest format-scores-test
  (is (= "29-28, 28-29, 29-28" (core/format-scores [[29 28] [28 29] [29 28]])))
  (is (= "" (core/format-scores nil))))

(deftest main-test
  (let [requested-url (atom nil)]
    (with-redefs [api/fetch-data (fn [url] (reset! requested-url url) event)]
      (testing "fetches the default event when no id is given"
        (with-out-str (core/-main))
        (is (= (api/event-url core/default-event-id) @requested-url)))

      (testing "fetches the event id passed on the command line"
        (with-out-str (core/-main "1337"))
        (is (= "https://d29dxerjsp82wz.cloudfront.net/api/v3/event/live/1337.json"
               @requested-url))))))

(deftest event-line-test
  (is (= "1336 | UFC 333: Volkanovski vs. Evloev | 2026-10-24T14:00Z | 14 fights"
         (core/event-line 1336 event)))
  (is (= "1339 missing" (core/event-line 1339 {:LiveEventDetail {}}))))

(deftest main-range-test
  (let [requested-urls (atom [])
        missing-url    (api/event-url 1339)]
    (with-redefs [core/request-delay-ms 0
                  api/fetch-data        (fn [url]
                                          (swap! requested-urls conj url)
                                          (if (= missing-url url) {:LiveEventDetail {}} event))]
      (let [output (with-out-str (core/-main "1338" "1340"))]
        (is (= (map api/event-url [1338 1339 1340]) @requested-urls))
        (is (= ["1338 | UFC 333: Volkanovski vs. Evloev | 2026-10-24T14:00Z | 14 fights"
                "1339 missing"
                "1340 | UFC 333: Volkanovski vs. Evloev | 2026-10-24T14:00Z | 14 fights"]
               (str/split-lines output)))))))

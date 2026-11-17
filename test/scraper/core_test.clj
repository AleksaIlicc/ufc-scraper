(ns scraper.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [scraper.core :as core]
            [scraper.crawl :as crawl]
            [scraper.fixtures :refer [event]]))

(deftest format-scores-test
  (is (= "29-28, 28-29, 29-28" (core/format-scores [[29 28] [28 29] [29 28]])))
  (is (= "" (core/format-scores nil))))

(deftest main-test
  (let [requested-id (atom nil)]
    (with-redefs [crawl/load-event! (fn [_ id] (reset! requested-id id) event)]
      (testing "loads the default event when no id is given"
        (with-out-str (core/-main))
        (is (= core/default-event-id @requested-id)))

      (testing "loads the event id passed on the command line"
        (with-out-str (core/-main "1337"))
        (is (= "1337" @requested-id))))))

(deftest event-line-test
  (is (= "1336 | UFC 333: Volkanovski vs. Evloev | 2026-10-24T14:00Z | 14 fights"
         (core/event-line 1336 event)))
  (is (= "1339 missing" (core/event-line 1339 {:LiveEventDetail {}}))))

(deftest main-range-test
  (let [requested-ids (atom [])]
    (with-redefs [core/request-delay-ms 0
                  crawl/load-event!     (fn [_ id]
                                          (swap! requested-ids conj id)
                                          (if (= 1339 id) {:LiveEventDetail {}} event))]
      (let [output (with-out-str (core/-main "1338" "1340"))]
        (is (= [1338 1339 1340] @requested-ids))
        (is (= ["1338 | UFC 333: Volkanovski vs. Evloev | 2026-10-24T14:00Z | 14 fights"
                "1339 missing"
                "1340 | UFC 333: Volkanovski vs. Evloev | 2026-10-24T14:00Z | 14 fights"]
               (str/split-lines output)))))))

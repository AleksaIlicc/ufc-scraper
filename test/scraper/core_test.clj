(ns scraper.core-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [scraper.core :as core]))

(defn- read-fixture
  [file]
  (json/parse-string (slurp (io/resource (str "fixtures/" file))) true))

(def event
  (read-fixture "event-1336.json"))

(def fights-without-winner
  (read-fixture "fights-without-winner.json"))

(deftest fight-summary-real-data-test
  (let [summaries (mapv core/fight-summary (get-in event [:LiveEventDetail :FightCard]))
        by-order  (into {} (map (juxt :order identity)) summaries)]
    (testing "main event"
      (is (= {:order    1
              :segment  "Main"
              :status   "Final"
              :fighters ["Alexander Volkanovski" "Movsar Evloev"]
              :winner   "Alexander Volkanovski"
              :method   "KO/TKO"
              :round    2
              :time     "4:54"
              :duration 594
              :scores   nil}
             (by-order 1))))

    (testing "winner listed as the second fighter"
      (is (= {:order    6
              :segment  "Prelims1"
              :status   "Final"
              :fighters ["Azamat Murzakanov" "Dominick Reyes"]
              :winner   "Dominick Reyes"
              :method   "KO/TKO"
              :round    1
              :time     "2:40"
              :duration 160
              :scores   nil}
             (by-order 6))))

    (testing "five round decision lasts the full 25 minutes"
      (is (= {:method   "Decision - Unanimous"
              :duration 1500
              :scores   [[48 47] [48 47] [48 47]]}
             (select-keys (by-order 2) [:method :duration :scores]))))

    (testing "split decision"
      (is (= {:method "Decision - Split"
              :scores [[29 28] [28 29] [29 28]]}
             (select-keys (by-order 10) [:method :scores]))))

    (testing "scores follow the fighter order when the second fighter wins"
      (is (= {:fighters ["Bolaji Oki" "Daniel Santos"]
              :winner   "Daniel Santos"
              :scores   [[28 29] [28 29] [28 29]]}
             (select-keys (by-order 12) [:fighters :winner :scores]))))

    (testing "every finished fight has a winner who took part in it"
      (doseq [{:keys [order fighters winner]} summaries]
        (is (some #{winner} fighters) (str "fight " order))))

    (testing "only decisions have scorecards, one per judge"
      (doseq [{:keys [order method scores]} summaries]
        (if (str/starts-with? method "Decision")
          (is (= 3 (count scores)) (str "fight " order))
          (is (nil? scores) (str "fight " order)))))))

(deftest fight-summary-without-winner-test
  (let [{:keys [draw upcoming]} fights-without-winner]
    (testing "draw"
      (is (= {:status   "Final"
              :winner   nil
              :duration 900
              :scores   [[28 28] [28 28] [29 27]]}
             (select-keys (core/fight-summary draw) [:status :winner :duration :scores]))))

    (testing "upcoming fight"
      (is (= {:status "Upcoming" :winner nil :method nil :duration nil :scores nil}
             (select-keys (core/fight-summary upcoming) [:status :winner :method :duration :scores]))))))

(deftest clock->seconds-test
  (is (= 0 (core/clock->seconds "0:00")))
  (is (= 294 (core/clock->seconds "4:54")))
  (is (= 300 (core/clock->seconds "5:00"))))

(deftest format-scores-test
  (is (= "29-28, 28-29, 29-28" (core/format-scores [[29 28] [28 29] [29 28]])))
  (is (= "" (core/format-scores nil))))

(deftest event-summary-test
  (let [summary (core/event-summary event)]
    (is (= {:event "UFC 333: Volkanovski vs. Evloev"
            :start "2026-10-24T14:00Z"
            :venue "Etihad Arena - Abu Dhabi, United Arab Emirates"}
           (dissoc summary :fights)))
    (is (= 14 (count (:fights summary))))))

(deftest main-test
  (let [requested-url (atom nil)]
    (with-redefs [core/fetch-data (fn [url] (reset! requested-url url) event)]
      (testing "fetches the default event when no id is given"
        (with-out-str (core/-main))
        (is (= (core/event-url core/default-event-id) @requested-url)))

      (testing "fetches the event id passed on the command line"
        (with-out-str (core/-main "1337"))
        (is (= "https://d29dxerjsp82wz.cloudfront.net/api/v3/event/live/1337.json"
               @requested-url))))))

(ns scraper.core-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
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
              :time     "4:54"}
             (by-order 1))))

    (testing "winner listed as the second fighter"
      (is (= {:order    6
              :segment  "Prelims1"
              :status   "Final"
              :fighters ["Azamat Murzakanov" "Dominick Reyes"]
              :winner   "Dominick Reyes"
              :method   "KO/TKO"
              :round    1
              :time     "2:40"}
             (by-order 6))))

    (testing "every finished fight has a winner who took part in it"
      (doseq [{:keys [order fighters winner]} summaries]
        (is (some #{winner} fighters) (str "fight " order))))))

(deftest fight-summary-without-winner-test
  (let [{:keys [draw upcoming]} fights-without-winner]
    (testing "draw"
      (is (= {:status "Final" :winner nil}
             (select-keys (core/fight-summary draw) [:status :winner]))))

    (testing "upcoming fight"
      (is (= {:status "Upcoming" :winner nil :method nil}
             (select-keys (core/fight-summary upcoming) [:status :winner :method]))))))

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

(ns scraper.core-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [scraper.core :as core]))

(def event
  (json/parse-string (slurp (io/resource "fixtures/event-1336.json")) true))

(def fight-card
  (get-in event [:LiveEventDetail :FightCard]))

(deftest fight-summary-real-data-test
  (let [summaries (mapv core/fight-summary fight-card)
        by-order  (into {} (map (juxt :order identity)) summaries)]
    (testing "main event"
      (is (= {:order    1
              :segment  "Main"
              :fighters ["Alexander Volkanovski" "Movsar Evloev"]
              :winner   "Alexander Volkanovski"
              :method   "KO/TKO"
              :round    2
              :time     "4:54"}
             (by-order 1))))

    (testing "winner listed as the second fighter"
      (is (= {:order    6
              :segment  "Prelims1"
              :fighters ["Azamat Murzakanov" "Dominick Reyes"]
              :winner   "Dominick Reyes"
              :method   "KO/TKO"
              :round    1
              :time     "2:40"}
             (by-order 6))))

    (testing "every finished fight has exactly one winner who took part in it"
      (is (= 14 (count summaries)))
      (doseq [{:keys [order fighters winner]} summaries]
        (is (some #{winner} fighters) (str "fight " order))))))

(deftest fight-summary-without-winner-test
  (testing "no winner when no fighter has a Win outcome (e.g. draw or upcoming fight)"
    (doseq [outcome ["Draw" nil]]
      (let [fight (update (first fight-card) :Fighters
                          (partial mapv #(assoc-in % [:Outcome :Outcome] outcome)))]
        (is (nil? (:winner (core/fight-summary fight))) (pr-str outcome))))))

(deftest main-test
  (testing "prints event header and one line per fight, with fetch-data stubbed"
    (let [requested-url (atom nil)]
      (with-redefs [core/fetch-data (fn [url] (reset! requested-url url) event)]
        (let [lines (str/split-lines (with-out-str (core/-main)))]
          (is (= core/api-url @requested-url))
          (is (= ["Event: UFC 333: Volkanovski vs. Evloev"
                  "Start: 2026-10-24T14:00Z"
                  "Venue: Etihad Arena - Abu Dhabi, United Arab Emirates"
                  "Num of fights: 14"]
                 (take 4 lines)))
          (is (= 14 (count (drop 4 lines))) "one line per fight"))))))

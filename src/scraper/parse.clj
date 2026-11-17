(ns scraper.parse
  (:require [clojure.string :as str]))

(def round-seconds 300)

(defn missing?
  [event]
  (empty? (:LiveEventDetail event)))

(defn fighter-name
  [{{:keys [FirstName LastName]} :Name}]
  (str FirstName " " LastName))

(defn winner
  [fighters]
  (some #(when (= "Win" (get-in % [:Outcome :Outcome])) (fighter-name %)) fighters))

(defn clock->seconds
  [clock]
  (let [[minutes seconds] (map parse-long (str/split clock #":"))]
    (+ (* 60 minutes) seconds)))

(defn fight-duration
  [{:keys [EndingRound EndingTime]}]
  (when (and EndingRound EndingTime)
    (+ (* round-seconds (dec EndingRound)) (clock->seconds EndingTime))))

(defn scorecards
  [fighters fight-scores]
  (let [fighter-ids (map :FighterId fighters)]
    (not-empty
     (mapv (fn [{judge-scores :Fighters}]
             (let [score-by-id (into {} (map (juxt :FighterId :Score)) judge-scores)]
               (mapv score-by-id fighter-ids)))
           fight-scores))))

(defn fight-summary
  [{:keys [FightOrder CardSegment Status Fighters Result]}]
  {:order    FightOrder
   :segment  CardSegment
   :status   Status
   :fighters (mapv fighter-name Fighters)
   :winner   (winner Fighters)
   :method   (:Method Result)
   :round    (:EndingRound Result)
   :time     (:EndingTime Result)
   :duration (fight-duration Result)
   :scores   (scorecards Fighters (:FightScores Result))})

(defn event-summary
  [{{:keys [Name StartTime Location FightCard]} :LiveEventDetail}]
  {:event  Name
   :start  StartTime
   :venue  (str (:Venue Location) " - " (:City Location) ", " (:Country Location))
   :fights (mapv fight-summary FightCard)})

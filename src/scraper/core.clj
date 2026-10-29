(ns scraper.core
  (:require [clj-http.client :as http]))

(def api-url "https://d29dxerjsp82wz.cloudfront.net/api/v3/event/live/1336.json")

(defn fetch-data
  [url]
  (:body (http/get url {:as :json})))

(defn fighter-name
  [{{:keys [FirstName LastName]} :Name}]
  (str FirstName " " LastName))

(defn fight-summary
  [{:keys [FightOrder CardSegment Fighters Result]}]
  (let [winner (some #(when (= "Win" (get-in % [:Outcome :Outcome])) (fighter-name %)) Fighters)]
    {:order   FightOrder
     :segment CardSegment
     :fighters (mapv fighter-name Fighters)
     :winner  winner
     :method  (:Method Result)
     :round   (:EndingRound Result)
     :time    (:EndingTime Result)}))

(defn -main [& _args]
  (let [{:keys [Name StartTime Location FightCard]} (:LiveEventDetail (fetch-data api-url))]
    (println "Event:" Name)
    (println "Start:" StartTime)
    (println "Venue:" (:Venue Location) "-" (:City Location) "," (:Country Location))
    (println "Num of fights:" (count FightCard))
    (doseq [fight (map fight-summary FightCard)]
      (println fight))))

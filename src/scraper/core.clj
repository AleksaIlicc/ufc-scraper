(ns scraper.core
  (:require [clj-http.client :as http]))

(def base-url "https://d29dxerjsp82wz.cloudfront.net/api/v3/event/live/")

(def default-event-id 1336)

(def timeout-ms 10000)

(defn event-url
  [id]
  (str base-url id ".json"))

(defn fetch-data
  [url]
  (:body (http/get url {:as                 :json
                        :connection-timeout timeout-ms
                        :socket-timeout     timeout-ms})))

(defn fighter-name
  [{{:keys [FirstName LastName]} :Name}]
  (str FirstName " " LastName))

(defn winner
  [fighters]
  (some #(when (= "Win" (get-in % [:Outcome :Outcome])) (fighter-name %)) fighters))

(defn fight-summary
  [{:keys [FightOrder CardSegment Fighters Result]}]
  {:order    FightOrder
   :segment  CardSegment
   :fighters (mapv fighter-name Fighters)
   :winner   (winner Fighters)
   :method   (:Method Result)
   :round    (:EndingRound Result)
   :time     (:EndingTime Result)})

(defn -main [& [event-id]]
  (let [url (event-url (or event-id default-event-id))
        {:keys [Name StartTime Location FightCard]} (:LiveEventDetail (fetch-data url))]
    (println "Event:" Name)
    (println "Start:" StartTime)
    (println "Venue:" (str (:Venue Location) " - " (:City Location) ", " (:Country Location)))
    (println "Num of fights:" (count FightCard))
    (doseq [fight (map fight-summary FightCard)]
      (println fight))))

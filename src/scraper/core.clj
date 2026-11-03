(ns scraper.core
  (:require [clj-http.client :as http]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

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
  [{:keys [FightOrder CardSegment Status Fighters Result]}]
  {:order    FightOrder
   :segment  CardSegment
   :status   Status
   :fighters (mapv fighter-name Fighters)
   :winner   (winner Fighters)
   :method   (:Method Result)
   :round    (:EndingRound Result)
   :time     (:EndingTime Result)})

(defn event-summary
  [{{:keys [Name StartTime Location FightCard]} :LiveEventDetail}]
  {:event  Name
   :start  StartTime
   :venue  (str (:Venue Location) " - " (:City Location) ", " (:Country Location))
   :fights (mapv fight-summary FightCard)})

(defn -main [& [event-id]]
  (let [url (event-url (or event-id default-event-id))
        {:keys [event start venue fights]} (event-summary (fetch-data url))]
    (println "Event:" event)
    (println "Start:" start)
    (println "Venue:" venue)
    (println "Num of fights:" (count fights))
    (pprint/print-table [:order :segment :status :fighters :winner :method :round :time]
                        (map #(update % :fighters (partial str/join " vs ")) fights))))

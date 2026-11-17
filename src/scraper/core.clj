(ns scraper.core
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [scraper.api :as api]
            [scraper.parse :as parse]))

(def default-event-id 1336)

(def request-delay-ms 1000)

(defn format-scores
  [scores]
  (str/join ", " (map (partial str/join "-") scores)))

(defn table-row
  [fight]
  (-> fight
      (update :fighters (partial str/join " vs "))
      (update :scores format-scores)))

(defn event-line
  [id data]
  (if (parse/missing? data)
    (str id " missing")
    (let [{:keys [event start fights]} (parse/event-summary data)]
      (str id " | " event " | " start " | " (count fights) " fights"))))

(defn print-event
  [id]
  (let [{:keys [event start venue fights]} (parse/event-summary (api/fetch-data (api/event-url id)))]
    (println "Event:" event)
    (println "Start:" start)
    (println "Venue:" venue)
    (println "Num of fights:" (count fights))
    (pprint/print-table [:order :segment :status :fighters :winner :method :round :time :duration :scores]
                        (map table-row fights))))

(defn print-events
  [from to]
  (doseq [id (range from (inc to))]
    (println (event-line id (api/fetch-data (api/event-url id))))
    (Thread/sleep request-delay-ms)))

(defn -main [& [from to]]
  (if to
    (print-events (parse-long from) (parse-long to))
    (print-event (or from default-event-id))))

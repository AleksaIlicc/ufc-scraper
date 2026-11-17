(ns scraper.crawl
  (:require [scraper.api :as api]
            [scraper.archive :as archive]
            [scraper.parse :as parse]))

(defn download!
  [dir id]
  (let [body  (api/fetch-event-json id)
        event (parse/json->event body)]
    (when-not (parse/missing? event)
      (archive/write-json! dir id body))
    event))

(defn load-event!
  [dir id]
  (if-let [json (archive/read-json dir id)]
    (parse/json->event json)
    (download! dir id)))

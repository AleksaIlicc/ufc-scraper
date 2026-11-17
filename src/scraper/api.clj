(ns scraper.api
  (:require [clj-http.client :as http]))

(def base-url "https://d29dxerjsp82wz.cloudfront.net/api/v3/event/live/")

(def timeout-ms 10000)

(defn event-url
  [id]
  (str base-url id ".json"))

(defn fetch-data
  [url]
  (:body (http/get url {:as                 :json
                        :connection-timeout timeout-ms
                        :socket-timeout     timeout-ms})))

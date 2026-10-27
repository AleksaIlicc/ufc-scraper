(ns scraper.core
  (:require [clj-http.client :as http]))

(def api-url "https://jsonplaceholder.typicode.com/posts")

(defn fetch-data
  [url]
  (:body (http/get url {:as :json})))

(defn -main [& _args]
  (let [posts (fetch-data api-url)]
    (println "Num of records:" (count posts))
    (println "First record:" (first posts))))

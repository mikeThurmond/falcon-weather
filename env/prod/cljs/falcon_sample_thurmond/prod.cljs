(ns falcon-sample-thurmond.prod
  (:require [falcon-sample-thurmond.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)

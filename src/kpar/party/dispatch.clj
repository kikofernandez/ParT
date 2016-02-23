(ns kpar.party.dispatch
  (:require [kpar.party.data :as data]))

;; todo: needs to choose the right implementation based on
;; throughput or latency of the system. i would prefer throughput,
;; which means choosing more granular tasks
(defn choose-implementation
  "Chooses an implementation from the list of implementations
  from sliding-window-gen."
  [fun-versions]
  (ffirst fun-versions))

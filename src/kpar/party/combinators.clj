(ns kpar.party.combinators
  (:require [kpar.party.data :as data]))

(defn >>
  "sequence combinator: similar to map but act on a parallel collection"
  [p & funs]
  (reduce #(data/update-party % %2) p funs))

(ns kpar.core
  (:require [kpar.data :as data])
  (:import (java.lang IllegalArgumentException)
           (java.util.concurrent CompletableFuture Executors)
           (java.util.function Supplier)))

(def ^:once executor
  (Executors/newWorkStealingPool))

(defmacro spawn
  "creates a new future that runs the computation asynchronously"
  [& fun]
  `(CompletableFuture/supplyAsync
    (reify Supplier
      (get [_] ~@fun))
    executor))

(defn liftv
  "lifts a value to a parallel collection, e.g.

     liftv 42 -> Par 42

   from this point on, you have to interact with it via combinators"
  [v]
  (data/create-f data/party-val v))

(defn liftf
  "lifts a future into a parallel collection, e.g.

     liftf (spawn (long-running-expression))

   only spawned expressions can be lifted into a parallel collection"
  [fut]
  (if (future? fut)
    (if (instance? CompletableFuture fut)
      (data/create data/party-fut fut)
      (throw (IllegalArgumentException. "Unexpected future created with method distinct from 'spawn'")))
    (throw (IllegalArgumentException. "Value needs to be of future type"))))

(defn |
  "par combinator: aggregates the parallel collections into a list"
  [& pars]
  {:pre [(> (count pars) 1)]}
  (reduce #(data/create data/party-par % %2) pars))

(defn >>
  "sequence combinator: similar to map but act on a parallel collection"
  [p fun]
  (data/update-party p fun))

(defn extract
  "extract combinator: gets the values from the parallel collection.
   this operation blocks the current working thread"
  [p]
  (data/extract p))

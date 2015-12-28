(ns kpar.core
  (:require [kpar.data :as data])
  (:import (java.lang IllegalArgumentException)
           (java.util.concurrent CompletableFuture Executors)
           (java.util.function Supplier Function)))

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
  (data/create-kdf data/par-val v))

(defn liftf
  "lifts a future into a parallel collection, e.g.

     liftf (spawn (long-running-expression))

   only spawned expressions can be lifted into a parallel collection"
  [fut]
  (if (future? fut)
    (if (instance? CompletableFuture fut)
      (data/create-kd data/par-fut fut)
      (throw (IllegalArgumentException. "Unexpected future created with method distinct from 'spawn'")))
    (throw (IllegalArgumentException. "Value needs to be of future type"))))

(defn |
  "par combinator: aggregates the parallel collections into a list"
  [& pars] pars)

(defn ^:private reify-sequence
  [p fun]
  (reify Function
    (apply [_ t]
      (fun t))))

(defn >>
  "sequence combinator: similar to map but act on a parallel collection"
  [ps fun]
  (map #(assoc % :value (.thenApplyAsync (:value %) (reify-sequence % fun))) ps))

(defn extract
  "extract combinator: gets the values from the parallel collection.
   this operation blocks the current working thread"
  [p]
  (map data/extractvalue-kd p))


;; Working example
;; (let [f (spawn (+ 3 2))
;;       v 43
;;       p (| (liftf f) (liftv v))]
;;   (-> (>> p #(+ 1 %))
;;       (>> #(+ 1 %))
;;       (>> #(* 3 %))
;;       extract))


;; TODO: working example of the thenApplyAsync for future chaining
;; (-> (CompletableFuture/supplyAsync (reify Supplier (get [t] (println t) 3)))
;;     (.thenApplyAsync (reify Function
;;                        (apply [_ t] (println (+ t 1)) 5)
;;                        ))
;;     (.get))

(ns kpar.core
  (:require [kpar.party.data :as data]
            [kpar.party.dispatch :as dsp]
            [kpar.party.combinators :as c]
            [clojure.core.memoize :as m])
  (:import (java.lang IllegalArgumentException)
           (java.util.concurrent CompletableFuture Executors)
           (java.util.function Supplier)))

(defonce executor (Executors/newWorkStealingPool))
(defonce ^:private window-size 2)
(defonce ^:private cache-limit 64)

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

(defn ||
  "par combinator: aggregates the parallel collections into a list"
  [& pars]
  {:pre [(> (count pars) 1)]}
  (reduce #(data/create data/party-par % %2) pars))

(defn extract
  "extract combinator: gets the values from the parallel collection.
   this operation blocks the current working thread"
  [p]
  (data/extract p))

;; todo: use the size variable to create customised sliding windows
(defn ^:private sliding-window-gen
  "Sliding window generator, takes functions funs and return multiple
  sliding windows to choose from.
  The returned format is as follows:

  e.g. (sliding-window-gen inc dec inc')
  ([(comp inc dec inc')] [(comp inc) (comp dec inc')] [(comp inc dec) (comp inc')])
  "
  [size & funs]
  (->> (map #(split-at % funs) (range 0 (count funs)))
       (map (fn [l] (if (empty? (first l)) [(second l)] l)))
       (map (fn [v] (mapv #(cons comp %) v)))))

(def >>!
  "memoize version of >> with a max. number of entries defined by cache-limit
  and lru replacement policy"
  (m/lru c/>> :lru/threshold cache-limit))

(defmacro >>
  "Asynchronously execute the functions funs in each item of the ParT p.

  Returns a new ParT on which the functions will be executed asynchronously"
  [p & funs]
  (let [fn-versioning (mapv #(with-meta % {:time 0.0}) (apply sliding-window-gen window-size funs))]
    `(c/>> ~p (dsp/choose-implementation ~fn-versioning))))



;; format returned from result
;;([(comp inc dec inc)], [(comp inc) (comp dec inc)], [(comp inc dec) (comp inc)])

;; (macroexpand '(>>= (liftv 1) inc dec))
;; (>> 1 [[(comp inc dec)] [(comp inc) (comp dec)]])
;; (macroexpand '(>>= (liftv 1) inc dec))
;; (>> (|| (liftv 1) (liftv 34)) inc dec)

(ns kpar.core
  (:import (java.lang IllegalArgumentException)
           (java.util.concurrent CompletableFuture FutureTask Executors)
           (java.util.function Supplier Function)))

(defonce executor (Executors/newWorkStealingPool))

(def par-default {:value nil})
(def par-val (merge {:type :v} par-default))
(def par-fut (merge {:type :f} par-default))
(def par-par (merge {:type :p} par-default))
(def par-join (merge {:type :j} par-default))
(def par-futpar (merge {:type :fp} par-default))
(def par-array (merge {:type :m} par-default))

(defmacro spawn
  "creates a new future that runs the computation asynchronously"
  [fun]
  `(CompletableFuture/supplyAsync
    (reify Supplier
      (get [_] ~fun))
    executor))

(defn liftv
  "lifts a value to a parallel collection, e.g.

     liftv 42 -> Par 42

   from this point on, you have to interact with it via combinators"
  [val]
  (assoc par-val :value (CompletableFuture/completedFuture val)))

(defn liftf
  "lifts a future into a parallel collection, e.g.

     liftf (spawn (long-running-expression))

   only spawned expressions can be lifted into a parallel collection"
  [fut]
  (if (future? fut)
    (if (instance? CompletableFuture fut)
      (assoc par-fut :value fut)
      (throw (IllegalArgumentException. "Unexpected future created with method distinct from 'spawn'")))
    (throw (IllegalArgumentException. "Value needs to be of future type"))))

(defn |
  "Par combinator: aggregates the parallel collections into a list"
  [& pars] pars)

(defn >>
  "Sequence combinator: similar to map but act on a parallel collection"
  [ps fun]
  (map (fn [p] (.thenApplyAsync (:value p)
                                (reify Function
                                  (apply [_ t]
                                    (assoc p :value (fun t))))))
       ps))

(let [f (spawn (+ 3 2))
      v 43
      p (| (liftf f) (liftv v))]
  (>> p #(+ 1 %)))


;; TODO: working example of the thenApplyAsync for future chaining
;; (-> (CompletableFuture/supplyAsync (reify Supplier (get [t] (println t) 3)))
;;     (.thenApplyAsync (reify Function
;;                        (apply [_ t] (println (+ t 1)) 5)
;;                        ))
;;     (.get))

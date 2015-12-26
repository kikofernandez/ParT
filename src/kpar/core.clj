(ns kpar.core
  (:import (java.lang IllegalArgumentException)
           (java.util.concurrent CompletableFuture FutureTask Executors)
           (java.util.function Supplier Function)))

(defonce executor (Executors/newWorkStealingPool))

(def par-val {:type :v})
(def par-fut {:type :f})
(def par-par {:type :p})
(def par-join {:type :j})
(def par-futpar {:type :fp})
(def par-array {:type :m})

;; TODO: spawn creates a new task that fulfills the future
;; Needs to be a macro, no evaluation as `fun` is the un-evaluated function
;; FutureTask receives a function that is already a callable
(defn spawn [fun]
  (let [fut (FutureTask.
             #(CompletableFuture/supplyAsync
               (reify Supplier
                 (get [_] (println fun) 3))))]
    (.execute executor fut)))

(spawn 42)

(defn liftv
  "Lifts a value to a Parallel collection. e.g.

     liftv 42 -> Par 42

   From this point on, you have to interact with it via combinators"
  [val]
  (assoc par-val :value val))


;; Lifts a future into the Par structure
(defmulti liftf :type)
(defmethod liftf :f [fut] (assoc par-fut :value fut))
(defmethod liftf :fp [fut] (assoc par-futpar :value fut))
(defmethod liftf :default [fut]
  (throw (IllegalArgumentException. "Value needs to be of future type")))

(defn |
  "Par combinator: aggregates the parallel collections into a list"
  [& pars] pars)

;; This implementation needs to wrap the fun function into a function
;; that extracts the content and passes the value to the function
(defn >>
  "Sequence combinator: similar to map but act on a parallel collection"
  [ps fun]
  map fun ps)


;; TODO: working example of the thenApplyAsync for future chaining
(-> (CompletableFuture/supplyAsync (reify Supplier (get [t] (println t) 3)))
    (.thenApplyAsync (reify Function
                       (apply [_ t] (println (+ t 1)) 5)
                       ))
    (.get))

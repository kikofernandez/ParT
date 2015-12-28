(ns kpar.data
  (:import (java.util.concurrent CompletableFuture)
           (java.util.function Function)))

(def ^:private par-default {:value nil})
(def par-val (merge {:type :v} par-default))
(def par-fut (merge {:type :f} par-default))
(def par-par (merge {:type :p} par-default))
(def par-join (merge {:type :j} par-default))
(def par-futpar (merge {:type :fp} par-default))
(def par-array (merge {:type :m} par-default))

(defn create-kd
  [p v]
  (assoc p :value v))

(defn create-kdf
  [p v]
  (create-kd p (CompletableFuture/completedFuture v)))

(defn getvalue-kd
  [p]
  (:value p))

(defn ^:private reify-sequence
  [p fun]
  (reify Function
    (apply [_ t]
      (fun t))))

(defn ^:private continuation
  [p fun]
  (let [v (getvalue-kd p)
        s (reify-sequence p fun)]
    (.thenApplyAsync v s)))

(defn setvalue-kd
  [p fun]
  (assoc p :value (continuation p fun)))

(def extractvalue-kd
  "internal function for getting a value. blocks the main thread"
  (fn [v] (.get (getvalue-kd v))))

(defn gettype-kd
  [p]
  (:type p))

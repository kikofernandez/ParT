(ns kpar.data
  (:import (java.util.concurrent CompletableFuture)
           (java.util.function Function)))

(def ^:private par-default {:value nil})
(def par-empty (merge {:type :s} par-default))
(def par-val (merge {:type :v} par-default))
(def par-fut (merge {:type :f} par-default))
(def par-par {:type :p, :left nil, :right nil})
(def par-join (merge {:type :j} par-default))
(def par-futpar (merge {:type :fp} par-default))
(def par-array (merge {:type :m} par-default))

(defn create-kd
  [p v & vs]
  (if (empty? vs)
    (assoc p :value v)
    (assoc p :left v :right (first vs))))

(defn create-kdf
  [p v]
  (create-kd p (CompletableFuture/completedFuture v)))

(defn getvalue-kd
  [p]
  (get p :value nil))

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

(defn setvalue-kdv
  [p fun]
  (assoc p :value (continuation p fun)))

(defmulti setvalue-kd :type)
(defmethod setvalue-kd :s [p fun] p)
(defmethod setvalue-kd :v [p fun] (setvalue-kdv p fun))
(defmethod setvalue-kd :f [p fun] (setvalue-kdv p fun))
(defmethod setvalue-kd :p [p fun] (create-kd par-par
                                             (setvalue-kd (:left p) fun)
                                             (setvalue-kd (:right p) fun)))

(defn ^:private extractvalue-kdv
  "internal function for getting a value. blocks the main thread"
  [v]
  (some-> (getvalue-kd v)
          .get))

(defmulti extractvalue-kd :type)
(defmethod extractvalue-kd :s [_] nil)
(defmethod extractvalue-kd :v [p] [(extractvalue-kdv p)])
(defmethod extractvalue-kd :f [p] [(extractvalue-kdv p)])
(defmethod extractvalue-kd :p [p] (concat (extractvalue-kd (:left p))
                                          (extractvalue-kd (:right p))))

(defn gettype-kd
  [p]
  (:type p))

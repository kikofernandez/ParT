(ns kpar.data
  (:import (java.util.concurrent CompletableFuture)
           (java.util.function Function)))

(defrecord ParT [type value dependents])


(def ^:private par-default (->ParT nil nil []))
(def par-empty (assoc par-default :type :s))
(def par-val (assoc par-default :type :v))
(def par-fut (assoc par-default :type :f))
(def par-par (assoc par-default :type :p, :value {:left nil, :right nil}))
(def par-join (assoc par-default :type :j))
(def par-futpar (assoc par-default :type :fp))
(def par-array (assoc par-default :type :m))

(defn create-kd
  [p v & vs]
  {:pre [(< (count vs) 2)]}
  (if (empty? vs)
    (assoc p :value v)
    (assoc p :value {:left v :right (first vs)})))

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
  (assoc p :value (continuation p fun) :dependents [(getvalue-kd p)]))

(defmulti setvalue-kd :type)
(defmethod setvalue-kd :s [p fun] p)
(defmethod setvalue-kd :v [p fun] (setvalue-kdv p fun))
(defmethod setvalue-kd :f [p fun] (setvalue-kdv p fun))
(defmethod setvalue-kd :p [p fun]
  (create-kd par-par
             (setvalue-kd (get-in p [:value :left]) fun)
             (setvalue-kd (get-in p [:value :right]) fun)))

(defn ^:private extractvalue-kdv
  "internal function for getting a value. blocks the main thread"
  [v]
  (some-> (getvalue-kd v)
          .get))

(defmulti extractvalue-kd :type)
(defmethod extractvalue-kd :s [_] nil)
(defmethod extractvalue-kd :v [p] [(extractvalue-kdv p)])
(defmethod extractvalue-kd :f [p] [(extractvalue-kdv p)])
(defmethod extractvalue-kd :p [p] (concat (extractvalue-kd (get-in p [:value :left]))
                                          (extractvalue-kd (get-in p [:value :right]))))

(defn gettype-kd
  [p]
  (:type p))

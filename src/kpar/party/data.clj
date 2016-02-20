(ns kpar.party.data
  (:import (java.util.concurrent CompletableFuture)
           (java.util.function Function)))

(defrecord ParT [type value dependents])

(def ^:private party-default (->ParT nil nil []))
(def party-empty (assoc party-default :type :s))
(def party-val (assoc party-default :type :v))
(def party-fut (assoc party-default :type :f))
(def party-par (assoc party-default :type :p, :value {:left nil, :right nil}))
(def party-join (assoc party-default :type :j))
(def party-futpar (assoc party-default :type :fp))
(def party-array (assoc party-default :type :m))

(def gettype :type)

(defn create
  [p v & vs]
  {:pre [(< (count vs) 2)]}
  (if (empty? vs)
    (assoc p :value v)
    (assoc p :value {:left v :right (first vs)})))

(defn create-f
  [p v]
  (create p (CompletableFuture/completedFuture v)))

(defn value
  [p]
  (get p :value nil))

(defn ^:private reify-sequence
  [p fun]
  (reify Function
    (apply [_ t]
      (fun t))))

(defn ^:private continuation
  [p fun]
  (let [v (value p)
        s (reify-sequence p fun)]
    (.thenApplyAsync v s)))

(defn update-party-v
  [p fun]
  (assoc p :value (continuation p fun) :dependents [(value p)]))

(defmulti update-party :type)
(defmethod update-party :s [p fun] p)
(defmethod update-party :v [p fun] (update-party-v p fun))
(defmethod update-party :f [p fun] (update-party-v p fun))
(defmethod update-party :p [p fun]
  (create party-par
          (update-party (get-in p [:value :left]) fun)
          (update-party (get-in p [:value :right]) fun)))

(defn ^:private extract-v
  "internal function for getting a value. blocks the main thread"
  [v]
  (some-> (value v) .get))

(defmulti extract :type)
(defmethod extract :s [_] nil)
(defmethod extract :v [p] [(extract-v p)])
(defmethod extract :f [p] [(extract-v p)])
(defmethod extract :p [p] (concat (extract (get-in p [:value :left]))
                                  (extract (get-in p [:value :right]))))

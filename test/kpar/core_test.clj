(ns kpar.core-test
  (:require [clojure.test :refer :all]
            [kpar.core :refer :all]
            [kpar.data :refer :all])
  (:import (java.util.concurrent CompletableFuture)))

(deftest test-spawn
  (let [task (spawn (inc 41))
        expected 42]
    (is (instance? CompletableFuture task))
    (is future? task)
    (is (= (.get task) expected))))

(deftest test-liftv
  (let [p (liftv 42)]
    (is (= (gettype-kd p) :v))
    (is future? (getvalue-kd p))))

(deftest test-liftf
  (let [p (liftf (spawn (inc 41)))]
    (is (= (gettype-kd p) :f))
    (is future? (getvalue-kd p))))

(deftest test-|
  (let [expected #{35 33 57 56}
        mid-30-crisis (| (liftf (spawn (inc 34))) (liftv (inc 32)))
        mid-50-crisis (| (liftv (inc 56)) (liftf (spawn (inc 55)))) 
        crisis (| mid-30-crisis mid-50-crisis)]
    (is (every? expected (extract crisis)))))


(deftest test->>
  (let [p (| (liftv (inc 42))
             (liftf (spawn (inc 11))))
        p1 (>> p #(inc %))
        p2 (>> p1 inc)
        expected1 '(44 13)
        expected2 (map inc '(44 13))]
    (is (= (extract p1) expected1))
    (is (= (extract p2) expected2))))

(deftest test-extract
    (let [p (| (liftv (inc 42))
               (liftf (spawn (inc 11))))
          p1 (>> p inc)
          p2 (>> p1 inc)
          expected1 '(44 13)
          expected2 (map inc '(44 13))]
    (is (= (extract p1) expected1))
    (is (= (extract p2) expected2))))

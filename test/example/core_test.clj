(ns example.core-test
  "Tests for example.core.  Intentionally leaves divide and factorial
  uncovered to produce an interesting Cobertura XML report."
  (:require [clojure.test :refer [deftest is testing]]
            [example.core :refer [add subtract multiply divide abs factorial fizzbuzz]]))

(deftest test-add
  (testing "basic addition"
    (is (= 4  (add 2 2)))
    (is (= 0  (add -1 1)))
    (is (= -3 (add -1 -2)))))

(deftest test-subtract
  (testing "basic subtraction"
    (is (= 1  (subtract 3 2)))
    (is (= -1 (subtract 2 3)))))

(deftest test-multiply
  (testing "basic multiplication"
    (is (= 6   (multiply 2 3)))
    (is (= 0   (multiply 0 100)))
    (is (= -12 (multiply -3 4)))))

;; divide is intentionally NOT tested here — it will show as uncovered.

(deftest test-abs
  (testing "positive numbers are unchanged"
    (is (= 5 (abs 5))))
  (testing "negative numbers are negated"
    (is (= 5 (abs -5))))
  (testing "zero stays zero"
    (is (= 0 (abs 0)))))

;; factorial is intentionally NOT tested here — it will show as uncovered.

(deftest test-fizzbuzz
  (testing "multiples of 15"
    (is (= "FizzBuzz" (fizzbuzz 15)))
    (is (= "FizzBuzz" (fizzbuzz 30))))
  (testing "multiples of 3 only"
    (is (= "Fizz" (fizzbuzz 3)))
    (is (= "Fizz" (fizzbuzz 9))))
  (testing "multiples of 5 only"
    (is (= "Buzz" (fizzbuzz 5)))
    (is (= "Buzz" (fizzbuzz 20))))
  (testing "other numbers"
    (is (= "1"  (fizzbuzz 1)))
    (is (= "7"  (fizzbuzz 7)))))

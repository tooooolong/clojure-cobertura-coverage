(ns example.core
  "Example namespace demonstrating cloverage + Cobertura XML coverage reporting.")

(defn add
  "Returns the sum of a and b."
  [a b]
  (+ a b))

(defn subtract
  "Returns a minus b."
  [a b]
  (- a b))

(defn multiply
  "Returns the product of a and b."
  [a b]
  (* a b))

(defn divide
  "Returns a divided by b. Throws ArithmeticException when b is zero."
  [a b]
  (if (zero? b)
    (throw (ArithmeticException. "Division by zero"))
    (/ a b)))

(defn abs
  "Returns the absolute value of n."
  [n]
  (if (neg? n) (- n) n))

(defn factorial
  "Returns n! for non-negative integer n."
  [n]
  (if (<= n 1)
    1
    (* n (factorial (dec n)))))

(defn fizzbuzz
  "Classic FizzBuzz: returns 'FizzBuzz', 'Fizz', 'Buzz', or the number as a string."
  [n]
  (cond
    (zero? (mod n 15)) "FizzBuzz"
    (zero? (mod n 3))  "Fizz"
    (zero? (mod n 5))  "Buzz"
    :else              (str n)))

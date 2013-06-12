(ns jest.visualize.points-test
  (:use jest.testutils
        midje.sweet)
  (:require [jest.visualize.points :as points]))

(facts "Stroke"
  (fact "Stroke-type is simple"
    (:stroke-type (meta (points/stroke [0] [3]))) => :simple)
  (fact "Stroke creates a 1-arity fn"
    (let [l (points/stroke [2] [4])]
      l => fn?
      (l) => (throws clojure.lang.ArityException)
      (l nil) =not=> (throws clojure.lang.ArityException)
      (l nil nil) => (throws clojure.lang.ArityException)))

  (fact "strokes-connected?"
    (points/strokes-connected? []) => falsey
    (points/strokes-connected? [(points/stroke [0 0]
                                               [1 1])
                                (points/stroke [1 1]
                                               [2 0])]) => truthy
    (points/strokes-connected? [(points/stroke [0 0]
                                               [1 0])
                                (points/stroke [1 1]
                                               [2 0])]) => falsey)
  
  (fact "Stroke basic functionality"
    (points/point (points/stroke [3] [7]) 0.5) => (vec-roughly [5] 0)
    (points/point (points/stroke [3 4] [7 0]) 0.25) => (vec-roughly [4 3] 0)
    (points/point (points/stroke [1 2 3 4 5 6] [4 5 6 7 8 9]) 0.5)
      => (vec-roughly [2.5 3.5 4.5 5.5 6.5 7.5] 0)

    ;; begin
      (points/point (points/stroke [3] [7]) 0) => (vec-roughly [3] 0)

    ;; end
      (points/point (points/stroke [3] [7]) 1) => (vec-roughly [7] 0))
  
    (fact "From point a to point a is not a stroke."
      ((points/stroke [1.1] [1.1]) 0.9) => (throws java.lang.AssertionError)))

(fact "Start & end points work as expected"
  (let [l (points/stroke [4 2]
                       [2 3])]
    (points/start-point l) => (vec-roughly [4 2] 0)
    (points/end-point l) (vec-roughly [2 3] 0)))

(fact "Length returns the length of a stroke is the euclidean distance between it's ends."
  (points/length (points/stroke [0 0 0]
                              [0 4 0])) => (roughly 4 0))

(facts "Tangent test."
  (fact "Tangent should be the same on a stroke."
    (let [l (points/stroke [0 0]
                         [0 10])]
      (points/tangent l 0.2453 [0 1])
        => (roughly (points/tangent l 0.2587 [0 1]) 0.01) 1))
  (fact "try to trick atan?"
    (let [l (points/stroke [0 0]
                           [-1 0])]
      (points/tangent l 0.2453 [0 1])
      => (roughly-angle Math/PI 1)))
  (fact "Some example corners"
    (let [l0 (points/stroke [0 0]
                          [0 1])
          l1 (points/stroke [0 0]
                          [0 -1])
          l2 (points/stroke [3 3]
                          [4 4])]
      (points/tangent l0 0.2 [0 1]) => (roughly (* Math/PI 0.5) 0.1)
      (points/tangent l1 0.2 [0 1]) => (roughly-angle (* Math/PI 1.5) 1)
      (points/tangent l2 0.2 [0 1]) => (roughly (* Math/PI 0.25) 0.1))))

(facts "Indexing sub-strokes by progress intervals."
  (let [s0 (points/stroke [0 0]
                          [0 1])
        s1 (points/stroke [0 1]
                          [1 1])
        s2 (points/stroke [1 1]
                          [1 2])
        s3 (points/stroke [1 2]
                          [2 2])
        ss' [s0 s1 s2 s3]
        c (points/index-sub-strokes ss')]
    (fact "Check intervals"
      (set (keys c)) => #{[0 0.25]
                          [0.25 0.5]
                          [0.5 0.75]
                          [0.75 1.0]})
    (fact "checking offset"
      (doall
       (map (fn [[[s _] {offset :offset}]]
              s => offset)
            c)))
    (fact "Check lenghts"
          (set (map :progress (vals c))) => #{0.25})
    (facts "In the right order"
           (:stroke (c [0 0.25])) => (fn [actual] (= actual s0))
           (:stroke (c [0.25 0.5])) => (fn [actual] (= actual s1))
           (:stroke (c [0.5 0.75])) => (fn [actual] (= actual s2))
           (:stroke (c [0.75 1.0])) => (fn [actual] (= actual s3)))))

(fact "Strokes work as a single stroke."
  (fact "Check at some key and random points"
    (let [lines (points/stroke-comp [(points/stroke [0 0]
                                                    [1 1])
                                     (points/stroke [1 1]
                                                    [2 0])])]
      (points/point lines 0) => (vec-roughly [0 0] 0)
      (points/point lines 0.25) => (vec-roughly [0.5 0.5] 0)
      (points/point lines 0.5) => (vec-roughly [1.0 1.0] 0)
      (points/point lines 0) => (vec-roughly [0 0] 0)
      (points/point lines 1) => (vec-roughly [2.0 0.0] 0)))
  (let [lines0 (points/stroke-comp [(points/stroke [0 0]
                                                   [1 1])
                                    (points/stroke [1 1]
                                                   [2 0])])
        lines1 (points/stroke-comp [(points/stroke [0 0]
                                                   [1 1])
                                    (points/stroke [1 1]
                                                   [2 0])
                                    (points/stroke-comp [(points/stroke [2 0]
                                                                        [3 1])
                                                         (points/stroke [3 1]
                                                                        [4 0])])])]
    (fact "Tangent asks the tangent of the correct sub-stroke."
      (points/tangent lines0 0 [0 1]) => (roughly-angle (* Math/PI 0.25) 0)
      (points/tangent lines0 0.5 [0 1]) => (roughly-angle (* Math/PI 0.25) 0)
          )
    (fact "Length is the sum of the length of it's parts."
      (points/length lines0) => (* 2 (Math/sqrt 2))
      (points/length lines1) => (* 4 (Math/sqrt 2)))))
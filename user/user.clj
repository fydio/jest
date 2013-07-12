;(set! *warn-on-reflection* true)

(ns user
  (:require [quil.core :as quil])
  (:use clojure.repl
        clojure.pprint
        jest.world.cell
        jest.visualize.resource
        [jest.visualize.visualize :exclude [min-borders]]
        jest.visualize.util
        jest.world
        jest.world.building
        jest.movement
        jest.util
        jest.world.path
        jest.level
        jest.input.highlight
        jest.color
        jest.score

        brick.drawable
        brick.image

        jest.testutils
        jest.scheduler

        jest.input.quil
        jest.input.wm-touch
        jest.input.core
        jest.input.interaction
        jest.vehicle
        jest.world.route)
  (:require [jest.input.editor :refer [disable-editor edit]]))

(defn build-level []
  (initialize-world 20 14)
  (build-spawn (cell [1 1]) :truck)
  (build-path (cell [1 1]) :south :road)
  (build-supply (cell [1 2]) :blue)
  (build-path (cell [1 2]) :south :road)
  (build-path (cell [1 3]) :east :road)
  (build-spawn (cell [2 1]) :truck)
  (build-path (cell [2 1]) :south :road)
  (build-supply (cell [2 2]) :red)
  (build-path (cell [2 2]) :south :road)
  (build-path (cell [2 3]) :east :road)

  (build-depot (cell [4 3]) :red 1000)
  (build-depot (cell [4 4]) :blue 1000)

  (build-spawn (cell [5 2]) :truck))

(def levels
  {:flurp
   [(fn level1 []
       (initialize-world 21 17)
       (enable-spawner (build-spawn (cell [3 6]) :truck) 0 1000)
       (dotimes [i 4]
         (build-path (cell [4 (+ 6 i)]) :south :road))
       (dotimes [i 3]
         (build-path (cell [(- 4 i) 10]) :west :road))
       (dotimes [i 4]
         (build-path (cell [1 (- 10 i)]) :north :road))
       (dotimes [i 3]
         (build-path (cell [(+ 1 i) 6]) :east :road))
       (build-path (cell [14 8]) :east :road)
       (build-spawn (cell [15 8]) :truck)
       (build-path (cell [6 8]) :east :road)
       (build-supply (cell [7 8]) :red)
       (build-path (cell [7 8]) :east :road)
       (build-path (cell [10 8]) :east :road)
       (build-depot (cell [11 8]) :red 10)
       (build-path (cell [11 8]) :east :road)
       )
    (fn level2 []
      (initialize-world 21 17)
      (enable-spawner (build-spawn (cell [3 6]) :truck) 0 4000)
      (enable-spawner (build-spawn (cell [3 10]) :truck) 2000 4000)
      (dotimes [i 4]
        (build-path (cell [(+ 3 i) 6]) :east :road)
        (build-path (cell [(+ 3 i) 10]) :east :road))
      (dotimes [i 4]
        (build-path (cell [(+ 10 i) 6]) :east :road)
        (build-path (cell [(+ 10 i) 10]) :east :road))
      
      (dotimes [i 2]
        (build-path (cell [7 (+ 6 i)]) :south :road)
        (build-path (cell [7 (- 10 i)]) :north :road)
        (build-path (cell [10 (+ 8 i)]) :south :road)
        (build-path (cell [10 (- 8 i)]) :north :road)
        )
      (dotimes [i 3]
        (build-path (cell [(+ 7 i) 8]) :east :road))

      (build-supply (cell [4 6]) :red)
      (build-supply (cell [4 10]) :blue)
      (build-depot (cell [13 6]) :blue 10)
      (build-depot (cell [13 10]) :red 10)

      (build-spawn (cell [14 6]) :truck)
      (build-spawn (cell [14 10]) :truck)
      )
    (fn level3 []
      (initialize-world 21 17)
      (jest.world.path/build-path (jest.world/cell [4 3]) :east :road)
      (jest.world.path/build-path (jest.world/cell [7 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [8 7]) :south :road)
      (jest.world.path/build-path (jest.world/cell [9 8]) :east :road)
      (jest.world.path/build-path (jest.world/cell [10 9]) :south :road)
      (jest.world.path/build-path (jest.world/cell [11 10]) :east :road)
      (jest.world.path/build-path (jest.world/cell [4 4]) :north :road)
      (jest.world.path/build-path (jest.world/cell [6 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [8 8]) :east :road)
      (jest.world.path/build-path (jest.world/cell [10 10]) :east :road)
      (jest.world.path/build-path (jest.world/cell [4 5]) :north :road)
      (jest.world.path/build-path (jest.world/cell [5 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [8 9]) :north :road)
      (jest.world.path/build-path (jest.world/cell [4 6]) :north :road)
      (jest.world.path/build-path (jest.world/cell [8 10]) :north :road)
      (jest.world.path/build-path (jest.world/cell [8 10]) :west :road)
      (jest.world.path/build-path (jest.world/cell [7 10]) :west :road)
      (jest.world.path/build-path (jest.world/cell [8 11]) :north :road)
      (jest.world.path/build-path (jest.world/cell [6 10]) :west :road)
      (jest.world.path/build-path (jest.world/cell [8 12]) :north :road)
      (jest.world.path/build-path (jest.world/cell [5 10]) :west :road)
      (jest.world.path/build-path (jest.world/cell [8 13]) :north :road)
      (jest.world.path/build-path (jest.world/cell [4 10]) :south :road)
      (jest.world.path/build-path (jest.world/cell [7 13]) :east :road)
      (jest.world.path/build-path (jest.world/cell [4 11]) :south :road)
      (jest.world.path/build-path (jest.world/cell [6 13]) :east :road)
      (jest.world.path/build-path (jest.world/cell [4 12]) :south :road)
      (jest.world.path/build-path (jest.world/cell [5 13]) :east :road)
      (jest.world.path/build-path (jest.world/cell [4 13]) :east :road)
      (jest.world.path/build-path (jest.world/cell [14 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [13 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [14 7]) :north :road)
      (jest.world.path/build-path (jest.world/cell [12 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [14 8]) :north :road)
      (jest.world.path/build-path (jest.world/cell [8 3]) :south :road)
      (jest.world.path/build-path (jest.world/cell [11 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [14 9]) :north :road)
      (jest.world.path/build-path (jest.world/cell [7 3]) :east :road)
      (jest.world.path/build-path (jest.world/cell [8 4]) :south :road)
      (jest.world.path/build-path (jest.world/cell [10 6]) :south :road)
      (jest.world.path/build-path (jest.world/cell [14 10]) :north :road)
      (jest.world.path/build-path (jest.world/cell [6 3]) :east :road)
      (jest.world.path/build-path (jest.world/cell [8 5]) :south :road)
      (jest.world.path/build-path (jest.world/cell [10 7]) :south :road)
      (jest.world.path/build-path (jest.world/cell [11 8]) :east :road)
      (jest.world.path/build-path (jest.world/cell [13 10]) :east :road)
      (jest.world.path/build-path (jest.world/cell [5 3]) :east :road)
      (jest.world.path/build-path (jest.world/cell [8 6]) :south :road)
      (jest.world.path/build-path (jest.world/cell [8 6]) :west :road)
      (jest.world.path/build-path (jest.world/cell [10 8]) :south :road)
      (jest.world.path/build-path (jest.world/cell [12 10]) :east :road)
      )]

   :tutorial
   [(fn []
      (initialize-world 9 4)
      (build-spawn (cell [1 1]) :truck)
      (build-spawn (cell [1 3]) :truck)
      (build-spawn (cell [8 1]) :truck)
      (build-spawn (cell [8 3]) :truck)
      
      (build-supply (cell [3 1]) :blue)
      (build-supply (cell [3 3]) :red)
      
      (build-depot (cell [6 1]) :blue 1000)
      (build-depot (cell [6 3]) :red 1000)

      (dotimes [i 6]
        (build-path (cell [(+ 1 i) 1]) :east :road)
        (build-path (cell [(+ 1 i) 3]) :east :road)))
    
    (fn []
      (initialize-world 9 4)
      (build-spawn (cell [1 1]) :truck)
      (build-spawn (cell [1 3]) :truck)
      (build-spawn (cell [8 1]) :truck)
      (build-spawn (cell [8 3]) :truck)
      
      (build-supply (cell [3 1]) :blue)
      (build-supply (cell [3 3]) :red)
      
      (build-depot (cell [6 1]) :red 1000)
      (build-depot (cell [6 3]) :blue 1000)

      (build-path (cell [1 1]) :east :road)
      (build-path (cell [2 1]) :east :road)
      (build-path (cell [3 1]) :east :road)
      (build-path (cell [4 1]) :south :road)
      (build-path (cell [4 2]) :east :road)
      (build-path (cell [5 2]) :north :road)
      (build-path (cell [5 2]) :south :road)
      
      (build-path (cell [1 3]) :north :road)
      (build-path (cell [1 2]) :east :road)
      (build-path (cell [2 2]) :south :road)
      (build-path (cell [2 3]) :east :road)
      (build-path (cell [3 3]) :east :road)
      (build-path (cell [4 3]) :north :road)

      (dotimes [i 3]
        (build-path (cell [(+ 5 i) 1]) :east :road)
        (build-path (cell [(+ 5 i) 3]) :east :road))

      )
    (fn []
      (initialize-world 22 18)
      (let [spawns [[1 1]
                    [3 2]
                    [2 3]
                    [4 4]]]
        (doseq [spawn spawns]
          (build-spawn (cell spawn) :truck))
        (doseq [spawn spawns
                dir [:north :east :south :west]]
          (build-path (cell spawn) dir :road)))
      
      (build-supply (cell [2 1]) :red)
      (build-supply (cell [3 4]) :green)
      
      (build-depot (cell [3 3]) :red 1000)
      (build-depot (cell [2 2]) :green 1000)
      )
    ]})

(defn build-another-level []
  (initialize-world 8 8)
  (enable-spawner (build-spawn (cell [1 1]) :truck) 0 3000)
  (enable-spawner (build-spawn (cell [2 1]) :truck) 1000 3000)
  (enable-spawner (build-spawn (cell [3 1]) :truck) 2000 3000)
  (build-supply (cell [1 2]) :red)
  (build-supply (cell [2 2]) :green)
  (build-mixer (cell [3 3]))
  (build-spawn (cell [4 3]) :truck)
  (build-depot (cell [3 4]) :yellow 1000)
  (build-spawn (cell [3 5]) :truck)

  (build-path (cell [1 1]) :south :road)
  (build-path (cell [2 1]) :south :road)
  (build-path (cell [3 1]) :south :road)
  (build-path (cell [1 2]) :south :road)
  (build-path (cell [2 2]) :south :road)
  (build-path (cell [3 2]) :south :road)
  (build-path (cell [1 3]) :east :road)
  (build-path (cell [2 3]) :east :road)
  (build-path (cell [3 3]) :east :road)
  (build-path (cell [3 3]) :south :road)
  (build-path (cell [3 4]) :south :road))

(defn graceful-exit []
  (if-let [world-bricklet @world-bricklet]
    (let [queue (:command-queue world-bricklet)]
      (swap! queue conj (fn [_] (quil/exit)) ))))

(defn common-setup []
  (graceful-exit)
  (scheduler-reset!)
  (reset-score)
  (interaction-setup)
  (load-level "levels/alpha_ugly.json")
  ((get-in levels [:tutorial 2]))

;  (build-spawn (cell [4 2]) :truck)
  (start!)
  (pause!))

(defn user-setup []
  (setup-quil-mouse-input)
  (common-setup)
  (sketch!))

(defn user-setup-touch []
  (common-setup)
  (sketch!)
  (undecorate-sketch @world-sketch)
  (ensure-wm-touch-input-setup!))

(defn path-definition-for [p]
  `(build-path (cell ~(:coords p)) ~(:direction p) ~(:type p)))

(defn extract-path-definitions []
  (map path-definition-for (flatten (map out-paths (all-cells)))))

(defn print-path-definitions []
  (doseq [pd (extract-path-definitions)]
    (println pd)))


(defmacro at-pointer-cell [[c] & body]
  `(let [~c (cell (first (vals (all-pointers))))]
     ~@body))

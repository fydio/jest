(ns jest.visualize.visualize
  "Functions to facilitate the visualisation of the world state."
  (:use jest.util)
  (:use [clojure.core.match :only [match]])
  (:require [brick.image :as image]
            [brick.drawable :as drawable]
            [quil.core :as quil])
  (:require [jest.world :as world]
            [jest.movement :as movement]
            [jest.world.building :as building]
            [jest.vehicle :as vehicle
             :refer [vehicle-cell]]
            [jest.world.path :as path]
            [jest.world.cell :as cell])
  (:require [jest.visualize.points :as points]
            [jest.visualize.util :as util
             :refer [cell-points absolute->relative]]
            [jest.visualize.resource :as resource]
            [jest.color :as color])
  (:require [jest.visualize.input :as input]))

(declare cell-bg)
(declare cell-building)
(declare cell-road)

(def min-borders [0.1 0.1])

(declare sketch-size)

(defn replace-fn [p v]
  (fn [x]
    (if (p x) v x)))

(defn arrow [cx cy angle colors]
  (let [amount (count colors)]
    (drawable/->Floating
     (reify drawable/Drawable
       (draw [this [w h]]
         (quil/with-translation [(* w 5/8)
                                 (/ h 2)]
           (quil/push-style)
           (quil/stroke 255)
           ;;(quil/smooth)
           (let [len (/ w 4)
                 fat (/ h 12)]
             (quil/stroke-weight (/ len 12))
             (quil/line 0 0 len 0)
             (quil/line len 0 (- len fat) (quil/ceil (- fat)))
             (quil/line len 0 (- len fat) (quil/ceil fat))
             (quil/color-mode :hsb)
             (loop [ls (drawable/ranges (max amount fat) len)
                    cs colors]
               (if (and (seq ls) (seq cs))
                 (let [[offset l] (first ls)
                       endp (+ offset l)
                       c (first cs)]
                   (apply quil/stroke (color/hue->hsb c))
                   (quil/line offset 0 (- endp fat) (quil/ceil (- fat)))
                   (quil/line offset 0 (- endp fat) (quil/ceil fat))
                   (recur (rest ls)
                          (rest cs)))))
             (quil/color-mode :rgb))

           (quil/pop-style))))
     [cx cy] 1 angle)))

(def direction-order {:north 1 ;;first
                        :west 2
                        :south 3
                        :east 4})

(defn match-roads [roads]
  (let [sorted-roads (sort-by (comp direction-order :direction) roads)
        road-tuples (map (juxt :direction :inout) sorted-roads)]
    (match (vec road-tuples)
           [[:north :out]] :road-n
           [[:west :out]] :road-w
           [[:south :out]] :road-s
           [[:east :out]] :road-e
           [[:north :in]] :road-end-s
           [[:west :in]] :road-end-e
           [[:south :in]] :road-end-n
           [[:east :in]] :road-end-w

           [[_ :in] [_ :in]] :road-blocked
           ;; double
           [[:north :in] [:west :out]] :turn-nw
           [[:north :in] [:south :out]] :road-s
           [[:north :in] [:east :out]] :turn-ne

           [[:north :out] [:west :in]] :turn-nw
           [[:north :out] [:south :in]] :road-n
           [[:north :out] [:east :in]] :turn-ne

           [[:west :in] [:south :out]] :turn-sw
           [[:west :in] [:east :out]] :road-e

           [[:west :out] [:south :in]] :turn-sw
           [[:west :out] [:east :in]] :road-w

           [[:south :in] [:east :out]] :turn-se

           [[:south :out] [:east :in]] :turn-se

           ;; triple
           [[:north _] [:west _] [:south _]] :cross-t-e
           [[:north _] [:south _] [:east _]] :cross-t-w
           [[:north _] [:west _] [:east _]] :cross-t-s
           [[:west _] [:south _] [:east _]] :cross-t-n

           ;; quatro
           [[:north _] [:west _] [:south _] [:east _]] :cross
           :else nil)))

(let [cardinal-arrow (memoize (partial arrow 0.5 0.5))
      arrow-stack (memoize (fn [dirs routes]
                             (drawable/->Stack
                              (vec (map cardinal-arrow dirs routes)))))]
  (defn paths-to-arrows [c]
    (let [*pi (partial * Math/PI)
          dir-to-radian  {:north (*pi 3/2)
                          :east (*pi 0)
                          :south (*pi 1/2)
                          :west (*pi 1)}
          out-p (path/out-paths c)]
      (arrow-stack (map dir-to-radian (map :direction out-p))
                   (map :routes out-p)))))

(defn nice-lookup []
  (let [loader (comp
                drawable/->Image
                image/path->PImage
                clojure.java.io/resource
                (partial str "junction/road/"))]
    (let [rn (loader "road-n.png")
          rw (loader "road-w.png")
          rs (loader "road-s.png")
          re (loader "road-e.png")
          rb (drawable/->Nothing)

          ren (loader "road-end-n.png")
          rew (loader "road-end-w.png")
          res (loader "road-end-s.png")
          ree (loader "road-end-e.png")

          tnw (loader "turn-nw.png")
          tne (loader "turn-ne.png")
          tse (loader "turn-se.png")
          tsw (loader "turn-sw.png")

                ctn (loader "cross-t-n.png")
          ctw (loader "cross-t-w.png")
          cts (loader "cross-t-s.png")
          cte (loader "cross-t-e.png")

          cross (loader "cross.png")
          junctions
          {:road-n rn
           :road-w rw
           :road-s rs
           :road-e re
           :road-blocked rb
           :road-end-n ren
           :road-end-w rew
           :road-end-s res
           :road-end-e ree
           :turn-nw tnw
           :turn-ne tne
           :turn-se tse
           :turn-sw tsw
           :cross-t-n ctn
           :cross-t-w ctw
           :cross-t-s cts
           :cross-t-e cte
           :cross cross}]
      (fn [c]
        (let [roads (path/paths c :road)
              n (drawable/->Nothing)]
           (get junctions (match-roads roads) n))))))

(defonce world-bricklet (atom nil))
(defonce world-sketch (atom nil))



(defn world-state->Grid
  "Builds a layer from the world state.
cell-draw-fn is a function that returns a Drawable."
  [cell-draw-fn]
;  {:post [(every? drawable/drawable? (vals (:grid %)))]}
  (apply drawable/->SquareTiledGrid
   (world/world-width)
   (world/world-height)
   (into {}
         (doall
          (for [c (world/all-cells)]
            [(world/coords c) (cell-draw-fn c)])))
   min-borders))


(defrecord Rect
  [color]
  drawable/Drawable
  (draw [this [w h]]
    (quil/color-mode :hsb)
    (apply quil/fill (:color this))
    (quil/rect
     (* 0 w)
     (* 0.2 h)
     (* 0.4 w)
     (* 0.4 h))
    (quil/color-mode :rgb)))

(defn moving-vehicle->location
  [v]
  (let [stroke (util/vehicle->stroke v [(quil/width)
                                        (quil/height)])
        p (util/vehicle->progress v)
        [x y] (points/point
               stroke p)]
    {:position [(/ x (quil/width))
          (/ y (quil/height))]
     :rotation (points/tangent stroke p [0 1])}))

(defn vehicle-animation [location-fn]
  (fn [v image]
    (let [{:keys [position rotation]} (location-fn v)]
      (drawable/->Stack [(drawable/->Floating image
                                              position
                                              (util/vehicle-scale)
                                              rotation)
                         (drawable/->Floating (resource/drawable-from-resource-rate
                                               (resource/vehicle-resource-rate v))
                                              position
                                              (util/vehicle-scale)
                                              0)]))))

(def moving-vehicle (vehicle-animation moving-vehicle->location))

(defn vehicle-center [v]
  (absolute->relative (:center (cell-points (vehicle-cell v)))))

(def lols
  {:north (* 1.5 Math/PI)
   :south (* 0.5 Math/PI)
   :west Math/PI
   :east 0.0})

(defn spawning-vehicle->location
  [v]
  (let [progress (util/vehicle->progress v)
        stroke (util/vehicle->stroke v [(quil/width) (quil/height)])]
    (if (< progress 0.5)
      {:position (vehicle-center v)
       :rotation (+ (lols (:exit-direction v))
                    (* 4 Math/PI progress))}
      {:position (absolute->relative
                  (points/point stroke (* 2 (- progress 0.5))))
       :rotation (lols (:exit-direction v))})))

(def spawning-vehicle (vehicle-animation spawning-vehicle->location))

(defn vehicles->Stack
  [vehicle-type image]
  (apply drawable/->Border
         (drawable/->Stack
          (vec
           (map (fn [v]
                  (cond
                   (vehicle/moving? v) (moving-vehicle v image)
                   (vehicle/spawning? v) (spawning-vehicle v image)
                   (vehicle/despawning? v) (drawable/->Nothing)
                   (vehicle/exploding? v) (drawable/->Nothing)))
                (vehicle/all-vehicles vehicle/truck?))))
             (drawable/square-borders-size
              (sketch-size)
              (world/world-size)
              min-borders)))

(defn world->drawable
  [tile-fn path-fn]
  (drawable/->Stack
   [
    (world-state->Grid (comp tile-fn (constantly :grass)))
    (world-state->Grid path-fn)
    (vehicles->Stack :truck (tile-fn :truck))
    (world-state->Grid paths-to-arrows)
    (world-state->Grid (partial cell-building tile-fn))
    ]))

(defn cell-bg [c]
  "What is the background tile-key for this cell?"
  (:background c))

(defn cell-building
  "Which building tile-key fits this cell?"
  [tile-fn c]
  (if-let [type (building/building-type c)]
    (let [resource-vis (comp resource/drawable-from-resource-rate
                             resource/building-resource-rate)]
      (case type
        :spawn (tile-fn
                (hyphenate-keywords :spawn (building/vehicle-type c)))
        :mixer (drawable/->Stack [(tile-fn :mixer)
                                  (resource-vis c)])
        :supply (drawable/->Stack [(tile-fn :supply-red)
                                   (resource-vis c)])
        :depot (drawable/->Stack [(tile-fn :dirt)
                                  (resource-vis c)])))
    (tile-fn nil)))

(defn cell-road
  "Return the appropriate tile-key for roads in this cell."
  [c]
  (let [{in :in
         out :out} (group-seq
                    (path/paths c :road)
                    {:in path/in-path?
                     :out path/out-path?})]
    ;; actual
    (apply (partial hyphenate-keywords :road)
           (map :direction out))
    ;; temp
    (if-not (empty? (first out))
      (hyphenate-keywords :road (:direction (first out))))))

(defn setup [tile-fn]
  ;init een bricklet met tile-set
  (let [path-fn (nice-lookup)]
    (reset! world-bricklet
            (drawable/->Bricklet
             (atom
              (reify drawable/Drawable
                (draw [this [w h]]
                  (drawable/.draw
                   (world->drawable
                    tile-fn
                    path-fn)
                   [w h]))))
             (atom [])
             :decor false
             :size [800 600]
             :renderer :java2d
             :init (fn [_] (quil/frame-rate 60))
             :mouse-pressed input/on-down-handler
             :mouse-released input/on-up-handler
             :mouse-dragged input/on-move-handler))))

(defn sketch! []
  (reset! world-sketch (drawable/drawable->sketch! @world-bricklet)))

(defn sketch-width []
  (.getWidth @world-sketch))

(defn sketch-height []
  (.getHeight @world-sketch))

(defn sketch-size []
  [(sketch-width) (sketch-height)])

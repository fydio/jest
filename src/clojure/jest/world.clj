(ns jest.world
  "Public interface for the world state."
  (:use [jest.util :only [maybe-deref]]))

(defonce
  #^{:private true
     :doc "Binding containing the world state as an atom containing a
          map from [x y] coordinates to cells."}
  world
  (atom {}))

(defonce ^{:private true
           :doc "Atom holding the world size."} world-size' (atom {}))

(let [max-wrapper
      (fn [& args]
        (if (seq args)
          (apply max args)
          -1))]
  (defn- calculate-world-width
    "Calculates the width of the loaded world."
    []
    (inc
     (apply max-wrapper (map first (keys @world)))))

  (defn- calculate-world-height
    "Calculates the height of the loaded world."
    []
    (inc
     (apply max-wrapper (map second (keys @world))))))

(defn reset-world
  "Forcefully resets the world to value v."
  [v]
  (reset! world v)
  (reset! world-size' [(calculate-world-width) (calculate-world-height)]))

(defn world-width
  "Returns the width of the loaded world."
  []
  (first @world-size'))

(defn world-height
  "Returns the height of the loaded world."
  []
  (second @world-size'))

(defn world-size
  "Returns the size of the loaded world as a [width height] tuple."
  []
  [(world-width) (world-height)])

(defmacro with-temp-world
  "For testing purposes, rebind the world state to an empty map."
  [& body]
  `(with-redefs [world (atom {})]
     ~@body))

(defn- cell-ref
  "Returns the cell ref for the given coordinate"
  [[x y]]
  (@world [x y]))

(defn cell
  "Returns the cell on the given coordinates"
  [[x y]]
  {:pre [(cell-ref [x y])]}
  @(cell-ref [x y]))

(defn coords
  "Returns the coordinates of the given cell/ref"
  [c]
  (:coord (maybe-deref c)))

(def ^{:doc "A map of direction keywords to coord delta's"}
  directions
  {:north [0 -1]
   :south [0 1]
   :west [-1 0]
   :east [1 0]})

(defn- calculate-coord
  "Returns the [x y] coord of the location of a possible adjacent cell
  to c in direction dir"
  [cell dir]
  (vec (map + (coords cell) (dir directions))))

(defn direction
  "Returns cell in the given direction"
  [c dir]
  (cell (calculate-coord c dir)))

(defn direction-exists?
  "Returns whether the cell is connected in the given direction"
  [c dir]
  (boolean (cell-ref (calculate-coord c dir))))

(defn all-cells
  "Returns a list of all cells, optionally filtered by a predicate"
  ([]
     (map (comp deref second) @world))
  ([pred]
     (filter pred (all-cells))))

(defn alter-cell
  "Alters a cell through it's ref. Must be called within a transaction."
  ([c f & args]
     (apply alter (@world (coords c)) f args)))

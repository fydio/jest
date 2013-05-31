(ns jest.world.path
  "Functions for adding, removing and searching for roads, rails and canals."
  (:use jest.util
        jest.world.cell))

; Temporary convention:
; Routes are preferred paths for the specified colors, but can actually
; be used for the transportation of any resource. In other words, every path
; has an implied :any color.

(defrecord Path [type direction inout resources routes])

;; TODO
; Implementation of the following functions doesn't always take into
; account the possibility of different Path types

(letfn [(some-paths [c inout]
          (for [[dir path] (:paths c)
                :when (= inout (:inout path))]
            path))]
  (defn in-paths
    "Returns all incoming paths for the given cell"
    [c]
    (some-paths c :in))

  (defn out-paths
    "Returns all outgoing paths for the given cell"
    [c]
    (some-paths c :out)))

(defn complete-paths
  "Returns all in-out path pairs of this cell"
  [c]
  (for [in (in-paths c)
        out (out-paths c)
        :when (= (:type in) (:type out))]
    [in out]))

(defn path
  "Returns the path in the given direction, or nil if there is none."
  [c direction]
  (get-in c [:paths direction]))

(defn in-path?
  "Returns whether or not the given path is an in-path."
  [path]
  (= :in (:inout path)))

(defn out-path?
  "Returns whether or not the given path is an out-path."
  [path]
  (= :out (:inout path)))

(defn- update-path
  [c path]
  (assoc-in c [:paths (:direction path)] path))

(defn- add-path
  "Adds the given path to the cell"
  [c direction type inout]
  {:pre [(not (get-in c [:paths direction]))]}
  (update-path c (map->Path {:type type
                          :direction direction
                          :inout inout})))

(defn- remove-path
  "removes the path from the cell for the given direction"
  [c direction]
  {:pre [(get-in c [:paths direction])]}
  (update-in c [:paths]
             #(dissoc % direction)))



(def opposite-dirs
  {:north :south,
   :south :north,
   :west :east,
   :east :west})

(defn build-path
  "Alters world state by building a path from cell c to the given direction"
  [c dir type]
  {:pre [(direction c dir)]}
  (dosync
   (alter-cell c add-path dir type :out)
   (alter-cell (direction c dir) add-path (opposite-dirs dir) type :in)))

(defn unbuild-path
  "Alters world state by building a path from cell c to the given direction"
  [c dir]
  {:pre [(direction c dir)]}
  (dosync
   (alter-cell c remove-path dir)
   (alter-cell (direction c dir) remove-path (opposite-dirs dir))))

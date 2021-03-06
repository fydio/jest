(ns jest.world.route
  "Functions for adding, removing and searching for routes along
  roads, rails and canals."
  (:use [jest.world :only [alter-cell]]
        [jest.world.path :only [paths]]
        [jest.color :only [contains-hue? hue-matches?]]))

(defn- add-route
  "Adds a route for resources of the specified color to an
  existing path"
  [path color]
  {:pre [path
         (not (contains-hue? (:routes path) color))]}
  (assoc path :routes
         (set (conj (:routes path) color))))

(defn- remove-route
  "Removes any existing routes for the specified color from an
  existing path"
  [path color]
  {:pre [path
         (contains-hue? (:routes path) color)]}
  (update-in path [:routes]
             (fn [routes]
               (set (remove #(hue-matches? % color)
                            routes)))))


(defn build-route
  "Adds a colored route to an existing path in cell c"
  [c dir color]
  (dosync
   (alter-cell c #(update-in % [:paths dir] add-route color))))

(defn unbuild-route
  "unbuilds a colored route from an existing path in cell c"
  [c dir color]
  (dosync
   (alter-cell c #(update-in % [:paths dir] remove-route color))))

(defn all-routes
  "Returns tuples of [direction type routes] for a cell c."
  [c]
  (for [[d p] (:paths c)
        :when (seq (:routes p))]
    [d (:type p) (:routes p)]))

(defn paths-with-route
  "Returns all paths at the given cell that contain the given hue."
  [c color]
  (filter #(contains-hue? (:routes %) color)
          (paths c)))

(ns jest.world.building
  "Functions for adding, removing and searching for buildings."
  (:use [clojure.core.incubator :only [defmacro-]]
        [jest.util :only [plural]]
        [jest.world :only [alter-cell all-cells cell]]
        [jest.color :only [average-hue]]))

(defn building-type
  "Returns the building type for the given cell."
  [c]
  (:building-type c))

(defn unbuilt?
  "Returns true if nothing is built on this cell, false otherwise."
  [c]
  (not (building-type c)))

(defn all-cells-type
  "returns all cells in the currently bound world grid with the given building
   type"
  [type]
  (all-cells #(= (building-type %1) type)))

(defn add-building
  "adds a building to the given cell"
  [c type & other-fields]
  {:pre [(not (building-type c))]}
  (apply assoc c
         :building-type type
         other-fields))

(defn- remove-building
  "removes a building from the given cell"
  ([c & extra-fields]
     {:pre [(building-type c)]}
     (apply assoc c
            :building-type nil
            (interleave extra-fields (repeat (count extra-fields) nil)))))

(defmacro- defbuilding
  "creates building helper functions"
  [type & fields]
  (let [add (symbol (str "add-" type))
        remove (symbol (str "remove-" type))
        build (symbol (str "build-" type))
        unbuild (symbol (str "unbuild-" type))
        pred (symbol (str type "?"))
        all (symbol (str "all-" (plural (str type))))
        field-syms (map (comp symbol name) fields)
        field-args (interleave fields field-syms)]
    `(do
       (defn ~pred
         ~(format "returns whether or not this cell is of type %s."
                  type)
         ([~'c]
            (= (building-type ~'c) ~(keyword type)))
         ([~'x ~'y]
            (~pred (cell [~'x ~'y]))))

       (defn- ~add
         ~(format "adds a %s to the given cell." type)
         [~'c ~@field-syms]
         {:pre  [(unbuilt? ~'c)]
          :post [(~pred ~'%)]}
         (add-building ~'c ~(keyword type) ~@field-args))

       (defn ~build
         ~(format "Alters world state by building a %s to the given cell.\n"
                  type)
         [~'c ~@field-syms]
         (dosync (alter-cell ~'c ~add ~@field-syms)))

       (defn- ~remove
         ~(format "removes a %s from the given cell." type)
         [~'c]
         {:pre  [(~pred ~'c)]
          :post [(unbuilt? ~'%)]}
         (remove-building ~'c ~@fields))

       (defn ~unbuild
         ~(format "Alters world state by unbuilding a %s from the given cell."
                  type)
         [~'c]
         (dosync (alter-cell ~'c ~remove)))

       (defn ~all
         ~(format "returns all cells with building type %s." type)
         []
         (all-cells-type ~(keyword type))))))

(defbuilding spawn :vehicle-type)
(defbuilding supply :resource-type)
(defbuilding depot :resource-type :quotum)
(defbuilding mixer)
(defbuilding restricted)

(defn enable-spawner
  "Sets a spawn point to be able to spawn vehicles, rather than just consume them.
   When the spawn process is started, vehicles will spawn from the given cell with
   the given rate, starting at the given offset."
  [spawn offset rate]
  (dosync
   (alter-cell spawn assoc :spawning? true)
   (alter-cell spawn assoc :spawn-offset offset)
   (alter-cell spawn assoc :spawn-rate rate)))

(defn spawning-spawners
  "Returns all enabled spawners."
  []
  (filter :spawning? (all-spawns)))

(defn resource-type
  "Returns the type of resource handled on this supply or depot."
  [c]
  {:pre [(or (supply? c)
             (depot? c)
             (mixer? c))]}
  (:resource-type c))

(defn vehicle-type
  "Returns the vehicle type spawned at this spawn."
  [c]
  {:pre [(spawn? c)]}
  (:vehicle-type c))

;; functions for depots
(defn- dropoff-resource'
  "Returns a new cell with the given amount of cargo added to it. The cell should contain a depot."
  [cell amount]
  {:pre [(depot? cell)]}
  (update-in cell [:amount] (fnil + 0) amount))

(defn dropoff-resource
  "Adds the given amount of cargo to this cell. The cell should contain a depot."
  [cell amount]
  (alter-cell cell dropoff-resource' amount))

(defn all-depots-filled?
  "Returns true if all depots in the loaded world have reached full capacity, and returns false otherwise."
  []
  (every? #((fnil >= 0 0) (:amount %) (:quotum %)) (all-depots)))

;; functions for mixers
(defn resource-color
  "Returns the color of the resource at this cell."
  [cell]
  (let [resource-seq (seq (or (:resource cell) {}))
        color-list (mapcat #(repeat (second %) (first %))
                        resource-seq)]
    (if (seq color-list)
      (apply average-hue color-list))))

(defn resource-count
  "Returns the amount of resource at this cell."
  [cell]
  (apply + (vals (or (:resource cell) {}))))

(defn- mix-colors'
  "Given a cell, returns a cell with the given color mixed with existing color
   there. Magnitude represents how much color should be mixed."
  [cell color magnitude]
  (let [resource-map (or (:resource cell) {})]
    (assoc cell :resource
           (assoc resource-map color
                  (+ (or (resource-map color) 0) magnitude)))))

(defn mix-colors
  "Given a cell, alters this cell in the world state to mix the given color
   at the given magnitude with existing colors there."
  [cell color magnitude]
  (alter-cell cell mix-colors' color magnitude))

(defn- reduce-resource'
  "Given a cell, returns a cell with the resource amount reduced with the given
   amount."
  [cell amount]
  (let [cnt (resource-count cell)
        resource-seq (seq (or (:resource cell) {}))]
    (assoc cell :resource (if (= amount cnt)
                            {}
                            (let [ratio (/ (- cnt amount) cnt)]
                              (reduce conj {}
                                      (map (fn [[color amount]]
                                             [color (* amount ratio)])
                                           resource-seq)))))))

(defn reduce-resource
  "Given a cell, alters this cell in the world state to reduce the resource
   amount with the given amount."
  [cell amount]
  (alter-cell cell reduce-resource' amount))


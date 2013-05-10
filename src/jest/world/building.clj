(ns jest.world.building
  (:use jest.util
        jest.world.cell))

(defn- add-building
  "adds a building to the given cell"
  [c type]
  {:pre [(= (:type c) :normal)]}
  (assoc c :type type))

(defn- remove-building
  "removes a building from the given cell"
  ([c]
     {:pre [(not= (:type c) :normal)]}
     (assoc c :type :normal))
  ([c type]
     {:pre [(= (:type c) type)]}
     (remove-building c)))

(defmacro defbuilding [name]
  (let [add (symbol (str "add-" name))
        remove (symbol (str "remove-" name))
        build (symbol (str "build-" name))
        unbuild (symbol (str "unbuild-" name))
        pred (symbol (str name "?"))
        all (symbol (str "all-" name "s"))]
    `(do (defn- ~add
           ~(format "adds a %s to the given cell" name)
           [~'c]
           (add-building ~'c ~(keyword name)))
         (defn- ~remove
           ~(format "removes a %s from the given cell" name)
           [~'c]
           (remove-building ~'c ~(keyword name)))
         
         (defn- ~pred
           ~(format "returns whether or not this cell/refcell is of type %s" name)
           ([~'c]
              (= (:type (maybe-deref ~'c)) ~(keyword name)))
           ([~'x ~'y]
              (~pred (cell ~'x ~'y))))
         
         (defn ~all
           ~(format "returns all cells with building type %s" name)
           []
           (all-cells-type ~(keyword name)))
         
         (defn ~build
           ~(format "builds a %s to the given cell ref" name)
           ([~'c]
              (dosync (alter ~'c ~add)))
           ([~'x ~'y]
              (dosync (alter (cell ~'x ~'y) ~add))))
         
         (defn ~unbuild
           ~(format "unbuilds a %s from the given cell ref" name)
           ([~'c]
              (dosync (alter ~'c ~remove)))
           ([~'x ~'y]
              (~unbuild (cell ~'x ~'y)))))))

(defmacro buildings [& names]
  `(do ~@(map #(list 'defbuilding %) names)))

(buildings spawn
           supply
           depot
           mixer)

(ns jest.input.core
  "Transforms device-agnostic input from pixel-based to tile-based."
  (:use [jest.visualize.visualize :only [sketch-size]]
        [jest.world :only [world-size]]))

(def ^:private pointers (atom {}))
(defn pointer [id]
  (@pointers id))

(defn- on-down-handler [id p])
(defn- on-up-handler [id p])
(defn- on-move-handler [id p1 p2])

(def handlers {:on-down #'on-down-handler
               :on-up #'on-up-handler
               :on-move #'on-move-handler})

(defn set-input-handler! [on fn]
  (alter-var-root (handlers on) (constantly fn)))

(def tl [0.0 0.0])
(def br [1.0 1.0])

(defn pixel->tile [x y]
  (let [tl (map * tl (sketch-size))
        br (map * br (sketch-size))
        map-size (map - br tl)
        cell-size (map / map-size (world-size))
        ppos (map - [x y] tl)
        tpos (map (comp int /) ppos cell-size)
        tpos (map min tpos (map dec (world-size)))]
    tpos))

(defmacro with-tile [[t c] & body]
  `(let [~t (apply pixel->tile ~c)]
     ~@body))

(defn receive-down [id p]
  (with-tile [t p]
    (swap! pointers assoc id t)
    (on-down-handler id t)))

(defn receive-up [id]
  (on-up-handler id (@pointers id))
  (swap! pointers dissoc id))

(defn step [a b]
  (cond (= a b) 0
        (neg? (- b a)) -1
        :default 1))

;;dumbly interpolates by first moving to the right x, then to the right y
(defn- interpolate [t1 [x y]]
  (loop [[px py] t1
         acc []]
    (let [sx (step px x)
          sy (if (= sx 0)
               (step py y)
               0)
          next (map + [px py] [sx sy])]
      (if (= [0 0] [sx sy])
        acc
        (recur next (conj acc next))))))

(defn receive-move [id p]
  (with-tile [t p]
    (let [prev (@pointers id)]
      (when-not (= prev t)
        (loop [prev prev
               [t & tnext] (interpolate prev t)]
          (swap! pointers assoc id t)
          (on-move-handler id prev t)
          (if tnext
            (recur t tnext)))))))

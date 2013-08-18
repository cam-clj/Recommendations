(ns cam-clj.recommendations
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.data.priority-map :refer [priority-map priority-map-by]]
            [incanter.core :refer [abs]]
            [incanter.stats :refer [correlation euclidean-distance]]))

;; Where to find the MovieLens dataset
(def ^{:dynamic true} *ml-path* "/home/ray/Downloads/ml-100k")

(defn- maybe-parse-int
  "If the string `s` looks like an integer, return the integer value,
  otherwise return the string itself. "
  [s]
  (if (re-find #"^\d+$" s) (Integer/parseInt s) s))

(defn- parse-line
  "Return a function that splits a line on the regular expression
  `sep` and applies `maybe-parse-int` to each column."
  [sep]
  (fn [s] (map maybe-parse-int (str/split s sep))))

(defn load-titles
  "Lead the movie titles dataset. Return a map of `id` to `title`."
  []
  (with-open [rdr (io/reader (io/file *ml-path* "u.item"))]
    (reduce (fn [accum [id title]] (assoc accum id title))
            {}
            (map (parse-line #"\|") (line-seq rdr)))))

(defn load-ratings
  "Load the user ratings. Return a nested map of `user`/`title` to `rating`."
  []
  (let [titles (load-titles)]
    (with-open [rdr (io/reader (io/file *ml-path* "u.data"))]
      (reduce (fn [accum [user-id movie-id rating]] (assoc-in accum [user-id (titles movie-id)] rating))
              {}
              (map (parse-line #"\t") (line-seq rdr))))))

(defn common-ratings
  "Given a `ratings` map and two people, return the set of ratings
  they have in common."
  [ratings p1 p2]
  (let [p1-keys (set (keys (ratings p1)))
        p2-keys (set (keys (ratings p2)))]
    (set/intersection p1-keys p2-keys)))

(defn build-sim-fn
  "Given a function `f` to score two sets of ratings for similarity,
  return a function that takes a map of ratings and two people, and
  returns their similarity score."
  [f]
  (fn [ratings p1 p2]
    (let [ks (common-ratings ratings p1 p2)]
      (if (seq ks)
        (f (map (ratings p1) ks) (map (ratings p2) ks))
        0))))

;; A similarity function based on Euclidean distance
(def sim-euclidean (build-sim-fn #(/ 1 (+ 1 (euclidean-distance %1 %2)))))

;; A similarity function based on Pearson correlation. We scale the
;; correlation (which is between -1 and 1) to give a value between 0
;; and 1, so it's on the same scale as sim-euclidean.
(def sim-pearson (build-sim-fn #(/ (+ 1 (correlation %1 %2)) 2)))

;; We use a priority map to accumulate the top n users. This is a map
;; sorted on value, so the first element of the map (returned by
;; `peek`) has the smallest value, and `pop` removes this entry from
;; the map. Once the accumulator has grown to `n` entries, we compare
;; the next score with the first entry and, if it is bigger, pop off
;; the smaller entry and add the new one. Otherwise, we ignore the new
;; entry and return the accumulator unchanged.

(defn top-n-similar-users
  "Find the `n` users most similar to `p`, where the similarity between
  two users is computed by `sim-fn`. Return a map keyed by user id
  whose value is the similarity score for that user."
  [sim-fn ratings p n]
  (reduce (fn [accum p']
            (let [s (sim-fn ratings p p')]
              (cond
               (< (count accum) n)      (conj accum [p' s])
               (> s (val (peek accum))) (conj (pop accum) [p' s])
               :else                    accum)))
          (priority-map)
          (remove #{p} (keys ratings))))

(defn score-for
  "Given a map of friends' similarity scores, return the weighted score for `item`."
  [ratings friends item]
  (loop [n 0 d 1 friends (seq friends)]
    (if friends
      (let [[friend-id friend-similarity] (first friends)]
        (if-let [friend-rating (get-in ratings [friend-id item])]
          (recur (+ n (* friend-rating friend-similarity)) (+ d (abs friend-similarity)) (next friends))
          (recur n d (next friends))))
      (/ n d))))

(defn recommendations-for
  "Return a sorted sequence of recommendations for `p`, with the highest recommendation first."
  ([sim-fn ratings p]
     (recommendations-for sim-fn ratings p (count ratings)))
  ([sim-fn ratings p n]
     (let [friends (top-n-similar-users sim-fn ratings p n)
           unseen  (remove (ratings p) (reduce set/union (map (comp set keys ratings) (keys friends))))
           ranked  (into (priority-map-by >)
                         (map vector unseen (map (partial score-for ratings friends) unseen)))]
       (keys ranked))))

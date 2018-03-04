(ns re-play.utils)

(defn current-time []
  (.getTime (js/Date.)))

(ns re-play.utils)

(defn current-time
  "Gets the current time in millis"
  []
  (.getTime (js/Date.)))

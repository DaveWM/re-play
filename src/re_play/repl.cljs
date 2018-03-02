(ns re-play.repl
  (:require [re-frame.core :as rf]
            [clojure.core.async :as async :refer [<!!]]
            [re-play.core :refer [tape replaying]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn- current-time []
  (.getTime (js/Date.)))

(defn mark
  ([] {:start-time (current-time)})
  ([time] {:start-time time}))

(defn end-mark
  ([mark] (assoc mark :end-time (current-time)))
  ([mark time] (assoc mark :end-time time)))

(defn db-at-time [time]
  (->> @tape
       (filter #(> time (:time %)))
       last
       :db-after))

(defn db-at-mark [{:keys [start-time]}]
  (db-at-time start-time))

(defn db-before-tape [tape]
  (-> (first tape)
      :time
      dec
      db-at-time))

(defn marked-tape [{:keys [start-time end-time]}]
  (->> @tape
       (filter #(<= start-time (:time %) end-time))
       vec))

(defn instant-replay!
  ([tape-to-replay]
   (instant-replay! tape-to-replay {}))
  
  ([tape-to-replay options]
   (let [default-options {:intitial-db (db-before-tape tape-to-replay)}
         {:keys [initial-db]} (merge default-options options)
         events (map :event tape-to-replay)]
     (reset! replaying true)
     (rf/dispatch-sync [::reset initial-db])
     (doseq [event events]
       (rf/dispatch-sync event))
     (reset! replaying false)
     nil)))

(defn replay!
  ([tape-to-replay]
   (replay! tape-to-replay {}))
  ([tape-to-replay options]
   (let [default-options {:initial-db (db-before-tape tape-to-replay)
                          :speed 1}
         {:keys [initial-db speed]} (merge default-options options)
         start-time (:time (first tape-to-replay))
         events-with-time (->> tape-to-replay
                               (map (fn [event]
                                      (update event :time #(- % start-time))))
                               (map (juxt :event :time)))]
     (reset! replaying true)
     (rf/dispatch-sync [::reset initial-db])
     (go-loop [[[event time] & other-events] events-with-time]
       (<! (async/timeout (/ time speed)))
       (rf/dispatch-sync event)
       (if other-events
         (recur other-events)
         (reset! replaying false)))
     nil)))

(def *slowmo-speed* 0.25)

(defn slowmo-replay!
  ([tape-to-replay]
   (replay! tape-to-replay {:speed *slowmo-speed*}))
  ([tape-to-replay options]
   (replay! tape-to-replay (assoc options :speed *slowmo-speed*))))

(defn go-back! [marked-tape]
  (let [new-tape (butlast marked-tape)
        penultimate-event-db (:db-after (last new-tape))]
    (rf/dispatch-sync [::reset penultimate-event-db])
    new-tape))

(defn go-forwards! [marked-tape]
  (let [last-event (last marked-tape)
        new-event (-> @tape
                      (filter (fn [{:keys [time] :as event}]
                                (< (:time last-event) (:time %))))
                      first)
        new-event-db (:db-after new-event)]
    (rf/dispatch-sync [::reset new-event-db])
    (conj marked-tape new-event)))

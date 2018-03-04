(ns re-play.repl
  (:require [re-frame.core :as rf]
            [clojure.core.async :as async :refer [<!]]
            [re-play.core :as rp :refer [tape replaying]]
            [re-play.utils :as utils])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn mark
  "Starts a mark. When provided a time, uses that time as the start time, otherwise uses the current time."
  ([] {:start-time (utils/current-time)})
  ([time] {:start-time time}))

(defn end-mark
  "Ends a mark. When provided a time, uses that time as the end time, otherwise uses the current time."
  ([mark] (assoc mark :end-time (utils/current-time)))
  ([mark time] (assoc mark :end-time time)))

(defn db-at-time
  "Gets the app-db as of the given time."
  [time]
  (->> @tape
       (filter #(> time (:time %)))
       last
       :db-after))

(defn db-at-mark
  "Gets the app-db as of the start-time of the given mark."
  [{:keys [start-time]}]
  (db-at-time start-time))

(defn db-before-tape
  "Gets the app-db before the start of the given tape (a seq of events)"
  [tape]
  (-> (first tape)
      :time
      dec
      db-at-time))

(defn marked-tape
  "Gets the section of tape that is covered by the given mark (i.e. between the start and end times of the mark)."
  [{:keys [start-time end-time]}]
  (->> @tape
       (filter #(<= start-time (:time %) end-time))
       vec))

(defn all-tape
  "Gets all the events recorded onto the tape."
  []
  @tape)

(defn instant-replay!
  "Synchronously and instantaneously replays the given events. Takes a seq of events, and an optional map containing an :initial-db key (the initial value of app-db to start the replay with)."
  ([tape-to-replay]
   (instant-replay! tape-to-replay {}))
  
  ([tape-to-replay options]
   (let [default-options {:intitial-db (db-before-tape tape-to-replay)}
         {:keys [initial-db]} (merge default-options options)
         events (map :event tape-to-replay)]
     (reset! replaying true)
     (rf/dispatch-sync [::rp/reset initial-db])
     (doseq [event events]
       (rf/dispatch-sync event))
     (reset! replaying false)
     nil)))

(defn replay!
  "Asynchronously replays the given events. Do not run more than 1 replay at once. Takes a seq of events, and an optional map containing an :initial-db key (the initial value of app-db to start the replay with), and :speed (a multiplier for the speed at which events are replayed)."
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
     (rf/dispatch-sync [::rp/reset initial-db])
     (go-loop [[[event time] & other-events] events-with-time]
       (<! (async/timeout (/ time speed)))
       (rf/dispatch-sync event)
       (if other-events
         (recur other-events)
         (reset! replaying false)))
     nil)))

(def *slowmo-speed*
  "The speed which slowmo-replay replays events. It is recommended to use replay! with the optional :speed parameter rather than overriding this."
  0.25)

(defn slowmo-replay!
  "Same as replay, but at 1/4 speed."
  ([tape-to-replay]
   (replay! tape-to-replay {:speed *slowmo-speed*}))
  ([tape-to-replay options]
   (replay! tape-to-replay (assoc options :speed *slowmo-speed*))))

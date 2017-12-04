(ns re-play.core
  (:require [re-frame.core :as rf]
            [clojure.core.async :as async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defonce tape (atom []))
(defonce ^:private replaying (atom false))

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
   (instant-replay! tape-to-replay (db-before-tape tape-to-replay)))
  
  ([tape-to-replay initial-db]
   (let [events (map :event tape-to-replay)]
     (reset! replaying true)
     (rf/dispatch-sync [::reset initial-db])
     (doseq [event events]
       (rf/dispatch-sync event))
     (reset! replaying false)
     nil)))

(defn replay!
  ([tape-to-replay]
   (replay! tape-to-replay (db-before-tape tape-to-replay)))
  ([tape-to-replay initial-db]
   (replay! tape-to-replay initial-db 1))
  ([tape-to-replay initial-db speed]
   (let [start-time (:time (first tape-to-replay))
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

(defn slowmo-replay!
  ([tape-to-replay initial-db]
   (replay! tape-to-replay initial-db 0.25))
  ([tape-to-replay]
   (replay! tape-to-replay (db-before-tape tape-to-replay) 0.25)))

(def recordable
  (rf/->interceptor
   :id :record
   :after (fn [context]
            (let [db (get-in context [:effects :db])
                  [event-type :as event] (get-in context [:coeffects :event])]
              (swap! tape #(conj % {:time (current-time)
                                    :db-after db
                                    :event event}))
              (if @replaying
                (update context :effects #(select-keys % [:db]))
                context)))))

(rf/reg-event-db
 ::reset
 [recordable]
 (fn [db [_ new-db]]
   new-db))

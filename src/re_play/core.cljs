(ns re-play.core
  (:require [re-frame.core :as rf]
            [re-play.utils :as utils]))

(defonce tape (atom []))
(defonce ^:private replaying (atom false))

(def *replayable-effects* (atom []))

(def recordable
  (rf/->interceptor
   :id :record
   :after (fn [context]
            (let [db (get-in context [:effects :db])
                  [event-type :as event] (get-in context [:coeffects :event])]
              (swap! tape #(conj % {:time (utils/current-time)
                                    :db-after db
                                    :event event}))
              (if @replaying
                (update context :effects #(select-keys % (cons :db @*replayable-effects*)))
                context)))))

(rf/reg-event-db
 ::reset
 [recordable]
 (fn [db [_ new-db]]
   new-db))

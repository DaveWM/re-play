(ns re-play.core
  (:require [re-frame.core :as rf]
            [re-play.utils :as utils]))

(defonce ^{:doc "Atom containing a seq of all events recorded by the 'recordable' interceptor. All events have the :time, :db-after, and :event keys."}
  tape (atom []))

(defonce ^{:doc "Atom containing a boolean of whether a replay is currently in progress"}
  replaying (atom false))

(def *replayable-effects*
  "An atom containing a set of effects that should be replayed (in addition to the :db effect). Use with caution."
  (atom #{}))

(def recordable
  "An interceptor for recording events onto tape. When replaying, it also removes all side effects (apart from those in *replayable-effects*)."
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

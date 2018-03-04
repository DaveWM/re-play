# re-play (alpha)

The aim of re-play is to shorten your feedback loop when debugging sequences of events.

## Why is it useful?

Debugging sequences of events, i.e. when one event triggers other events, can be quite tedious. This is especially true if some events in the chain cause side effects which aren't easy to undo (e.g. a POST request). Re-play gives you a way to easily replay a series of events, without triggering any side effects.

For example, say in your app you have a button that, when clicked, should be disabled for 2 seconds. Now imagine there's a bug in one of your event handlers, so the button is never re-enabled. Debugging this manually is a pain, because after you've clicked the button you can't (easily) get back to the original state, so you have to refresh the application and start again.

If you use re-play instead, you can record the events that occured when you clicked the button, modify your code, then replay (in slow motion if you want) those events to see if you've fixed the bug. This drastically shortens your feedback loop.

This may seem like a contrived example, but in real apps you can often find yourself in similar situations. Having to reload the entire app, and re-create the state of the app, on every code change can make your feedback loop very long. Re-play aims to shorten this feedback loop.

## Usage

First, add re-play to your `project.clj` dependencies:

`[re-play "0.1.0-SNAPSHOT"]`

You then need to add the `recordable` interceptor to all event handlers that you want to be able to replay. If possible, I'd recommend doing this for every event handler. You add the interceptor like this:

```clojure
(ns my-ns
  (:require [re-frame.core :as rf]
            [re-play.core :as rp]))
  
(rf/reg-event-db
 :init
 [rp/recordable]
 (constantly {:count 0}))

```

This interceptor will record all events that occur in the `tape` atom in `re-play.core`.

Start up figwheel, and in the repl run `(require '[re-play.repl :as rp])`. There are several useful functions in this namespace (see the docs), but let's just consider the `replay!` function to start with. This function takes a seq of events, and replays them at real-time speed.

To get these events, you can use what re-play calls `marks`. This is just a map, containing the `:start-time` and `:end-time` keys. Run this in your repl to define a mark: 
```clojure
(def m (rp/mark))
```
Do something in your app, then run:
```clojure
(def mark (rp/end-mark m))
```
You can now get a seq of events that occured between the start and the end of the mark by running:
```clojure
(def events (rp/marked-tape mark))
```
You can then replay the events like so:
```clojure
(rp/replay! events)
```
This should replay the events that ocurred between the start and the end of the mark in real time. If you want to see the events in slow motion, use `slowmo-replay!`.

You can replay events even after you've made changes to your event handler code. Try making some changes to your event handler code, and re-running `replay!` - the events will be replayed through your new event handlers.

## How it works

The `recordable` interceptor records every event in the `tape` atom in `re-play.core`. When you replay events (using either `replay!` or `instant-replay!`), re-play will fire a `re-play.core/reset` event. This will reset the app-db to its value before the sequence of events. Re-play then fires all the events that you pass to `replay!`/`instant-replay!` using `rf/dispatch-sync` (how exactly this is done is different for each function, see the docs).

It's important to note that `tape` is an append-only seq of _all_ events that occur - including those triggered by re-play itself. Therefore, if you inspect the `tape` atom after you call `replay!`, you will see a `re-play.core/reset` event, and all the events you passed to `replay!`.

## License

Distributed under the MIT license.

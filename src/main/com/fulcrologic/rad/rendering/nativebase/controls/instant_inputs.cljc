(ns com.fulcrologic.rad.rendering.nativebase.controls.instant-inputs
  (:require
    [com.fulcrologic.rad.type-support.date-time :as dt]
    [com.fulcrologic.rad.rendering.nativebase.controls.control :as control]
    [cljc.java-time.local-time :as lt]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [cljc.java-time.local-date-time :as ldt]
    [cljc.java-time.local-date :as ld]))

(defn ui-date-instant-input [{::keys [default-local-time]} {:keys [value onChange local-time] :as props}]
  (let [value      (dt/inst->html-date (or value (dt/now)))
        local-time (or local-time default-local-time)]
    ;; TASK: port
    #_(dom/input
        (merge props
          {:value        value
           :type         "date"
           :onChangeText (fn [txt]
                           (when onChange
                             (let [date-string (evt/target-value evt)
                                   instant     (dt/html-date->inst date-string local-time)]
                               (onChange instant))))}))))

(defn ui-ending-date-instant-input
  "Display the date the user selects, but control a value that is midnight on the next date. Used for generating ending
  instants that can be used for a proper non-inclusive end date."
  [_ {:keys [value onChange] :as props}]
  (let [today        (dt/inst->local-datetime (or value (dt/now)))
        display-date (ldt/to-local-date (ldt/minus-days today 1))
        value        (dt/local-date->html-date-string display-date)]
    ;; TASK: port
    #_(dom/input
        (merge props
          {:value    value
           :type     "date"
           :onChange (fn [evt]
                       (when onChange
                         (let [date-string (evt/target-value evt)
                               tomorrow    (ld/at-time (ld/plus-days (dt/html-date-string->local-date date-string) 1)
                                             lt/midnight)
                               instant     (dt/local-datetime->inst tomorrow)]
                           (onChange instant))))}))))

(defn ui-date-time-instant-input [_ {:keys [disabled? value onChange] :as props}]
  (let [value (dt/inst->html-datetime-string (or value (dt/now)))]
    ;; TASK: port
    #_(dom/input
        (merge props
          (cond->
            {:value    value
             :type     "date"
             :onChange (fn [evt]
                         (when onChange
                           (let [date-time-string (evt/target-value evt)
                                 instant          (dt/html-datetime-string->inst date-time-string)]
                             (onChange instant))))}
            disabled? (assoc :readOnly true))))))

(defn date-time-control [render-env]
  (control/ui-control (assoc render-env :input-factory ui-date-time-instant-input)))

(defn midnight-on-date-control [render-env]
  (control/ui-control (assoc render-env
                        :input-factory ui-date-instant-input
                        ::default-local-time lt/midnight)))

(defn midnight-next-date-control [render-env]
  (control/ui-control (assoc render-env
                        :input-factory ui-ending-date-instant-input)))

(defn date-at-noon-control [render-env]
  (control/ui-control (assoc render-env
                        ::default-local-time lt/noon
                        :input-factory ui-date-instant-input)))


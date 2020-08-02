(ns com.fulcrologic.rad.rendering.nativebase.instant-field
  (:require
    [com.fulcrologic.rad.rendering.nativebase.field :refer [render-field-factory]]))

(def ui-datetime-input
  #_(comp/factory (inputs/StringBufferedInput ::DateTimeInput
                    {:model->string (fn [tm]
                                      (if tm
                                        (datetime/inst->html-datetime-string tm)
                                        ""))
                     :string->model (fn [s] (some-> s (datetime/html-datetime-string->inst)))})))

(def ui-date-noon-input
  #_(comp/factory (inputs/StringBufferedInput ::DateTimeInput
                    {:model->string (fn [tm]
                                      (if tm
                                        (str/replace (datetime/inst->html-datetime-string tm) #"T.*$" "")
                                        ""))
                     :string->model (fn [s] (some-> s (str "T12:00") (datetime/html-datetime-string->inst)))})))

(def render-field
  "Uses current timezone and gathers date/time."
  (render-field-factory {:type "datetime-local"} ui-datetime-input))
(def render-date-at-noon-field
  "Uses current timezone and gathers a local date but saves it as an instant at noon on that date."
  (render-field-factory {:type "date"} ui-date-noon-input))


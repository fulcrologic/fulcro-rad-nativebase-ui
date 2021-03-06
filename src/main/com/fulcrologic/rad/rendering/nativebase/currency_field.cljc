(ns com.fulcrologic.rad.rendering.nativebase.currency-field
  (:require
    [com.fulcrologic.rad.rendering.nativebase.field :refer [render-field-factory]]
    [com.fulcrologic.rad.type-support.decimal :as math]))

(def ui-currency-input
  #_(comp/factory (inputs/StringBufferedInput ::DecimalInput
                    {:model->string (fn [n]
                                      (math/numeric->currency-str n))
                     :string->model (fn [s]
                                      (math/numeric (str/replace s #"[$,]" "")))
                     :string-filter (fn [s] s)})))

(def render-field (render-field-factory {} ui-currency-input))

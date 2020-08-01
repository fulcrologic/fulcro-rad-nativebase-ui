(ns com.fulcrologic.rad.rendering.nativebase.int-field
  (:require
    [com.fulcrologic.fulcro.dom.inputs :as inputs]
    [com.fulcrologic.rad.rendering.nativebase.field :refer [render-field-factory]]))

(def render-field (render-field-factory inputs/ui-int-input))

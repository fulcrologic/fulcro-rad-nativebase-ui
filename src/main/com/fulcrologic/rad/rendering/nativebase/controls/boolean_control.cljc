(ns com.fulcrologic.rad.rendering.nativebase.controls.boolean-control
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :refer [body list-item checkbox]]
    [com.fulcrologic.rad.control :as control]))

(defsc BooleanControl [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        {:keys [label onChange disabled? visible?] :as control} (get controls control-key)]
    (if control
      (let [label     (or (?! label instance))
            disabled? (?! disabled? instance)
            visible?  (or (nil? visible?) (?! visible? instance))
            value     (control/current-value instance control-key)]
        (when visible?
          (list-item {:key (str control-key)}
            (checkbox {:checked (boolean value)
                       :onPress (fn [_]
                                  (when-not disabled?
                                    (control/set-parameter! instance control-key (not value))
                                    (when onChange
                                      (onChange instance (not value)))))})
            (body {} label))))
      (log/error "Could not find control definition for " control-key))))

(def render-control (comp/factory BooleanControl {:keyfn :control-key}))

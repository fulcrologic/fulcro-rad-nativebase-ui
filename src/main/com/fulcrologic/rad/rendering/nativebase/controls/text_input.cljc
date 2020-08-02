(ns com.fulcrologic.rad.rendering.nativebase.controls.text-input
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.options-util :refer [?! debounce]]
    [taoensso.timbre :as log]))

(defsc TextControl [this {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        {:keys [onChange placeholder disabled? visible?] :as control} (get controls control-key)]
    (when control
      (let [disabled? (?! disabled? instance)
            visible?  (or (nil? visible?) (?! visible? instance))
            chg!      #(control/set-parameter! instance control-key %)
            run!      (fn [v] (when onChange (onChange instance v)))
            value     (control/current-value instance control-key)]
        (when visible?
          (nbc/input {:placeholder  placeholder
                      :value        (str value)
                      :enabled      (not disabled?)
                      :onEndEditing (fn [] (run! value))
                      :onChangeText (fn [v] (chg! v))}))))))

(def render-control (comp/factory TextControl {:keyfn :control-key}))

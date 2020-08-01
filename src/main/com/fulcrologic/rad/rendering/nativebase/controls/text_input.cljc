(ns com.fulcrologic.rad.rendering.nativebase.controls.text-input
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.options-util :refer [?! debounce]]
    [taoensso.timbre :as log]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])))

(defsc TextControl [this {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        props    (comp/props instance)
        {:keys [label onChange icon placeholder disabled? visible?] :as control} (get controls control-key)]
    (when control
      (let [label       (?! label instance)
            disabled?   (?! disabled? instance)
            placeholder (?! placeholder)
            visible?    (or (nil? visible?) (?! visible? instance))
            chg!        #(control/set-parameter! instance control-key (evt/target-value %))
            run!        (fn [evt] (let [v (evt/target-value evt)]
                                    (when onChange (onChange instance v))))
            value       (control/current-value instance control-key)]
        (when visible?
          (dom/div :.ui.field {:key (str control-key)}
            (dom/label label)
            (if icon
              (dom/div :.ui.icon.input
                (dom/i {:className (str icon " icon")})
                (dom/input {:readOnly    (boolean disabled?)
                            :placeholder (str placeholder)
                            :onChange    chg!
                            :onBlur      run!
                            :onKeyDown   (fn [evt] (when (evt/enter? evt) (run! evt)))
                            :value       (str value)}))
              (dom/input {:readOnly    (boolean disabled?)
                          :placeholder (str placeholder)
                          :onChange    chg!
                          :onBlur      run!
                          :onKeyDown   (fn [evt] (when (evt/enter? evt) (run! evt)))
                          :value       (str value)}))))))))

(def render-control (comp/factory TextControl {:keyfn :control-key}))

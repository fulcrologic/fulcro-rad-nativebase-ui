(ns com.fulcrologic.rad.rendering.nativebase.controls.pickers
  (:require
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.picker-options :as po]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.rendering.nativebase.components :refer [ui-wrapped-dropdown]]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [taoensso.timbre :as log]))

(defsc SimplePicker [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)
   :componentDidMount     (fn [this]
                            (let [{:keys [instance control-key] :as props} (comp/props this)
                                  controls (control/component-controls instance)
                                  {::po/keys [query-key] :as picker-options} (get controls control-key)]
                              (when query-key
                                (po/load-picker-options! instance (comp/react-type instance) props picker-options))))}
  (let [controls (control/component-controls instance)
        props    (comp/props instance)
        {::po/keys [query-key cache-key]
         :keys     [label onChange disabled? visible? action placeholder options user-props] :as control} (get controls control-key)
        options  (or options (get-in props [::po/options-cache (or cache-key query-key) :options]))]
    (when control
      (let [label       (or (?! label instance))
            disabled?   (?! disabled? instance)
            placeholder (?! placeholder instance)
            visible?    (or (nil? visible?) (?! visible? instance))
            value       (control/current-value instance control-key)]
        (when visible?
          (ui-wrapped-dropdown (merge
                                 user-props
                                 {:disabled    disabled?
                                  :label       label
                                  :placeholder (str placeholder)
                                  :options     options
                                  :value       value
                                  :onChange    (fn [v]
                                                 (control/set-parameter! instance control-key v)
                                                 (binding [comp/*after-render* true]
                                                   (when onChange
                                                     (onChange instance v))
                                                   (when action
                                                     (action instance))))})))))))

(def render-control (comp/factory SimplePicker {:keyfn :control-key}))


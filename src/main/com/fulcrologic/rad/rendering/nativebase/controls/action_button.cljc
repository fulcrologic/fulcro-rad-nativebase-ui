(ns com.fulcrologic.rad.rendering.nativebase.controls.action-button
  (:require
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))

(defsc ActionButton [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        props    (comp/props instance)
        {:keys [label icon action disabled? visible?] :as control} (get controls control-key)]
    (when control
      (let [label     (?! label instance)
            loading?  (df/loading? (get-in props [df/marker-table (comp/get-ident instance)]))
            disabled? (or loading? (?! disabled? instance))
            visible?  (or (nil? visible?) (?! visible? instance))]
        (when visible?
          (nbc/button
            {:key      (str control-key)
             :disabled (boolean disabled?)
             :onPress  (fn [] (when action (action instance control-key)))}
            #_(when icon (dom/i {:className (str icon " icon")}))
            (when label label)))))))

(def render-control (comp/factory ActionButton {:keyfn :control-key}))

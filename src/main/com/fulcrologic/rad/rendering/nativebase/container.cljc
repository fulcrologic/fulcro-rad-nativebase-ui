(ns com.fulcrologic.rad.rendering.nativebase.container
  (:require
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.container :as container]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.rendering.nativebase.form :as nbc-form]
    [taoensso.timbre :as log]))

(comp/defsc StandardContainerControls [_ {:keys [instance]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        {:keys [input-layout action-layout]} (control/standard-control-layout instance)]
    #_(div :.ui.top.attached.compact.basic.segment
      (dom/h3 :.ui.header
        (or (some-> instance comp/component-options ::container/title (?! instance)) "")
        (div :.ui.right.floated.buttons
          (keep (fn [k] (control/render-control instance k (get controls k))) action-layout)))
      (div :.ui.form
        (map-indexed
          (fn [idx row]
            (div {:key idx :className (nbc-form/n-fields-string (count row))}
              (map #(if-let [c (get controls %)]
                      (control/render-control instance % c)
                      (dom/div :.ui.field {:key (str %)} "")) row)))
          input-layout)))))

(let [ui-standard-container-controls (comp/factory StandardContainerControls)]
  (defn render-standard-controls [instance]
    (ui-standard-container-controls {:instance instance})))

(defn render-container-layout [container-instance]
  (let [{::container/keys [children layout]} (comp/component-options container-instance)]
    ;; TODO: Custom controls rendering as a separate config?
    (let [container-props (comp/props container-instance)
          render-cls      (fn [id cls]
                            (let [factory (comp/computed-factory cls)
                                  props   (get container-props id {})]
                              (factory props {::container/controlled? true})))]
      #_(dom/div :.ui.basic.segments
        (render-standard-controls container-instance)
        (dom/div :.ui.basic.segment
          (if layout
            (dom/div :.ui.container.centered.grid
              (map-indexed
                (fn *render-row [idx row]
                  (let [cols (count row)]
                    (dom/div :.row {:key idx}
                      (map
                        (fn *render-col [entry]
                          (let [id    (if (keyword? entry) entry (:id entry))
                                width (or
                                        (and (map? entry) (:width entry))
                                        (int (/ 16 cols)))
                                cls   (get children id)]
                            (dom/div {:key id :classes [(when width (str (n-string width) " wide")) "column"]}
                              (render-cls id cls))))
                        row))))
                layout))
            (map
              (fn [[id cls]]
                (dom/div {:key id}
                  (render-cls id cls))) (container/id-child-pairs children))))))))

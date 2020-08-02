(ns com.fulcrologic.rad.rendering.nativebase.report
  (:require
    [clojure.string :as str]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.rad.rendering.nativebase.form :as sui-form]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.form :as form]))

(defn row-action-buttons [report-instance row-props]
  (let [{::report/keys [row-actions]} (comp/component-options report-instance)]
    (when (seq row-actions)
      #_(div :.ui.buttons
          (map-indexed
            (fn [idx {:keys [label reload? visible? disabled? action]}]
              (when (or (nil? visible?) (?! visible? report-instance row-props))
                (dom/button :.ui.button
                  {:key      idx
                   :disabled (boolean (?! disabled? report-instance row-props))
                   :onClick  (fn [evt]
                               (evt/stop-propagation! evt)
                               (when action
                                 (action report-instance row-props)
                                 (when reload?
                                   (control/run! report-instance))))}
                  (?! label report-instance row-props))))
            row-actions)))))

(comp/defsc ListRowLayout [this {:keys [report-instance props]}]
  {}
  (let [{::report/keys [columns]} (comp/component-options report-instance)]
    (let [header-column      (first columns)
          description-column (second columns)
          {:keys [edit-form entity-id]} (some->> header-column (::attr/qualified-key) (report/form-link report-instance props))
          header-label       (some->> header-column (report/formatted-column-value report-instance props))
          description-label  (some->> description-column (report/formatted-column-value report-instance props))
          action-buttons     (row-action-buttons report-instance props)]
      #_(div :.item
          (div :.content
            (when action-buttons
              (div :.right.floated.content
                action-buttons))
            (when header-label
              (if edit-form
                (dom/a :.header {:onClick (fn [evt]
                                            (evt/stop-propagation! evt)
                                            (form/edit! report-instance edit-form entity-id))} header-label)
                (div :.header header-label)))
            (when description-label
              (div :.description description-label)))))))

(let [ui-list-row-layout (comp/factory ListRowLayout {:keyfn ::report/idx})]
  (defn render-list-row [report-instance row-class row-props]
    (ui-list-row-layout {:report-instance report-instance
                         :row-class       row-class
                         :props           row-props})))

(comp/defsc StandardReportControls [this {:keys [report-instance] :as env}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls report-instance)
        {:keys [::report/paginate?]} (comp/component-options report-instance)
        {:keys [input-layout action-layout]} (control/standard-control-layout report-instance)
        {:com.fulcrologic.rad.container/keys [controlled?]} (comp/get-computed report-instance)]
    #_(comp/fragment
        (div :.ui.top.attached.compact.segment
          (dom/h3 :.ui.header
            (or (some-> report-instance comp/component-options ::report/title (?! report-instance)) "Report")
            (div :.ui.right.floated.buttons
              (keep (fn [k]
                      (let [control (get controls k)]
                        (when (or (not controlled?) (:local? control))
                          (control/render-control report-instance k control))))
                action-layout)))
          (div :.ui.form
            (map-indexed
              (fn [idx row]
                (div {:key idx :className (sui-form/n-fields-string (count row))}
                  (keep #(let [control (get controls %)]
                           (when (or (not controlled?) (:local? control))
                             (control/render-control report-instance % control))) row)))
              input-layout))
          (when paginate?
            (let [page-count (report/page-count report-instance)]
              (when (> page-count 1)
                (div :.ui.two.column.centered.grid
                  (div :.column
                    (div {:style {:paddingTop "4px"}}
                      #?(:cljs
                         (sui-pagination/ui-pagination {:activePage   (report/current-page report-instance)
                                                        :onPageChange (fn [_ data]
                                                                        (report/goto-page! report-instance (comp/isoget data "activePage")))
                                                        :totalPages   page-count
                                                        :size         "tiny"}))))))))))))

(let [ui-standard-report-controls (comp/factory StandardReportControls)]
  (defn render-standard-controls [report-instance]
    (ui-standard-report-controls {:report-instance report-instance})))

(comp/defsc ListReportLayout [this {:keys [report-instance] :as env}]
  {:shouldComponentUpdate (fn [_ _ _] true)
   :initLocalState        (fn [_] {:row-factory (memoize
                                                  (fn [cls]
                                                    (comp/computed-factory cls
                                                      {:keyfn (fn [props] (some-> props (comp/get-computed ::report/idx)))})))})}
  (let [{::report/keys [BodyItem]} (comp/component-options report-instance)
        render-row      ((comp/get-state this :row-factory) BodyItem)
        render-controls (report/control-renderer this)
        rows            (report/current-rows report-instance)
        loading?        (report/loading? report-instance)]
    (comp/fragment
      (when render-controls
        (render-controls report-instance))
      #_(div :.ui.attached.segment
          (div :.ui.loader {:classes [(when loading? "active")]})
          (when (seq rows)
            (div :.ui.relaxed.divided.list
              (map-indexed (fn [idx row] (render-row row {:report-instance report-instance
                                                          :row-class       BodyItem
                                                          ::report/idx     idx})) rows)))))))

(let [ui-list-report-layout (comp/factory ListReportLayout {:keyfn ::report/idx})]
  (defn render-list-report-layout [report-instance]
    (ui-list-report-layout {:report-instance report-instance})))
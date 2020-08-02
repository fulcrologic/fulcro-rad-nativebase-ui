(ns com.fulcrologic.rad.rendering.nativebase.form
  (:require
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.options-util :refer [?! narrow-keyword]]
    [com.fulcrologic.rad.ui-validation :as validation]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.fulcro-i18n.i18n :refer [tr]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]))

(defn render-to-many [{::form/keys [form-instance] :as env} {k ::attr/qualified-key :as attr} {::form/keys [subforms] :as options}]
  #_(let [{:nativebase/keys [add-position]
           ::form/keys      [ui title can-delete? can-add? added-via-upload?]} (get subforms k)
          form-instance-props (comp/props form-instance)
          read-only?          (form/read-only? form-instance attr)
          add?                (if read-only? false (?! can-add? form-instance attr))
          delete?             (fn [item] (and (not read-only?) (?! can-delete? form-instance item)))
          items               (get form-instance-props k)
          title               (?! (or title (some-> ui (comp/component-options ::form/title)) "") form-instance form-instance-props)
          invalid?            (validation/invalid-attribute-value? env attr)
          validation-message  (validation/validation-error-message env attr)
          add                 (when (or (nil? add?) add?)
                                (let [order (if (keyword? add?) add? :append)]
                                  (button :.ui.tiny.icon.button
                                    {:onClick (fn [_]
                                                (form/add-child! (assoc env
                                                                   ::form/order order
                                                                   ::form/parent-relation k
                                                                   ::form/parent form-instance
                                                                   ::form/child-class ui)))}
                                    (i :.plus.icon))))
          ui-factory          (comp/computed-factory ui {:keyfn (fn [item] (-> ui (comp/get-ident item) second str))})]
      (div :.ui.container {:key (str k)}
        (h3 title (span ent/nbsp ent/nbsp) (when (or (nil? add-position) (= :top add-position)) add))
        (when invalid?
          (div :.ui.error.message
            validation-message))
        (if (seq items)
          (div :.ui.segments
            (mapv
              (fn [props]
                (ui-factory props
                  (merge
                    env
                    {::form/parent          form-instance
                     ::form/parent-relation k
                     ::form/can-delete?     (if delete? (delete? props) false)})))
              items))
          (div :.ui.message "None."))
        (when (= :bottom add-position) add))))

(defn render-to-one [{::form/keys [form-instance] :as env} {k ::attr/qualified-key :as attr} {::form/keys [subforms] :as options}]
  #_(let [{::form/keys [ui can-delete? title]} (get subforms k)
          form-props         (comp/props form-instance)
          props              (get form-props k)
          title              (?! (or title (some-> ui (comp/component-options ::form/title)) "") form-instance form-props)
          ui-factory         (comp/computed-factory ui)
          invalid?           (validation/invalid-attribute-value? env attr)
          validation-message (validation/validation-error-message env attr)
          std-props          {::form/nested?         true
                              ::form/parent          form-instance
                              ::form/parent-relation k
                              ::form/can-delete?     (or
                                                       (?! can-delete? form-instance form-props)
                                                       false)}]
      (cond
        props
        (div {:key (str k)}
          (h3 :.ui.header title)
          (when invalid?
            (div :.ui.error.message validation-message))
          (ui-factory props (merge env std-props)))

        :else
        (div {:key (str k)}
          (h3 :.ui.header title)
          (button {:onClick (fn [] (form/add-child! (assoc env
                                                      ::form/parent-relation k
                                                      ::form/parent form-instance
                                                      ::form/child-class ui)))} "Create")))))

(defn standard-ref-container [env {::attr/keys [cardinality] :as attr} options]
  (if (= :many cardinality)
    (render-to-many env attr options)
    (render-to-one env attr options)))

(defn render-attribute [env attr {::form/keys [subforms] :as options}]
  (let [{k ::attr/qualified-key} attr]
    (if (contains? subforms k)
      (let [render-ref (or (form/ref-container-renderer env attr) standard-ref-container)]
        (render-ref env attr options))
      (form/render-field env attr))))

(def attribute-map (memoize
                     (fn [attributes]
                       (reduce
                         (fn [m {::attr/keys [qualified-key] :as attr}]
                           (assoc m qualified-key attr))
                         {}
                         attributes))))

(defn- render-layout* [env options k->attribute layout]
  (when #?(:clj true :cljs goog.DEBUG)
    (when-not (and (vector? layout) (every? vector? layout))
      (log/error "::form/layout must be a vector of vectors!")))
  (try
    #_(into []
        (map-indexed
          (fn [idx row]
            (div {:key idx :className (n-fields-string (count row))}
              (mapv (fn [col]
                      (enc/if-let [_    k->attribute
                                   attr (k->attribute col)]
                        (render-attribute env attr options)
                        (if (some-> options ::control/controls (get col))
                          (control/render-control (::form/form-instance env) col)
                          (log/error "Missing attribute (or lookup) for" col))))
                row)))
          layout))
    (catch #?(:clj Exception :cljs :default) _)))

(defn render-layout [env {::form/keys [attributes layout] :as options}]
  (let [k->attribute (attribute-map attributes)]
    (render-layout* env options k->attribute layout)))

(defsc TabbedLayout [this env {::form/keys [attributes tabbed-layout] :as options}]
  {:initLocalState (fn [this]
                     (try
                       {:current-tab 0
                        :tab-details (memoize
                                       (fn [attributes tabbed-layout]
                                         (let [k->attr           (attribute-map attributes)
                                               tab-labels        (filterv string? tabbed-layout)
                                               tab-label->layout (into {}
                                                                   (map vec)
                                                                   (partition 2 (mapv first (partition-by string? tabbed-layout))))]
                                           {:k->attr           k->attr
                                            :tab-labels        tab-labels
                                            :tab-label->layout tab-label->layout})))}
                       (catch #?(:clj Exception :cljs :default) _
                         (log/error "Cannot build tabs for tabbed layout. Check your tabbed-layout options for" (comp/component-name this)))))}
  (let [{:keys [tab-details current-tab]} (comp/get-state this)
        {:keys [k->attr tab-labels tab-label->layout]} (tab-details attributes tabbed-layout)
        active-layout (some->> current-tab
                        (get tab-labels)
                        (get tab-label->layout))]
    #_(div {:key (str current-tab)}
        (div :.ui.pointing.menu {}
          (map-indexed
            (fn [idx title]
              (dom/a :.item
                {:key     (str idx)
                 :onClick #(comp/set-state! this {:current-tab idx})
                 :classes [(when (= current-tab idx) "active")]}
                title)) tab-labels))
        (div :.ui.segment
          (render-layout* env options k->attr active-layout)))))

(def ui-tabbed-layout (comp/computed-factory TabbedLayout))

(declare standard-form-layout-renderer)

(defsc StandardFormContainer [this {::form/keys [props computed-props form-instance master-form] :as env}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [{::form/keys [can-delete?]} computed-props
        nested?         (not= master-form form-instance)
        read-only-form? (or
                          (?! (comp/component-options form-instance ::form/read-only?) form-instance)
                          (?! (comp/component-options master-form ::form/read-only?) master-form))
        invalid?        (if read-only-form? false (form/invalid? env))
        render-fields   (or (form/form-layout-renderer env) standard-form-layout-renderer)]
    (when #?(:cljs goog.DEBUG :clj true)
      (let [valid? (if read-only-form? true (form/valid? env))
            dirty? (if read-only-form? false (or (:ui/new? props) (fs/dirty? props)))]
        (log/debug "Form " (comp/component-name form-instance) " valid? " valid?)
        (log/debug "Form " (comp/component-name form-instance) " dirty? " dirty?)))
    #_(if nested?
      (div :.ui.segment
        (div :.ui.form {:classes [(when invalid? "error")]
                        :key     (str (comp/get-ident form-instance))}
          (when can-delete?
            (button :.ui.icon.primary.right.floated.button {:disabled (not can-delete?)
                                                            :onClick  (fn []
                                                                        (form/delete-child! env))}
              (i :.times.icon)))
          (render-fields env)))
      (let [{::form/keys [title action-buttons controls]} (comp/component-options form-instance)
            title          (?! title form-instance props)
            action-buttons (if action-buttons action-buttons form/standard-action-buttons)]
        (div :.ui.container {:key (str (comp/get-ident form-instance))}
          (div :.ui.top.attached.segment
            (dom/h3 :.ui.header
              title
              (div :.ui.right.floated.buttons
                (keep #(control/render-control master-form %) action-buttons))))
          (div :.ui.attached.form {:classes [(when invalid? "error")]}
            (div :.ui.error.message (tr "The form has errors and cannot be saved."))
            (div :.ui.attached.segment
              (render-fields env))))))))

(def standard-form-container (comp/factory StandardFormContainer))

(defn standard-form-layout-renderer [{::form/keys [form-instance] :as env}]
  (let [{::form/keys [attributes layout tabbed-layout] :as options} (comp/component-options form-instance)]
    (cond
      (vector? layout) (render-layout env options)
      (vector? tabbed-layout) (ui-tabbed-layout env options)
      :else (mapv (fn [attr] (render-attribute env attr options)) attributes))))

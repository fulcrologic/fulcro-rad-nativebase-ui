(ns com.fulcrologic.rad.rendering.nativebase.enumerated-field
  (:require
    #?@(:cljs [[cljs.reader :refer [read-string]]])
    [com.fulcrologic.rad.ui-validation :as validation]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.rendering.nativebase.components :refer [ui-wrapped-dropdown]]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.rad.attributes :as attr]
    [clojure.string :as str]
    [com.fulcrologic.rad.form :as form]))

(defn enumerated-options [{::form/keys [form-instance] :as env} {::attr/keys [qualified-key] :as attribute}]
  (let [{::attr/keys [enumerated-values]} attribute
        enumeration-labels (merge
                             (::attr/enumerated-labels attribute)
                             (comp/component-options form-instance ::form/enumerated-labels qualified-key))]
    ;; TODO: Sorting should be something users control
    (sort-by :text
      (mapv (fn [k]
              {:text  (?! (get enumeration-labels k (name k)))
               :value k}) enumerated-values))))

(defn- render-to-many [{::form/keys [form-instance] :as env} {::form/keys [field-label]
                                                              ::attr/keys [qualified-key] :as attribute}]
  (when (form/field-visible? form-instance attribute)
    (let [props        (comp/props form-instance)
          read-only?   (form/read-only? form-instance attribute)
          options      (enumerated-options env attribute)
          selected-ids (set (get props qualified-key))
          label        (or field-label (some-> qualified-key name str/capitalize))
          item-array   (to-array options)]
      ;; TASK: Cannot be embedded in scroll view, so probably needs to be a modal
      (nbc/ui-list {:dataArray    item-array
                    :keyExtractor :value
                    :renderRow    (fn [{:keys [text value]}]
                                    (let [checked? (contains? selected-ids value)]
                                      (nbc/list-item {:key (str value)}
                                        (nbc/checkbox {:checked checked?
                                                       :enabled (not read-only?)
                                                       :onPress (fn []
                                                                  (let [selection (if-not checked?
                                                                                    (conj (set (or selected-ids #{})) value)
                                                                                    (disj selected-ids value))]
                                                                    (form/input-changed! env qualified-key selection)))})
                                        (nbc/body {} text))))}))))

(defn- render-to-one [{::form/keys [form-instance] :as env} {::form/keys [field-label]
                                                             ::attr/keys [qualified-key] :as attribute}]
  (when (form/field-visible? form-instance attribute)
    (let [props      (comp/props form-instance)
          read-only? (form/read-only? form-instance attribute)
          invalid?   (validation/invalid-attribute-value? env attribute)
          user-props (form/field-style-config env attribute :input/props)
          options    (enumerated-options env attribute)
          value      (get props qualified-key)
          label      (str (or field-label (some-> qualified-key name str/capitalize))
                       (when invalid? " (Required)"))]
      (if read-only?
        (let [value (first (filter #(= value (:value %)) options))]
          (nbc/text {} (:text value)))
        (ui-wrapped-dropdown (merge
                               {:disabled    read-only?
                                :placeholder label
                                :label       label
                                :options     options
                                :value       value
                                :onChange    (fn [v] (form/input-changed! env qualified-key v))}
                               user-props))))))

(defn render-field [env {::attr/keys [cardinality] :or {cardinality :one} :as attribute}]
  (if (= :many cardinality)
    (render-to-many env attribute)
    (render-to-one env attribute)))

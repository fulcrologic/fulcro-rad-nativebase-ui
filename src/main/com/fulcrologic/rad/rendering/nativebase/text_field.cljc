(ns com.fulcrologic.rad.rendering.nativebase.text-field
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.ui-validation :as validation]
    [com.fulcrologic.rad.rendering.nativebase.components :refer [ui-wrapped-dropdown]]
    [com.fulcrologic.rad.rendering.nativebase.field :refer [render-field-factory]]))

(defn- with-handlers [{:keys [value onChange onBlur] :as props}]
  (assoc props
    :value (or value "")
    :onBlur (fn [] (when onBlur (onBlur value)))
    :onChangeText (fn [txt] (when onChange (onChange txt)))))

(defn- text-input [props] (nbc/input (with-handlers props)))
(defn- password-input [{:keys [value onChange onBlur] :as props}] (nbc/input (with-handlers (assoc props :secureTextEntry true))))

(defsc ViewablePasswordField [this {:keys [value onChange onBlur] :as props}]
  {:initLocalState (fn [_] {:hidden? true})}
  (let [hidden? (comp/get-state this :hidden?)]
    (nbc/input (assoc props
                 :value (if hidden? "*******" (or value ""))
                 :type "text"
                 :onEndEditing (fn []
                                 (comp/set-state! this {:hidden? true})
                                 (when onBlur (onBlur value)))
                 :onFocus (fn [_] (comp/set-state! this {:hidden? false}))
                 :onChangeText (fn [txt] (when onChange (onChange txt)))))))

(def render-field (render-field-factory text-input))
(def render-password (render-field-factory password-input))
(def render-viewable-password (render-field-factory (comp/factory ViewablePasswordField)))

(defn render-dropdown [{::form/keys [form-instance] :as env} attribute]
  (let [{k           ::attr/qualified-key
         ::attr/keys [required?]} attribute
        values             (form/field-style-config env attribute :sorted-set/valid-values)
        input-props        (form/field-style-config env attribute :input/props)
        options            (mapv (fn [v] {:text v :value v}) values)
        props              (comp/props form-instance)
        value              (and attribute (get props k))
        invalid?           (not (contains? values value))
        validation-message (when invalid? (validation/validation-error-message env attribute))
        field-label        (form/field-label env attribute)
        read-only?         (form/read-only? form-instance attribute)]
    (ui-wrapped-dropdown
      (merge
        {:disabled    read-only?
         :label       field-label
         :placeholder field-label
         :options     options
         :clearable   (not required?)
         :value       value
         :onChange    (fn [v] (form/input-changed! env k v))}
        input-props))))

(def render-multi-line
  (render-field-factory (fn [{:keys [value onChange onBlur] :as props}]
                          (nbc/textarea (assoc props
                                          :value (or value "")
                                          :rowSpan 5
                                          :onBlur (fn [] (when onBlur (onBlur value)))
                                          :onChangeText (fn [txt] (when onChange (onChange txt))))))))

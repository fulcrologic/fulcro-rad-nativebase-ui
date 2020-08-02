(ns com.fulcrologic.rad.rendering.nativebase.field
  (:require
    [clojure.string :as str]
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.ui-validation :as validation]
    [taoensso.timbre :as log]))

(defn render-field-factory
  "Create a general field factory using the given input factory as the function to call to draw an input."
  ([input-factory]
   (render-field-factory {} input-factory))
  ([addl-props input-factory]
   (fn [{::form/keys [form-instance] :as env} {::attr/keys [type qualified-key] :as attribute}]
     (let [props              (comp/props form-instance)
           value              (or (form/computed-value env attribute)
                                (and attribute (get props qualified-key)))
           invalid?           (validation/invalid-attribute-value? env attribute)
           validation-message (when invalid? (validation/validation-error-message env attribute))
           user-props         (form/field-style-config env attribute :input/props)
           field-label        (form/field-label env attribute)
           visible?           (form/field-visible? form-instance attribute)
           read-only?         (form/read-only? form-instance attribute)
           addl-props         (if read-only? (assoc addl-props :readOnly "readonly") addl-props)]
       (when visible?
         (nbc/input {:key          (str qualified-key)
                     :placeholder  (str
                                     (or field-label (some-> qualified-key name str/capitalize))
                                     ;; TASK: validation display
                                     (when validation-message (str " (" validation-message ")")))
                     :value        value
                     :enabled      (not read-only?)
                     :onEndEditing (fn [] (form/input-blur! env qualified-key value))
                     :onChangeText (fn [v] (form/input-changed! env qualified-key v))}))))))

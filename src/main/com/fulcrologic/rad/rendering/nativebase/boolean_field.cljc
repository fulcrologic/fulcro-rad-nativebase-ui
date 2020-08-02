(ns com.fulcrologic.rad.rendering.nativebase.boolean-field
  (:require
    [com.fulcrologic.rad.rendering.nativebase.raw-controls :as nbc]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.form :as form]))

(defn render-field [{::form/keys [form-instance] :as env} attribute]
  (let [k           (::attr/qualified-key attribute)
        props       (comp/props form-instance)
        user-props  (form/field-style-config env attribute :input/props)
        field-label (form/field-label env attribute)
        read-only?  (form/read-only? form-instance attribute)
        value       (get props k false)]
    (if read-only?
      (nbc/text (str field-label " " (if value "Yes" "No")))
      (nbc/list-item {}
        (nbc/checkbox (merge
                        {:checked (boolean value)
                         :enabled (not (boolean read-only?))
                         :onPress (fn []
                                    (let [v (not value)]
                                      (form/input-blur! env k v)
                                      (form/input-changed! env k v)))}
                        user-props))
        (nbc/body {} field-label)))))


(ns com.fulcrologic.rad.rendering.nativebase.raw-controls
  (:require
    #?@(:cljs [["native-base" :as native-base-package]
               ["react-native-modal-datetime-picker" :default DateTimePicker]
               [goog.object :as gobj]
               ["react-native" :as react-native-package]])
    [com.fulcrologic.fulcro-native.alpha.components :as c]
    [clojure.string :as str]
    [com.fulcrologic.fulcro.components :as comp]
    [taoensso.timbre :as log]
    [taoensso.tufte :as tufte]
    [com.fulcrologic.fulcro.algorithms.do-not-use :as futil]))

(def nb #?(:cljs native-base-package :clj {}))
(def rn #?(:cljs react-native-package :clj {}))

(defn isoget [pkg k]
  #?(:cljs (gobj/get pkg (name k))))

(def ios? #?(:cljs (= "ios" react-native-package/Platform.OS) :clj true))

(defn- remove-separators [s]
  (when s
    (str/replace s #"^[.#]" "")))

(defn- get-tokens [k]
  (re-seq #"[#.]?[^#.]+" (name k)))

(defn- parse
  "Parse CSS shorthand keyword and return vector of prop strings.

  (parse :.klass3.klass1.klass2)
  => [\"klass1\"
      \"klass2\"
      \"klass3\"]"
  [k]
  (if k
    (let [tokens  (get-tokens k)
          classes (->> tokens (clojure.core/filter #(re-matches #"^\..*" %)))]
      (when-not (re-matches #"^(\.[^.#]+|#[^.#]+)+$" (name k))
        (throw (ex-info "Invalid style keyword. It contains something other than classnames and IDs." {:item k})))
      (keep remove-separators classes))
    []))

(defn add-kwprops-to-props
  [props kw]
  (let [kw-strings (parse kw)]
    (let [props (reduce #(assoc %1 %2 true)
                  props
                  kw-strings)]
      #?(:cljs (clj->js props)))))

#_(declare container header item input icon button content
    list list-item checkbox left body right label card card-item
    form spinner separator picker text textarea title subtitle
    date-time-picker radio)

(defn text
  ([str] (text nil str))
  ([props str]
   #?(:cljs (c/create-element (isoget nb :Text) (clj->js props) str))))

(defn b
  "Emits a Text element with font weight set to bold."
  [txt]
  (text {:style {:fontWeight "bold"}} txt))

(defn title
  ([str] (title nil str))
  ([props str]
   #?(:cljs (c/create-element (isoget nb :Title) (clj->js props) str))))

(defn subtitle
  ([str] (subtitle nil str))
  ([props str]
   #?(:cljs (c/create-element (isoget nb :Subtitle) (clj->js props) str))))

;;; Used for profiling performance
(def pdata (tufte/new-pdata))
(def render-stats (atom {}))

(defn react-factory-profiled
  ([js-component-class {:keys [ui-text]}]
   #?(:cljs
      (fn [props & children]
        (do                                                 ;tufte/with-profiling pdata {}
          ;; (swap! render-stats update (gobj/get js-component-class "displayName") (fnil inc 0))
          (do                                               ;tufte/p ::react-factory
            (let [cs (comp/force-children children)
                  c  (first cs)]
              (if (and c ui-text (string? c) (= 1 (count cs)))
                (c/create-element
                  js-component-class
                  (clj->js props)
                  (ui-text c))
                (apply c/create-element
                  js-component-class
                  (clj->js props)
                  cs))))))))
  ([js-component-class]
   (react-factory-profiled js-component-class {:ui-text (fn [child]
                                                          (c/create-element
                                                            (comp/isoget c/rn "Text")
                                                            nil
                                                            child))})))

(comment
  (tufte/with-profiling pdata {}
    (time (dotimes [i 1000]
            (tufte/p ::big-dec
              (str i "10.30")))))

  @@pdata
  (def pdata (tufte/new-pdata))
  (reset! render-stats {})


  (println (tufte/format-pstats @pdata))
  )



(defn react-factory
  ([js-class]
   (react-factory-profiled js-class {:ui-text (fn [child] (text nil child))}))
  ([js-class default-attrs]
   (let [f (react-factory js-class)]
     (fn [props & children]
       (apply f (merge default-attrs props) children)))))

(let [f (react-factory (isoget nb :Button))]
  (defn button
    "* :block boolean Make the button a block button
    "
    ([css props text] (f (add-kwprops-to-props props css) text))
    ([props text] (f props text))))


(def body
  ""
  (react-factory (isoget nb :Body)))
(def card (react-factory (isoget nb :Card)))
(def card-item
  "Known props:

  * :onPress event handler.
  * :header boolean - Makes the item look like a header
  * :footer boolean - Makes the item look like a footer
  * :bordered boolean - Makes the item have a border
  * :button boolean - Makes the card item clickable. Add onPress to handle interaction.

  Use nested NativeBase Body and Text elements for proper content formatting.
  "
  (react-factory (isoget nb :CardItem)))
(def checkbox
  "* :checked
   * :onPress"
  (react-factory (isoget nb :CheckBox) {:hitSlop {:top 10 :bottom 10 :left 10 :right 10}}))
(def radio (react-factory (isoget nb :Radio) {:hitSlop {:top 10 :bottom 10 :left 10 :right 10}}))
(def container (react-factory (isoget nb :Container)))
(def content
  "* :padder boolean - Adds padding to content
   "
  (react-factory (isoget nb :Content)))
(def footer (react-factory (isoget nb :Footer)))
(def footer-tab (react-factory (isoget nb :FooterTab)))
(def tabs (react-factory (isoget nb :Tabs)))
(def tab (react-factory (isoget nb :Tab)))
(def form (react-factory (isoget nb :Form)))
(def header (react-factory (isoget nb :Header)))
(def icon (react-factory (isoget nb :Icon)))
(def input
  "
   * :autoCapitalize boolean
   * :autoCompleteType - One of off, username, password, email, name, tel, street-address, postal-code, cc-number, cc-csc, cc-exp, cc-exp-month, cc-exp-year.
   * :autoCorrect boolean
   * :autoFocus boolean (focuses on initial mount)
   * :clearButtonMode enum('never', 'while-editing', 'unless-editing', 'always') (defaule never)
   * :editable boolean
   * :keyboardType - default number-pad decimal-pad numeric email-address phone-pad
   * :maxLength int
   * :onChangeText (fn [txt])
   * :onEndEditing (fn [])
   * :placeholder string
   * :returnKeyLabel string (android only)
   * :returnKeyType enum('done', 'go', 'next', 'search', 'send', 'none', 'previous', 'default', 'emergency-call', 'google', 'join', 'route', 'yahoo')
   * :textAlign enum('left', 'center', 'right')
   * :value current-value

   "
  #?(:cljs (c/wrap-text-input (react-factory (isoget nb :Input))) ::stub))
(def item (react-factory (isoget nb :Item)))
(def label (react-factory (isoget nb :Label)))
(def left (react-factory (isoget nb :Left)))
(def ui-list
  ":dataArray - Array of things to render
   :renderRow - (fn [element] )
   :keyExtractor (fn [element] string)"
  (react-factory (isoget nb :List)))
(def list-item
  ":button - boolean (false). To navigate on click of a list item.
   :selected - boolean (true). Highlight the selected item.
   :noIndent - boolean (true). Remove margin from left.
   :itemDivider - boolean (false). Show divider.
   :icon - boolean. Format to allow for icon on left.
   :avatar - boolean. Format for an avatar on the item.
   :thumbnail - boolean. Format for an image on the item.
   "
  (react-factory (isoget nb :ListItem)))
(def li
  "Convenience wrapper around list-item."
  (fn [x y] (list-item (left x) (body y))))
(def picker
  "* :selectedValue
   * :onValueChange
   * :placeholder
   "
  (react-factory (isoget nb :Picker)))
(def picker-item
  "* :label
   * :value "
  #?(:cljs (react-factory (.-Item ^js (isoget nb :Picker))) :clj {}))
(def right (react-factory (isoget nb :Right)))
(def separator (react-factory (isoget nb :Separator)))
(def spinner (react-factory (isoget nb :Spinner)))
(def segment (react-factory (isoget nb :Segment)))
(def textarea (react-factory (isoget nb :Textarea)))

;;; These should not have strings wrapped by base/text
(defn h1
  ([str] (h1 nil str))
  ([props str]
   #?(:cljs (c/create-element (isoget nb :H1) (clj->js props) str))))
(defn h2
  ([str] (h2 nil str))
  ([props str]
   #?(:cljs (c/create-element (isoget nb :H2) (clj->js props) str))))
(defn h3
  ([str] (h3 nil str))
  ([props str]
   #?(:cljs (c/create-element (isoget nb :H3) (clj->js props) str))))

(defn heading
  "A header with the specified string as the single body element"
  [title]
  (header {} (body {} title)))

(defn warning-message
  "Creates a red message as the body of a Card."
  [txt]
  (card {}
    (card-item {}
      (body {}
        (text {:style {:color    "red"
                       :fontSize 20}} txt)))))

(defn confirm!
  "Pop a confirm dialog.

  onCancel - optional fn to call on cancel
  onOK - fn to call on OK"
  [{:keys [onCancel onOK title message]}]
  #?(:cljs
     (let [buttons (clj->js
                     [(cond-> {:text "Cancel"}
                        onCancel (assoc :onPress onCancel))
                      (cond-> {:text "OK"}
                        onOK (assoc :onPress onOK))])]
       (react-native-package/Alert.alert (or title "Confirm") (or message "") buttons))))

(defn back-button
  "Renders a back button."
  [props]
  (button (merge {:transparent true} props)
    (icon {:name "arrow-back"})))

(defn add-button
  "Renders an add button."
  [props]
  (button (merge {:transparent true} props)
    (icon {:name "add"})))

(def datetime-picker
  "Create a datetime picker.

  :isVisible - Show the picker
  :onConfirm - (fn [inst])
  :onCancel - (fn [inst])
  "
  #?(:cljs (react-factory DateTimePicker {:timePickerModeAndroid "spinner"
                                          :is24Hour              false})
     :clj  {}))

(def theme
  {:brandPrimary     (if ios? "#007aff" "#3F51B5")
   :brandInfo        (if ios? "#62B1F6" "#62B1F6")
   :brandSuccess     (if ios? "#fcfff5" "#5cb85c")
   :brandDanger      (if ios? "#d9534f" "#d9534f")
   :brandWarning     (if ios? "#f0ad4e" "#f0ad4e")
   :brandDark        (if ios? "#000" "#000")
   :brandLight       (if ios? "#f4f4f4" "#f4f4f4")
   :inverseTextColor "#fff"
   :fontFamily       (if ios? "System" "Roboto_medium")
   :.danger          {:backgroundColor "#fff6f6"
                      :borderRadius    4
                      :borderWidth     1.5
                      :borderColor     "#e0b4b4"}
   :.info            {:backgroundColor "#f8ffff"
                      :borderRadius    4
                      :borderWidth     1.5
                      :borderColor     "#a9d5de"}
   :.success         {:backgroundColor "#fcfff5"
                      :borderRadius    4
                      :borderWidth     1.5
                      :borderColor     "#a3c293"}
   :.warning         {:backgroundColor "#fffaf3"
                      :borderRadius    4
                      :borderWidth     1.5
                      :borderColor     "#c9ba9b"}})

(defn message
  "(message :.danger \"Limit Exceeded\")"
  [kind attrs & msgs]
  (card attrs
    (card-item {:style (theme kind)}
      (c/ui-view {:flexDirection "column"}
        (map-indexed
          (fn [i msg]
            (text {:key   i
                   :style {:fontFamily (theme :fontFamily)}}
              msg))
          msgs)))))

(defn confirm-button
  [this state-key {:keys [attrs text confirm-text onPress]}]
  (if (comp/get-state this state-key)
    (button (merge attrs {:onPress (fn [e]
                                     (comp/set-state! this {state-key false})
                                     (onPress e))})
      (or confirm-text (str "Confirm " text)))
    (button
      (merge attrs
        {:onPress  #(comp/set-state! this {state-key true})
         :bordered true})
      text)))

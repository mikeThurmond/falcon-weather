(ns falcon-sample-thurmond.core
  (:require
   [clojure.string :refer [lower-case]]
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]
   [ajax.core :refer [GET POST]]
   [clojure.walk :refer [keywordize-keys]]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(path-for :about)
;; -------------------------
;; Page components


;;notes
;;preload json from server and use delay on client side
;; -> I wasted time here trying to do just that
;; with a new library in the handler that I am not familiar with
;;use cljs http and core.async with <!

;;;find days with date math or see note below

;add a day column and then convert the "time" or "sunriseTime" key
; from the weather map from an int to the appropriate date time.
; I got hung up trying to convert it in a timely manner.
; I would have brought in moment js for the time conversion

;;add spinner when searching in re-frame modal panel
;;replace spaces with '+'

;;do a better job filtering the list when getting lat longs based on city.
;;I would concat :name and :subcountry.  Issue being if someone for example,
;; types in ID instead of Idaho

;;I should have studies the api docs for dark sky and open cage before starting,
;; it would have saved me some time pulling out the data

;;;;;;;; code ;;;;;;;;;

(def cors-anywhere "https://cors-anywhere.herokuapp.com/")
(def dark-sky-url "https://api.darksky.net/forecast/")
(def dark-sky-key "17f6ff315990ff88ab772f5e6785a2b5/")
(def open-cage-url "https://api.opencagedata.com/geocode/v1/json?q=")
(def open-cage-key "&key=0c521d73226347caab6d5f313d42cfdf")


(defn get-cities [cities]
  (GET "https://www.falconproj.com/code-sample-data/world-cities.json"
    {:handler #(reset! cities (keywordize-keys (js->clj %)))}))


(defn get-weather [lat long weather]
  (GET
    (str cors-anywhere dark-sky-url dark-sky-key lat "," long)
    {:handler #(reset! weather (get-in % ["daily" "data"]))
     :error-handler #(js/alert "Error getting weather data for city")}))


(defn get-geo-data [data]
  (-> (get data "results")
    (first)
    (get "geometry")))


(defn get-lat-longs [city cities weather message]
  (reset! message false)
  (reset! weather nil)
  (if (some #(= (lower-case @city) (lower-case (:name %))) @cities)
    (GET
      (str open-cage-url @city open-cage-key)
      {:handler       (fn [resp]
                        (let [geo-data (get-geo-data resp)]
                          (get-weather (get geo-data "lat") (get geo-data "lng") weather)))
       :error-handler #(js/alert "Error getting geo data for city")})
    (reset! message true)))


(defn input-element [value]
  [:input {:class          "form-control"
           :type           "text"
           :placeholder    "City"
           :value          @value
           :on-change      #(reset! value (-> % .-target .-value))}])


(defn home-page []
  (let [cities (atom nil)
        _ (get-cities cities)
        city (atom nil)
        weather (atom nil)
        message (atom nil)]
    (fn []
      [:span.main
       [:h1 "Weather Summary"]
       [:form
        [input-element city]
        [:br]
        [:input
         {:type  "button"
          :value "Search"
          :on-click #(get-lat-longs city cities weather message)
          :disabled (nil? @cities)}]
        [:br] [:br]
        (when @message [:label "City not valid"])
        (when @weather
          [:table.table.table-striped.table-bordered
           [:thead
            [:tr
             [:th "Summary"]
             [:th "Max Temperature"]
             [:th "Min Temperature"]
             [:th "Precipitation Chance"]]]
           [:tbody
            (doall (for [day @weather]
                     ^{:key (get day "time")}
                     [:tr
                      [:td (get day "summary")]
                      [:td (get day "temperatureMax")]
                      [:td (get day "temperatureMin")]
                      [:td (get day "precipProbability")]]))]])]])))

;;;;;;;; end code ;;;;;;;;;;

(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of falcon-sample-thurmond"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))


(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of falcon-sample-thurmond")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))


(defn about-page []
  (fn [] [:span.main
          [:h1 "About falcon-sample-thurmond"]]))


;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:p [:a {:href (path-for :index)} "Home"] " | "
         [:a {:href (path-for :about)} "About falcon-sample-thurmond"]]]
       [page]
       [:footer
        [:p "falcon-sample-thurmond was generated by the "
         [:a {:href "https://github.com/reagent-project/reagent-template"} "Reagent Template"] "."]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))

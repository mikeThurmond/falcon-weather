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

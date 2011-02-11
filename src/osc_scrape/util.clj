(ns osc-scrape.util
  (:require [osc-scrape.templates :as temp]
	    [clojure.string :as str]
	    [net.cgrand.enlive-html :as e])
  (:use hiccup.core
	hiccup.page-helpers
	evalive.core)
  (:gen-class))

(def *site-title* "National Commission on the BP Deepwater Horizon Oil Spill and Offshore Drilling")

(defn has-extension
  "Function factory, creating boolean functions that verify if a string is a filename with an extension of `type`"
  [type]
  (fn [s]
    (let [t (name type)]
      (if
	  (not (empty? (re-find (re-pattern (str "." t "$")) s)))
	true
	false))))

(defn points-to-file?
  "Returns true if string `s` represents a file name. Optional `ext` param specifies type of file extension."
  ([s] (if (boolean (re-find #"\.[^/]*$" s))
	 true
	 false))
  ([s ext] (let [ext? (has-extension ext)]
	     (if (and
		  (points-to-file? s)
		  (ext? s))
	       true
	       false))))

(defn pattern-from-url
  "Generate a regex pattern suitable for `re-find` from a string, escape characters special to regexes commonly found in urls"
  [url]
  (re-pattern (str "^" (str/replace url "." "\\."))))

(defn remove-url-fragment
  "Remove the # fragment from a url"
  [url]
  (str/replace url #"#.*$" ""))

(defn get-final-path-arg
  "Get the last part of a URL path"
  [path]
  (second (re-find #"/([^/]*)$" path)))

(defn clean-title [title]
  (if (= title (str *site-title* " | "))
    *site-title*
    (str/replace title (str " | " *site-title*) "")))

;;; Functions on @outline
(defn has-docs?
  "Verify whether or not an outline record has document links"
  [e]
  (not (empty? (:doc-links (val e)))))

(defn total-docs [outline]
  (reduce + (for [[k v] outline] (count (:doc-links v)))))

;;; HTML generation

(defn outline-detail-to-html
  "Create an HTML string representation of `outline`"
  [outline]
  (html
   [:ul
    (for [[k v] outline]
      [:li
       [:strong "URL: "]
       k
       [:ul
	[:li
	 [:strong "Page Title: "]
	 (clean-title (:page-title v))]
	[:li
	 [:strong (str "PDF Documents (" (count (:doc-links v)) "):")]
	 [:ul
	  (for [enlive-node (doall (:doc-links v))]
	    [:li
	     [:a {:href (get-in enlive-node [:attrs :href])}
	      (str/replace
	       (get-final-path-arg (get-in enlive-node [:attrs :href]))
	       "%20"
	       " ")]])]]]])]))

(defn outline-summary-to-html [outline]
  (html
   [:p
    [:ul
     [:li
      [:strong "Total PDF Documents: "]
      (total-docs outline)]
     [:li
      [:strong "Total Number of Pages with PDF's: "]
      (count outline)]]]))

(defn generate-summary [outline]
  (temp/summary (e/html-snippet (outline-summary-to-html outline))))
   
(defn generate-detail [outline]
  (temp/detail (e/html-snippet (outline-detail-to-html outline))))

(defn write-report
  "Generate final HTML report from outline data structure"
  [outline]
  (let [final-outline (->> @outline
			   (filter has-docs?)
			   (sort))
	summary-nodes (generate-summary final-outline)
	detail-nodes (generate-detail final-outline)
	content-nodes (flatten (conj detail-nodes summary-nodes))
	report-page (temp/page content-nodes)
	output (apply str report-page)]
    (spit "outline.html" output)))

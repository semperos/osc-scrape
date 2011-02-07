(ns osc-scrape.util
  (:require [clojure.contrib.str-utils2 :as str]
	    [net.cgrand.enlive-html :as e])
  (:use hiccup.core
	hiccup.page-helpers)
  (:gen-class))

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
  
(defn outline-to-html-list
  "Create an HTML representation of `outline`"
  [m]
  (html
   [:body
    [:h1 "PDF Documents on OilSpillCommission.gov"]
    [:ul
     (for [[k v] m]
       [:li
  	[:strong "URL: "]
  	k
  	[:ul
  	 [:li
  	  [:strong "Page Title: "]
  	  (:page-title v)]
  	 [:li
  	  [:strong "PDF Documents:"]
  	  [:ul
  	   (for [enlive-node (doall (:doc-links v))]
  	     [:li
  	      [:a {:href (get-in enlive-node [:attrs :href])}
  	       (str/replace
  	   	(get-final-path-arg (get-in enlive-node [:attrs :href]))
  	   	"%20"
  	   	" ")]])]]]])]]))

(defn has-docs?
  "Verify whether or not an outline record has document links"
  [e]
  (not (empty? (:doc-links (val e)))))

(defn gen-report
  "Generate final HTML report from outline data structure"
  [outline]
  (let [final-outline (->> @outline
			   (filter has-docs?)
			   (sort))
	output (outline-to-html-list final-outline)]
    (spit "outline.html" output)))
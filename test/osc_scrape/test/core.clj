;; Tests for OSC Scrape Utility
;;
;; A "page" var storing the Watir Example Google doc form as a set of Enlive nodes
;; is already bound in the osc-scrape.test.var namespace as "page".
;;

(ns osc-scrape.test.core
  (:require [osc-scrape.test.var :as var])
  (:use [osc-scrape core util] :reload)
  (:use clojure.test
	midje.sweet))

(fact
  "Get internal nodes"
  (let [link-nodes (select-internal-link-nodes "http://www.oilspillcommission.gov/" var/page)]
    (count link-nodes) => 53))

(fact
  "Get page title"
  (select-page-title var/page) => (just "National Commission on the BP Deepwater Horizon Oil Spill and Offshore Drilling | "))

(facts
 "Build full URL's from partial or full URL's"
 (let [f (partial build-full-url "http://www.oilspillcommission.gov/resources")]
   (f "/sites/default/files") => (just "http://www.oilspillcommission.gov/sites/default/files")
   (f "/news#media-advisories") => (just "http://www.oilspillcommission.gov/news")
   (f "docs/document.pdf") => (just "http://www.oilspillcommission.gov/resources/docs/document.pdf")
   (f "/sites/default/files") => (just "http://www.oilspillcommission.gov/sites/default/files")
   (f "http://www.oilspillcommission.gov/") => (just "http://www.oilspillcommission.gov/")))

(fact
  "Filter vector of URL nodes for ones we want to visit"
  (let [urls (select-internal-link-nodes "http://www.oilspillcommission.gov/" var/page)
	current-url "http://www.oilspillcommission.gov/"]
    (take 5 (filter-urls-to-visit current-url urls)) => (seq ["http://www.oilspillcommission.gov/" "http://www.oilspillcommission.gov/page/about-commission" "http://www.oilspillcommission.gov/page/commission-members" "http://www.oilspillcommission.gov/document/charter" "http://www.oilspillcommission.gov/staff"])))

(facts
 "Prepare doc entries for use in outline"
 (let [doc-entries (prepare-doc-entries "http://www.oilspillcommission.gov" (select-internal-link-nodes "http://www.oilspillcommission.gov/" var/page))]
    (count doc-entries) => 2 ; two known pdf's on example page
    (map #(boolean (->> (:attrs %)
			:href
			(re-find #"\.pdf$"))) ; ensure URL ends with .pdf
	 doc-entries) => [true true]
	 (map #(boolean (->> (:attrs %)
			     :href
			     (re-find #"/\w+$"))) ; ensure URL does not just end with /words-here
	      doc-entries) => [false false]))

(fact
  "Preparing url entries should produce a certain data structure"
  (prepare-url-entries ["http://www.oilspillcommission.gov/" "http://www.oilspillcommission.gov/resources"]) => {"http://www.oilspillcommission.gov/resources" {:status :inactive, :result :to-read}, "http://www.oilspillcommission.gov/" {:status :inactive, :result :to-read}})

(fact
  "Getting a url entry from scrape urls can be accomplished by passing in a result state"
  (let [url-entries {"http://www.oilspillcommission.gov/resources" {:status :inactive, :result :to-read}, "http://www.oilspillcommission.gov/" {:status :inactive, :result :been-read}}]
    (get-scrape-url url-entries :to-read) => "http://www.oilspillcommission.gov/resources"
    (get-scrape-url url-entries :been-read) => "http://www.oilspillcommission.gov/"
    (get-scrape-url url-entries :foobar) => nil))
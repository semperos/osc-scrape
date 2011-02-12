;; ## Recursive Scrape Utility for OilSpillCommission.gov
;;
;; This program scrapes the entire OSC website recursively, beginning with the
;; home-page and working through every link on the website. Any materials that
;; have been published to the website but for which exist no direct links will
;; not be detected, as this is a link-based scrape.
;;
;; After the scrape is complete, a file `outline.html` is auto-generated which
;; lists all PDF documents found on the site, grouped by web page. The web pages
;; are ordered alphabetically by URL, to provide some sense of structure.
;;
;; If you run this utility from the REPL, you will also have the `outline` atom
;; available to manipulate in any way you see fit.

(ns osc-scrape.core
  (:require [net.cgrand.enlive-html :as e]
	   [clojure.contrib.str-utils2 :as str])
  (:use osc-scrape.util)
  (:gen-class :main true))

;; ## Global vars and program state
;;
;; The `scrape-urls` atom stores all known URL's scraped from links, keeping
;; track of which ones have been read and which ones need to be read.
;;
;; The `outline` atom is a nested map structure which collects page url's,
;; titles, and all of the nodes returned from an Enlive scrape of the page for
;; links.

(def *base-url* "http://www.oilspillcommission.gov/")
(def scrape-urls (atom {*base-url* { :status :inactive, :result :to-read }}))
(def outline (atom {}))

;; ## Enlive-based functions

(defn fetch-url
  "Return a set of Enlive nodes from the source at url"
  [url]
  (e/html-resource (java.net.URL. url)))

(defn select-internal-link-nodes
  "Return all nodes from the page that are links pointing to internal url's."
  [node-or-nodes]
  (e/select node-or-nodes [[:a (or
				(e/attr-starts :href "/")
				(e/attr-starts *base-url*))]]))

(defn select-page-title
  "Retrieve the page's title from the `head` tag"
  [node-or-nodes]
  (->> [:head :title]
       (e/select node-or-nodes)
       first
       :content
       (apply str)))

;; ## Data structure manipulation utilities

(defn build-full-url
  "Remove URL fragments and make sure URL string is a fully-qualified URL string"
  [current-url url]
  (let [base-path "http://www.oilspillcommission.gov/"
	url (remove-url-fragment url)]
    (if-not (boolean (re-find #"^http://" url))
      (cond
       (= \/ (first url)) (str (second (re-find #"(.*?)/$" base-path)) url)
       (= \/ (last url)) (str current-url url)
       :else (str current-url "/" url))
      (if (= "http://www.oilspillcommission.gov" url)
	base-path
	url))))

(defn prepare-pdf-entries
  "Prepare pdf links scraped from a page to be inserted into our outline data structure"
  [link-nodes current-url]
  (let [build-url (partial build-full-url current-url)]
    (->> (map #(update-in % [:attrs :href] build-url) link-nodes)
	 (filter #(points-to-file? (get-in % [:attrs :href]) :pdf)))))

(defn prepare-url-entries
  "Create a map by zipping up new URL's with a default map instance for inactive, unread URL entries"
  [v]
  (zipmap v (repeat {:status :inactive, :result :to-read})))
			    
(defn get-scrape-url
  "Get a URL from the scrape-urls data structure. Result is either :to-read or :been-read"
  [url-entries result]
  (first (for [[k v] url-entries :when (= (:result v) result)] k)))

(defn filter-urls-to-visit
  "Convert link nodes to just url strings, filter out urls that we won't/can't visit"
  [links]
  (->> (map #(get-in % [:attrs :href]) links)
       (remove points-to-file?)
       (map remove-url-fragment)
       (remove empty?)
       (map build-full-url)))

;; ## Functions that deal with state

(defn add-pdfs-to-record!
  "Add pdfs to record for particular URL"
  [outline current-url all-nodes]
  (let [link-nodes (select-internal-link-nodes all-nodes)
	page-title (select-page-title all-nodes)	
	pdf-links (prepare-pdf-entries link-nodes current-url)]
    (if (> (count pdf-links) 0)
      (do
	(swap! outline
	       assoc-in [current-url :page-title] page-title)
	(swap! outline
	       assoc-in [current-url :doc-links] pdf-links))
      (println "No documents for this page."))))

(defn update-scrape-urls!
  "Remove current url from to-visit and add new URL's"
  [scrape-urls current-url all-nodes]
  (let [new-urls (filter-urls-to-visit (select-internal-link-nodes all-nodes))
	new-urls-map (prepare-url-entries new-urls)]
    (swap! scrape-urls assoc-in [current-url] {:status :inactive, :result :been-read})
    (swap! scrape-urls #(merge %2 %1) new-urls-map)))

(defn scrape-document-info!
  "Scrape document info from current page defined in driver"
  [outline scrape-urls]
  (let [current-url (get-scrape-url @scrape-urls :to-read)
	all-nodes (fetch-url current-url)]
    (println (str "Scraping page: " current-url))
    (add-pdfs-to-record! outline current-url all-nodes)
    (update-scrape-urls! scrape-urls current-url all-nodes)))

;; ## Start the Scrape

(defn start-scrape
  "Start recursive scrape of entire website"
  [outline scrape-urls]
  (loop [outline outline scrape-urls scrape-urls]
    (scrape-document-info! outline scrape-urls)
    (if (nil? (get-scrape-url @scrape-urls :to-read))
      (do
	(println "Scrape complete! Check the outline atom for full results.")
	(view-report outline scrape-urls))
      (recur outline scrape-urls))))
  
(defn -main
  "Main function for AOT compilation"
  []
  (start-scrape outline scrape-urls))
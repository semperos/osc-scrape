(ns osc-scrape.util
  (:refer-clojure :exclude [extend])
  (:require [osc-scrape.templates :as temp]
	    [clojure.string :as str]
            [clojure.contrib.duck-streams :as ds]
	    [net.cgrand.enlive-html :as e])
  (:use clojure.java.browse
	hiccup.core
	hiccup.page-helpers
	clj-time.core
	clj-time.format
	evalive.core)
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

(defn unique-path
  "Get non-base-url portion of a URL"
  [url]
  (->>
   (-> #"^(http|https)://[^/]*(/.*?)$"
              (re-find url)
              (nth 2))
   (drop 1)
   (apply str)))

(defn clean-path-for-fs
  "Remove URL-y stuff that we don't want in a path name in a filesystem"
  [path]
  (str/replace path "%20" " "))

(defn final-path
  "Get the last part of a URL path and strip %20 spaces"
  [path]
  (if-let [final-path (second (re-find #"/([^/]*)$" path))]
    (str/replace final-path "%20" " ")
    (str/replace path "%20" " ")))

(defn clean-title [title]
  (let [site-title "National Commission on the BP Deepwater Horizon Oil Spill and Offshore Drilling"]
    (if (= title (str site-title " | "))
      site-title
      (str/replace title (str " | " site-title) ""))))

(defn local-now [fmt]
  (.toString (now) (->> "America/New_York"
			time-zone-for-id
			(with-zone fmt))))

(defn log!
  "Used to log time"
  ([] (log! (str (local-now (formatters :rfc822)) "\n")))
  ([s] (ds/append-spit "log.txt" (str s "\n"))))

(defn pdf-page-percentage [outline scrape-urls]
  (->> (/ (count outline) (count scrape-urls))
       float
       (* 100)
       Math/ceil
       int))

(defn group-by-extension
  "Group a list of strings according to file extension"
  [ss]
  (group-by #(.toLowerCase (second (re-find #"\.(\w+)$" %))) ss))

;;; Functions on @outline
(defn has-docs?
  "Verify whether or not an outline record has document links"
  [e]
  (not (empty? (:doc-links (val e)))))

(defn total-docs [outline]
  (reduce + (for [[k v] outline] (count (:doc-links v)))))

(defn get-doc-urls [outline-entry]
  (let [doc-nodes (doall (:doc-links outline-entry))]
    (map #(get-in % [:attrs :href]) doc-nodes)))


;;; HTML generation

(defn outline-summary-to-html [outline scrape-urls]
  (html
   [:p
    [:ul
     [:li
      [:strong "Date Generated: "]
      (local-now (formatters :rfc822))]
     [:li
      [:strong "Total Documents: "]
      (total-docs outline)]
     [:li
      [:strong "Total Number of Pages on Site: "]
      (count scrape-urls)]
     [:li
      [:strong "Total Number of Pages with Documents: "]
      (str
       (count outline)
       " (" (pdf-page-percentage outline scrape-urls) "% of site)")]]]))

(defn outline-detail-to-html
  "Create an HTML string representation of `outline`"
  [outline]
  (html
   [:ul
    (for [[k v] outline]
      [:li [:strong "URL: "] k
       [:ul
	[:li [:strong "Page Title: "] (clean-title (:page-title v))]
	[:li [:strong (str "Documents (" (count (:doc-links v)) "):")]
	 [:ul
	  (let [doc-urls (get-doc-urls v)
		docs-by-group (sort (group-by-extension doc-urls))]
	    (for [doc-type docs-by-group]
	      [:li (str (-> (key doc-type) .toUpperCase)
			" (" (count (val doc-type)) ")")
	       (let [docs-as-links (map (fn [url] [:a {:href url}
						   (final-path url)])
					(val doc-type))]
		 (unordered-list docs-as-links))]))]]]])]))

(defn generate-summary [outline scrape-urls]
  (temp/summary (e/html-snippet (outline-summary-to-html outline scrape-urls))))

(defn generate-detail [outline]
  (temp/detail (e/html-snippet (outline-detail-to-html outline))))

(defn write-report
  "Generate final HTML report from outline data structure"
  [outline scrape-urls]
  (let [final-outline (->> outline
			   (filter has-docs?)
			   (sort))
	summary-nodes (generate-summary final-outline scrape-urls)
	detail-nodes (generate-detail final-outline)
	content-nodes (flatten (conj detail-nodes summary-nodes))
	report-page (temp/page content-nodes)
	output (apply str report-page)]
    (spit "outline.html" output)))

(defn view-report
  [outline scrape-urls]
  (do
    (write-report @outline @scrape-urls)
    (browse-url "outline.html")))

(defn fetch-url-data
  "Fetch binary data at the given url"
  [^String url out-file]
  (let  [con    (-> url java.net.URL. .openConnection)
         fields (reduce (fn [h v]
                          (assoc h (.getKey v) (into [] (.getValue v))))
                        {} (.getHeaderFields con))
         size   (first (fields "Content-Length"))
         in     (java.io.BufferedInputStream. (.getInputStream con))
         out    (java.io.BufferedOutputStream.
                 (java.io.FileOutputStream. out-file))
         buffer (make-array Byte/TYPE 1024)]
    (loop [g (.read in buffer)
           r 0]
      (if-not (= g -1)
        (do
          (.write out buffer 0 g)
          (recur (.read in buffer) (+ r g)))))
    (.close in)
    (.close out)
    (.disconnect con)))
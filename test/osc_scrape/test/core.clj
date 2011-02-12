(ns osc-scrape.test.core
  (:use [osc-scrape core util] :reload)
  (:use [clojure.test]))

(deftest get-page-title
  (is (= "OSC Scrape" (select-page-title
		       {:tag :html, :attrs nil, :content[
							 {:tag :head, :attrs nil, :content [
											    {:tag :title, :attrs nil, :content ["OSC Scrape"]}]}
							 {:tag :body, :attrs nil, :content nil}]}))))

(deftest make-full-url
  (are [part full] (= full (build-full-url "http://www.oilspillcommission.gov/resources" part))
       "/sites/default/files"                       "http://www.oilspillcommission.gov/sites/default/files"
       "/news#media-advisories"                     "http://www.oilspillcommission.gov/news"
       "docs/document.pdf"                          "http://www.oilspillcommission.gov/resources/docs/document.pdf"
       "http://www.oilspillcommission.gov/"         "http://www.oilspillcommission.gov/"
       "http://www.oilspillcommission.gov#foobar"   "http://www.oilspillcommission.gov/"))
       
		     
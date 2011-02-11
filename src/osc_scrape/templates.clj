(ns osc-scrape.templates
  (:use net.cgrand.enlive-html))

(defsnippet summary "summary.html" [:body :> any-node]
  [cnt]
  [:div.content] (content cnt))

(defsnippet detail "detail.html" [:body :> any-node]
  [cnt]
  [:div.content] (content cnt))

(deftemplate page "page.html" [cnt]
  [:div#main-content] (content cnt))
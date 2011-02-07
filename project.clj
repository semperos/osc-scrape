(defproject osc-scrape "1.0.0-SNAPSHOT"
  :description "Code for Scraping OSC Website"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [enlive "1.0.0-SNAPSHOT"]
		 [hiccup "0.3.4"]]
  :dev-dependencies [[marginalia "0.5.0-alpha"]]
  :main osc-scrape.core)

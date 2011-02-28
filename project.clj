(defproject osc-scrape "0.1.0"
  :description "Code for Scraping OSC Website"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [clj-time "0.3.0-SNAPSHOT"]
                 [clj-file-utils "0.2.1"]
		 [enlive "1.0.0-SNAPSHOT"]
		 [hiccup "0.3.4"]]
  :dev-dependencies [[midje "1.1-alpha-1"]
		     [marginalia "0.5.0-alpha"]
		     [evalive "1.0.0"]
		     [swank-clojure "1.3.0-SNAPSHOT"]]
  :main osc-scrape.core)

#_ :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"}
#_ [com.stuartsierra/lazytest "1.1.2"]
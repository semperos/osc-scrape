(ns osc-scrape.file
  (:require [clojure.string :as str])
  (:use osc-scrape.util
        clj-file-utils.core))

(defn url-encode
  "Encode a string to URL-formatted string"
  [s]
  (let [protocol (second (re-find #"^(http|https)://" s))
        base (nth (re-find #"^(http|https)://([^/]*)/" s) 2)
        path (str "/" (unique-path s))] ; unique-path excludes / after base
    (if (re-find #"%" path)
      path ; it's already encoded
      (.toString (java.net.URI. protocol base path nil)))))

(defn download-and-save-file
  "Download an individual file and save under a directory to match the page the link for that file appears on."
  [page-url target-url]
  (if (= page-url "http://www.oilspillcommission.gov/")
    (download-and-save-file "http://www.oilspillcommission.gov/site_front_page" target-url)
    (let [path (unique-path page-url)
          f (->> target-url
                 final-path
                 (str path "/")
                 clean-path-for-fs
                 file)]
      (mkdir-p path)
      (if (exists? f)
        nil
        (do
          (println (str "Saving file for " target-url))
          (try (fetch-url-data (url-encode target-url) f)
               (catch java.io.FileNotFoundException e
                 (prn (str "File not found: " e)))))))))

(defn download-files!
  "Run through all of the scraped document URL's for a given page and download and save the files."
  [outline current-url]
  (doseq [doc-url (get-doc-urls (@outline current-url))]
    (download-and-save-file current-url doc-url)))

#_"5. Nice-to-have: Use iText on locally-stored PDF's to scrape even more metadata"
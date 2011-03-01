(ns osc-scrape.gui
  (:use [osc-scrape core util])
  (:import [javax.swing
            JFrame JPanel JButton
            JRadioButton ButtonGroup JLabel
            JOptionPane JProgressBar SwingWorker]
           net.miginfocom.swing.MigLayout)
  (:gen-class
   :main true))

(defmacro on-action
  "Macro to add action listeners to Swing components"
  [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))

;;; Component Definitions
(def btn-run (JButton. "Run!"))
(def radio-btn-scrape (JRadioButton. "Scrape" true))
(def radio-btn-scrape-and-dl (JRadioButton. "Scrape & Download"))
(def btn-group (doto (ButtonGroup.)
                 (.add radio-btn-scrape)
                 (.add radio-btn-scrape-and-dl)))
(def lbl-status (JLabel. "Status: Not Running"))
(def pb (doto (JProgressBar. 0 1)
          (.setStringPainted false)
          (.setValue 0)))

;;; Actions
(on-action btn-run event
           (let [start-scrape-and-dl (.isSelected radio-btn-scrape-and-dl)]
             (if (true? start-scrape-and-dl)
               (.setText lbl-status "RUNNING SCRAPE & DOWNLOAD, please be very patient.")
               (.setText lbl-status "RUNNING SCRAPE, please be patient."))
             (.setValue pb 0)
             (.setIndeterminate pb true)
             (.setEnabled btn-run false)
             (.setEnabled radio-btn-scrape false)
             (.setEnabled radio-btn-scrape-and-dl false)
             (doto (proxy [SwingWorker] []
                            (doInBackground []
                              (if (true? start-scrape-and-dl)
                                (start-scrape true)
                                (start-scrape false)))
                            (done []
                              (.setIndeterminate pb false)
                              (.setValue pb 1)
                              (.setEnabled btn-run true)
                              (.setEnabled radio-btn-scrape true)
                              (.setEnabled radio-btn-scrape-and-dl true)
                              (.setText lbl-status "Status: COMPLETE")))
               (.execute))))

;;; Layout
(def layout (MigLayout. "wrap 3", "[][]"))
(def pnl-main (doto (JPanel. layout)
                (.add btn-run)
                (.add radio-btn-scrape)
                (.add radio-btn-scrape-and-dl)
                (.add pb "span 3, growx, gaptop 15")
                (.add lbl-status "span 3, growx, gaptop 15")))

(def frame (doto (JFrame. "OSC Site Scraper")
             (.setSize 370 150)
             (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
             (.setContentPane pnl-main)))

(defn- create-and-show-gui []
  (.setVisible frame true))

(defn -main
  []
  (javax.swing.SwingUtilities/invokeLater create-and-show-gui))
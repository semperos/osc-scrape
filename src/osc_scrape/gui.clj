(ns osc-scrape.gui
  (:use [osc-scrape core util])
  (:import [javax.swing
            JFrame JPanel JButton
            JCheckBox JLabel
            JOptionPane SwingWorker]
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
(def btn-run (JButton. "Run Scrape"))
(def chkbox-scrape (doto (JCheckBox. "Scrape")
                      (.setSelected true)))
(def chkbox-scrape-and-dl (JCheckBox. "Scrape & Download"))
(def lbl-status (JLabel. "Status: (Not Running)"))

;;; Actions
(on-action btn-run event
           (let [statuses (map #(.isSelected %) [chkbox-scrape chkbox-scrape-and-dl])]
             (if (every? false? statuses)
               (JOptionPane/showMessageDialog
                nil
                "You must select at least one scrape option before running the scrape"
                "Scrape Options"
                JOptionPane/ERROR_MESSAGE)
               (let [sw (doto (proxy [SwingWorker] []
                       (doInBackground []
                         (.setText lbl-status "Status: RUNNING SCRAPE, please be patient.")
                         (if (true? (.isSelected chkbox-scrape-and-dl))
                           (start-scrape outline scrape-urls true)
                           (start-scrape outline scrape-urls false)))
                       (done []
                         (.setText lbl-status "Status: COMPLETE"))))]
                 (.execute sw)))))

;;; Layout
(def layout (MigLayout. "wrap 3", "[][]"))
(def pnl-main (doto (JPanel. layout)
                (.add btn-run)
                (.add chkbox-scrape)
                (.add chkbox-scrape-and-dl)
                (.add lbl-status "span 3, growx, gaptop 15")))
(def frame (doto (JFrame. "OSC Site Scraper")
             (.setSize 370 130)
             (.setContentPane pnl-main)))

(defn- create-and-show-gui []
  (.setVisible frame true))

(defn -main
  []
  (javax.swing.SwingUtilities/invokeLater create-and-show-gui))
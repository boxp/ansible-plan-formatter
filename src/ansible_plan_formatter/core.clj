(ns ansible-plan-formatter.core
    "Entry point for ansible-plan-formatter CLI."
    (:require [ansible-plan-formatter.formatter :as fmt]
              [ansible-plan-formatter.parser :as parser]
              [clojure.string :as str]
              [clojure.tools.cli :as cli])
    (:gen-class))

(def ^:private cli-options
     [["-i" "--input FILE" "Input file (default: stdin)"]
      ["-n" "--node NAME" "Node name label"]
      ["-p" "--playbook NAME" "Playbook name label"]
      ["-h" "--help" "Show help"]])

(defn- usage
  "Generate usage message."
  [summary]
  (str/join
   "\n"
   ["Usage: ansible-plan-formatter [OPTIONS]"
    "" "Options:" summary ""
    "Exit codes:" "  0 - No changes or failures"
    "  2 - Changes or failures detected"
    "  1 - Tool error"]))

(defn- read-input
  "Read input from file or stdin."
  [input-file]
  (if input-file
    (slurp input-file)
    (slurp *in*)))

(defn- parse-input
  "Parse input and extract host data."
  [{:keys [input node playbook]}]
  (let [parsed (parser/parse-json (read-input input))
        hostname (or node
                     (first (parser/extract-hostnames
                             parsed)))]
    {:parsed parsed
     :hostname hostname
     :playbook-name (or playbook "playbook")}))

(defn- format-and-print
  "Format output and return exit code."
  [parsed hostname playbook-name]
  (let [stats (parser/extract-stats parsed)
        changed (parser/extract-changed-tasks
                 parsed hostname)
        failed (parser/extract-failed-tasks
                parsed hostname)
        output (fmt/format-output
                hostname playbook-name
                stats changed failed)]
    (println output)
    (if (fmt/has-changes? changed failed) 2 0)))

(defn- run
  "Main logic: parse input and format output."
  [options]
  (let [{:keys [parsed hostname playbook-name]}
        (parse-input options)]
    (format-and-print
     parsed hostname playbook-name)))

(defn- handle-help
  "Show help and exit."
  [summary]
  (println (usage summary))
  (System/exit 0))

(defn- handle-errors
  "Show errors and exit."
  [errors]
  (doseq [e errors] (println e))
  (System/exit 1))

(defn- run-with-exit
  "Run and exit with appropriate code."
  [options]
  (try
    (System/exit (run options))
    (catch Exception e
      (binding [*out* *err*]
        (println "Error:" (.getMessage e)))
      (System/exit 1))))

(defn -main
  "CLI entry point."
  [& args]
  (let [{:keys [options errors summary]}
        (cli/parse-opts args cli-options)]
    (cond
      (:help options) (handle-help summary)
      errors (handle-errors errors)
      :else (run-with-exit options))))

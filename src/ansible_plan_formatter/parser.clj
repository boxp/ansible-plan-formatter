(ns ansible-plan-formatter.parser
    "Parses Ansible JSON callback output into structured data."
    (:require [clojure.data.json :as json]))

(defn parse-json
  "Parse JSON string into Clojure data structure."
  [json-str]
  (json/read-str json-str :key-fn keyword))

(defn- extract-host-result
  "Extract result for a specific host from a task."
  [task hostname]
  (get-in task [:hosts (keyword hostname)]))

(defn- task-changed?
  "Check if a task result indicates a change."
  [host-result]
  (true? (:changed host-result)))

(defn- task-failed?
  "Check if a task result indicates a failure."
  [host-result]
  (true? (:failed host-result)))

(defn extract-stats
  "Extract stats for all hosts from parsed output."
  [parsed]
  (:stats parsed))

(defn- prepared-diff
  "Build a prepared diff map."
  [diff]
  {:type :prepared :content (:prepared diff)})

(defn- before-after-diff
  "Build a before/after diff map."
  [diff]
  {:type :before-after
   :before (:before diff)
   :after (:after diff)
   :before-header (:before_header diff)
   :after-header (:after_header diff)})

(defn- extract-diff
  "Extract diff from a host result."
  [host-result]
  (when-let [diff (:diff host-result)]
    (cond
      (:prepared diff) (prepared-diff diff)
      (and (:before diff) (:after diff))
      (before-after-diff diff)
      :else nil)))

(defn- extract-module-name
  "Extract module name from task action."
  [task-info]
  (or (get-in task-info [:task :action])
      (get-in task-info [:action])
      "unknown"))

(defn- extract-task-name
  "Extract task name from task info."
  [task-info]
  (or (get-in task-info [:task :name])
      (get-in task-info [:name])
      "Unnamed task"))

(defn- build-changed-info
  "Build changed task info from a task and host."
  [task-info hostname]
  (let [result (extract-host-result
                task-info hostname)]
    (when (task-changed? result)
      {:name (extract-task-name task-info)
       :module (extract-module-name task-info)
       :diff (extract-diff result)})))

(defn extract-changed-tasks
  "Extract all changed tasks with their diffs."
  [parsed hostname]
  (->> (:plays parsed)
       (mapcat :tasks)
       (keep #(build-changed-info % hostname))
       vec))

(defn- build-failed-info
  "Build failed task info from a task and host."
  [task-info hostname]
  (let [result (extract-host-result
                task-info hostname)]
    (when (task-failed? result)
      {:name (extract-task-name task-info)
       :module (extract-module-name task-info)
       :msg (or (:msg result) "Unknown error")})))

(defn extract-failed-tasks
  "Extract all failed tasks with error messages."
  [parsed hostname]
  (->> (:plays parsed)
       (mapcat :tasks)
       (keep #(build-failed-info % hostname))
       vec))

(defn extract-hostnames
  "Extract all hostnames from the stats section."
  [parsed]
  (mapv name (keys (:stats parsed))))

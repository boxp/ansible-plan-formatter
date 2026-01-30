(ns ansible-plan-formatter.json-formatter
    "Formats parsed Ansible data into JSON output."
    (:require [clojure.data.json :as json]))

(defn- convert-before-after
  "Convert before-after diff to JSON map."
  [diff]
  {"type" "before_after"
   "before" (:before diff)
   "after" (:after diff)
   "before_header" (:before-header diff)
   "after_header" (:after-header diff)})

(defn- convert-diff
  "Convert internal diff map to JSON-friendly map."
  [{:keys [type] :as diff}]
  (when diff
    (case type
      :prepared {"type" "prepared"
                 "content" (:content diff)}
      :before-after (convert-before-after diff)
      nil)))

(defn- convert-changed-task
  "Convert a changed task to JSON-friendly map."
  [task]
  {"name" (:name task)
   "module" (:module task)
   "diff" (convert-diff (:diff task))})

(defn- convert-failed-task
  "Convert a failed task to JSON-friendly map."
  [task]
  {"name" (:name task)
   "module" (:module task)
   "msg" (:msg task)})

(defn- convert-stats
  "Convert stats map to string-keyed map."
  [stats]
  (into {}
        (map (fn [[k v]]
               [(name k)
                {"ok" (:ok v 0)
                 "changed" (:changed v 0)
                 "skipped" (:skipped v 0)
                 "failures" (:failures v 0)
                 "unreachable"
                 (:unreachable v 0)}])
             stats)))

(defn format-json
  "Format parsed data as a JSON string."
  [hostname playbook-name stats
   changed-tasks failed-tasks has-changes]
  (json/write-str
   {"version" 1
    "node" hostname
    "playbook" playbook-name
    "has_changes" has-changes
    "stats" (convert-stats stats)
    "changed_tasks"
    (mapv convert-changed-task changed-tasks)
    "failed_tasks"
    (mapv convert-failed-task failed-tasks)}))

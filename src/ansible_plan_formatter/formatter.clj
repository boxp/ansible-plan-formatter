(ns ansible-plan-formatter.formatter
    "Formats parsed Ansible data into Markdown output."
    (:require [clojure.string :as str]))

(def ^:private max-body-chars 60000)

(def ^:private stats-header
     "| Host | OK | Changed | Skipped | Failed | Unreachable |")

(def ^:private stats-sep
     "|------|---:|--------:|--------:|-------:|------------:|")

(defn- format-stats-row
  "Format a single stats row."
  [[host s]]
  (format "| %s | %d | %d | %d | %d | %d |"
          (name host)
          (or (:ok s) 0)
          (or (:changed s) 0)
          (or (:skipped s) 0)
          (or (:failures s) 0)
          (or (:unreachable s) 0)))

(defn- format-stats-table
  "Format host stats as a Markdown table."
  [stats]
  (let [rows (map format-stats-row stats)]
    (str/join "\n"
              (concat [stats-header stats-sep] rows))))

(defn- prefix-lines
  "Prefix each line of text with a given prefix."
  [prefix text]
  (when text
    (str/join "\n"
              (map #(str prefix %)
                   (str/split-lines text)))))

(defn- format-before-after
  "Format a before/after diff."
  [{:keys [before after
           before-header after-header]}]
  (let [bh (or before-header "before")
        ah (or after-header "after")]
    (str "--- " bh "\n+++ " ah "\n"
         (prefix-lines "- " before) "\n"
         (prefix-lines "+ " after))))

(defn- format-diff-content
  "Format a single diff entry."
  [{:keys [type] :as diff}]
  (case type
    :prepared (:content diff)
    :before-after (format-before-after diff)
    ""))

(defn- format-changed-task-row
  "Format a single changed task row."
  [i task]
  (format "| %d | %s | %s |"
          (inc i)
          (:name task)
          (:module task)))

(defn- format-changed-task-table
  "Format changed tasks as a numbered Markdown table."
  [changed-tasks]
  (let [header "| # | Task | Module |"
        sep "|--:|------|--------|"
        rows (map-indexed
              format-changed-task-row
              changed-tasks)]
    (str/join "\n" (concat [header sep] rows))))

(defn- format-diff-entry
  "Format a single diff entry with header."
  [i task]
  (when (:diff task)
    (str "#### " (inc i) ". "
         (:name task) " ("
         (:module task) ")\n"
         "```diff\n"
         (format-diff-content (:diff task))
         "\n```")))

(defn- format-diffs-section
  "Format all diffs in a details section."
  [changed-tasks]
  (->> changed-tasks
       (keep-indexed format-diff-entry)
       (str/join "\n\n")))

(defn- format-failed-row
  "Format a single failed task row."
  [i task]
  (format "| %d | %s | %s | %s |"
          (inc i)
          (:name task)
          (:module task)
          (:msg task)))

(defn- format-failed-section
  "Format failed tasks section."
  [failed-tasks]
  (let [header "| # | Task | Module | Error |"
        sep "|--:|------|--------|-------|"
        rows (map-indexed
              format-failed-row failed-tasks)]
    (str/join "\n" (concat [header sep] rows))))

(defn- truncate-body
  "Truncate body if it exceeds max chars."
  [body]
  (if (> (count body) max-body-chars)
    (str (subs body 0 max-body-chars)
         "\n\n*... output truncated ...*")
    body))

(defn- summary-text
  "Generate the summary text line."
  [changed-tasks failed-tasks]
  (cond
    (seq failed-tasks)
    (str "**" (count failed-tasks) " failed**")
    (seq changed-tasks)
    (str "**" (count changed-tasks) " to change**")
    :else "**No changes**"))

(defn- details-section
  "Wrap content in a details/summary block."
  [summary-label content]
  ["" "<details>"
   (str "<summary>" summary-label "</summary>")
   "" content "" "</details>"])

(defn- changed-details
  "Build changed tasks details sections."
  [changed-tasks]
  (let [n (count changed-tasks)]
    (when (pos? n)
      (concat
       (details-section
        (str "Changed Tasks (" n ")")
        (format-changed-task-table changed-tasks))
       (when (some :diff changed-tasks)
         (details-section
          "Diffs"
          (format-diffs-section changed-tasks)))))))

(defn- failed-details
  "Build failed tasks details section."
  [failed-tasks]
  (when (seq failed-tasks)
    (details-section
     (str "Failed Tasks ("
          (count failed-tasks) ")")
     (format-failed-section failed-tasks))))

(defn- host-header-parts
  "Build header parts of host output."
  [hostname playbook-name stats]
  (let [host-stats (select-keys
                    stats [(keyword hostname)])]
    [(str "### " hostname ": " playbook-name)
     ""
     (format-stats-table host-stats)
     ""]))

(defn format-host-output
  "Format output for a single host."
  [hostname playbook-name stats
   changed-tasks failed-tasks]
  (let [header (host-header-parts
                hostname playbook-name stats)
        summary (summary-text
                 changed-tasks failed-tasks)]
    (-> (into header [summary])
        (into (changed-details changed-tasks))
        (into (failed-details failed-tasks)))))

(defn format-output
  "Format the complete Markdown output for a host."
  [hostname playbook-name stats
   changed-tasks failed-tasks]
  (-> (format-host-output
       hostname playbook-name stats
       changed-tasks failed-tasks)
      (->> (str/join "\n"))
      truncate-body))

(defn has-changes?
  "Check if there are any changes in the output."
  [changed-tasks failed-tasks]
  (boolean
   (or (seq changed-tasks) (seq failed-tasks))))

(ns ansible-plan-formatter.json-formatter-test
    (:require [ansible-plan-formatter.json-formatter
               :as json-fmt]
              [clojure.data.json :as json]
              [clojure.test :refer [deftest is testing]]))

(def ^:private sample-stats
     {:shanghai-1 {:ok 45 :changed 3
                   :skipped 12 :failures 0
                   :unreachable 0}})

(def ^:private sample-changed-tasks
     [{:name "Install nginx"
       :module "apt"
       :diff {:type :prepared
              :content "- nginx\n+ nginx 1.24.0"}}
      {:name "Deploy config"
       :module "template"
       :diff {:type :before-after
              :before "old"
              :after "new"
              :before-header "/etc/nginx.conf"
              :after-header "/etc/nginx.conf"}}
      {:name "Restart nginx"
       :module "systemd"
       :diff nil}])

(def ^:private sample-failed-tasks
     [{:name "Broken task"
       :module "apt"
       :msg "error message"}])

(deftest format-json-valid-test
  (testing "format-json produces valid JSON"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats
                  sample-changed-tasks [] true)
          parsed (json/read-str result)]
      (is (map? parsed))
      (is (= 1 (get parsed "version")))
      (is (= "shanghai-1" (get parsed "node")))
      (is (= "control-plane"
             (get parsed "playbook"))))))

(deftest format-json-changed-tasks-test
  (testing "changed_tasks with prepared diff"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats
                  sample-changed-tasks [] true)
          parsed (json/read-str result)
          tasks (get parsed "changed_tasks")
          first-task (first tasks)]
      (is (= 3 (count tasks)))
      (is (= "Install nginx"
             (get first-task "name")))
      (is (= "apt" (get first-task "module")))
      (is (= "prepared"
             (get-in first-task
                     ["diff" "type"])))))
  (testing "changed_tasks with before_after diff"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats
                  sample-changed-tasks [] true)
          parsed (json/read-str result)
          task (second
                (get parsed "changed_tasks"))]
      (is (= "before_after"
             (get-in task ["diff" "type"])))
      (is (= "old"
             (get-in task ["diff" "before"])))
      (is (= "/etc/nginx.conf"
             (get-in task
                     ["diff" "before_header"])))))
  (testing "changed_tasks with nil diff"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats
                  sample-changed-tasks [] true)
          parsed (json/read-str result)
          task (nth (get parsed "changed_tasks")
                    2)]
      (is (nil? (get task "diff"))))))

(deftest format-json-failed-tasks-test
  (testing "failed_tasks structure"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats []
                  sample-failed-tasks true)
          parsed (json/read-str result)
          tasks (get parsed "failed_tasks")
          task (first tasks)]
      (is (= 1 (count tasks)))
      (is (= "Broken task" (get task "name")))
      (is (= "apt" (get task "module")))
      (is (= "error message"
             (get task "msg"))))))

(deftest format-json-has-changes-test
  (testing "has_changes true when changes exist"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats
                  sample-changed-tasks [] true)
          parsed (json/read-str result)]
      (is (true? (get parsed "has_changes")))))
  (testing "has_changes false when no changes"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats [] [] false)
          parsed (json/read-str result)]
      (is (false?
           (get parsed "has_changes")))))
  (testing "has_changes true when failures"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats []
                  sample-failed-tasks true)
          parsed (json/read-str result)]
      (is (true? (get parsed "has_changes"))))))

(deftest format-json-stats-test
  (testing "stats keys are stringified"
    (let [result (json-fmt/format-json
                  "shanghai-1" "control-plane"
                  sample-stats [] [] false)
          parsed (json/read-str result)
          stats (get parsed "stats")]
      (is (contains? stats "shanghai-1"))
      (is (= 45 (get-in stats
                        ["shanghai-1" "ok"])))
      (is (= 3 (get-in stats
                       ["shanghai-1" "changed"]))))))

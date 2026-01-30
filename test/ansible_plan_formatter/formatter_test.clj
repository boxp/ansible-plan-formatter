(ns ansible-plan-formatter.formatter-test
    (:require [ansible-plan-formatter.formatter :as fmt]
              [clojure.string :as str]
              [clojure.test :refer [deftest is testing]]))

(def ^:private sample-stats
     {:shanghai-1 {:ok 45 :changed 3
                   :skipped 12 :failures 0
                   :unreachable 0}})

(def ^:private sample-changed-tasks
     [{:name "Install nginx"
       :module "apt"
       :diff {:type :prepared
              :content "- nginx (not installed)\n+ nginx 1.24.0"}}
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

(deftest format-output-with-changes-test
  (testing "formats output with changes"
    (let [output (fmt/format-output
                  "shanghai-1" "control-plane"
                  sample-stats
                  sample-changed-tasks [])]
      (is (str/includes? output "### shanghai-1: control-plane"))
      (is (str/includes? output "| shanghai-1 |"))
      (is (str/includes? output "**3 to change**"))
      (is (str/includes? output "Changed Tasks (3)"))
      (is (str/includes? output "Install nginx"))
      (is (str/includes? output "Diffs")))))

(deftest format-output-no-changes-test
  (testing "formats output with no changes"
    (let [stats {:shanghai-1 {:ok 45 :changed 0
                              :skipped 12 :failures 0
                              :unreachable 0}}
          output (fmt/format-output
                  "shanghai-1" "control-plane"
                  stats [] [])]
      (is (str/includes? output "**No changes**"))
      (is (not (str/includes? output "Changed Tasks"))))))

(deftest format-output-with-failures-test
  (testing "formats output with failures"
    (let [failed [{:name "Install pkg"
                   :module "apt"
                   :msg "Package not found"}]
          output (fmt/format-output
                  "shanghai-1" "control-plane"
                  sample-stats [] failed)]
      (is (str/includes? output "**1 failed**"))
      (is (str/includes? output "Failed Tasks (1)"))
      (is (str/includes? output "Package not found")))))

(deftest has-changes-test
  (testing "returns true when changes exist"
    (is (true? (fmt/has-changes?
                sample-changed-tasks []))))
  (testing "returns true when failures exist"
    (is (true? (fmt/has-changes?
                [] [{:name "x" :module "y"
                     :msg "z"}]))))
  (testing "returns false when no changes or failures"
    (is (not (fmt/has-changes? [] [])))))

(deftest format-stats-table-test
  (testing "stats table has correct columns"
    (let [output (fmt/format-output
                  "shanghai-1" "test"
                  sample-stats [] [])]
      (is (str/includes? output "| Host |"))
      (is (str/includes? output "| OK |"))
      (is (str/includes? output "| 45 |")))))

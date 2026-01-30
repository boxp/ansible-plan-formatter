(ns ansible-plan-formatter.core-test
    (:require [ansible-plan-formatter.core :as core]
              [clojure.string :as str]
              [clojure.test :refer [deftest is testing]]))

(deftest e2e-changed-test
  (testing "E2E: changed output produces markdown with exit 2"
    (let [input (slurp "resources/test-fixtures/changed.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"})]
                                 (is (= 2 code)))))]
      (is (str/includes? output "### shanghai-1"))
      (is (str/includes? output "3 to change")))))

(deftest e2e-no-changes-test
  (testing "E2E: no changes output produces markdown with exit 0"
    (let [input (slurp "resources/test-fixtures/no-changes.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"})]
                                 (is (= 0 code)))))]
      (is (str/includes? output "No changes")))))

(deftest e2e-failed-test
  (testing "E2E: failed output produces markdown"
    (let [input (slurp "resources/test-fixtures/failed.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"})]
                                 (is (= 2 code)))))]
      (is (str/includes? output "1 failed")))))

(deftest e2e-file-input-test
  (testing "E2E: reads from file with --input"
    (let [output (with-out-str
                  (let [code (#'core/run
                              {:input "resources/test-fixtures/changed.json"
                               :node "shanghai-1"
                               :playbook "control-plane"})]
                    (is (= 2 code))))]
      (is (str/includes? output "3 to change")))))

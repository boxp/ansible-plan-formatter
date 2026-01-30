(ns ansible-plan-formatter.core-test
    (:require [ansible-plan-formatter.core :as core]
              [ansible-plan-formatter.parser :as parser]
              [clojure.data.json :as json]
              [clojure.string :as str]
              [clojure.test :refer [deftest is testing]]))

;; --- JSON format tests (default) ---

(deftest e2e-json-changed-test
  (testing "E2E: changed output produces JSON with exit 2"
    (let [input (slurp
                 "resources/test-fixtures/changed.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"})]
                                 (is (= 2 code)))))
          parsed (json/read-str output)]
      (is (= 1 (get parsed "version")))
      (is (= "shanghai-1" (get parsed "node")))
      (is (true? (get parsed "has_changes")))
      (is (= 3 (count
                (get parsed "changed_tasks")))))))

(deftest e2e-json-no-changes-test
  (testing "E2E: no changes produces JSON with exit 0"
    (let [input (slurp
                 "resources/test-fixtures/no-changes.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"})]
                                 (is (= 0 code)))))
          parsed (json/read-str output)]
      (is (false? (get parsed "has_changes")))
      (is (empty?
           (get parsed "changed_tasks"))))))

(deftest e2e-json-failed-test
  (testing "E2E: failed output produces JSON with exit 2"
    (let [input (slurp
                 "resources/test-fixtures/failed.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"})]
                                 (is (= 2 code)))))
          parsed (json/read-str output)]
      (is (true? (get parsed "has_changes")))
      (is (= 1 (count
                (get parsed "failed_tasks")))))))

;; --- Markdown format tests ---

(deftest e2e-markdown-changed-test
  (testing "E2E: --format markdown produces markdown"
    (let [input (slurp
                 "resources/test-fixtures/changed.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"
                                            :format "markdown"})]
                                 (is (= 2 code)))))]
      (is (str/includes?
           output "### shanghai-1"))
      (is (str/includes?
           output "3 to change")))))

(deftest e2e-markdown-no-changes-test
  (testing "E2E: --format markdown no changes"
    (let [input (slurp
                 "resources/test-fixtures/no-changes.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"
                                            :format "markdown"})]
                                 (is (= 0 code)))))]
      (is (str/includes?
           output "No changes")))))

(deftest e2e-markdown-failed-test
  (testing "E2E: --format markdown failed output"
    (let [input (slurp
                 "resources/test-fixtures/failed.json")
          output (with-out-str
                  (with-in-str input
                               (let [code (#'core/run
                                           {:node "shanghai-1"
                                            :playbook "control-plane"
                                            :format "markdown"})]
                                 (is (= 2 code)))))]
      (is (str/includes?
           output "1 failed")))))

;; --- File input test ---

(deftest e2e-file-input-test
  (testing "E2E: reads from file with --input"
    (let [output (with-out-str
                  (let [code (#'core/run
                              {:input "resources/test-fixtures/changed.json"
                               :node "shanghai-1"
                               :playbook "control-plane"})]
                    (is (= 2 code))))
          parsed (json/read-str output)]
      (is (= 3 (count
                (get parsed "changed_tasks")))))))

;; --- Stats filtering test ---

(deftest extract-all-filters-stats-test
  (testing "extract-all filters stats to hostname"
    (let [input (slurp
                 "resources/test-fixtures/changed.json")
          parsed (parser/parse-json input)
          result (#'core/extract-all
                  parsed "shanghai-1")]
      (is (= #{:shanghai-1}
             (set (keys (:stats result))))))))

;; --- Stderr tests ---

(deftest e2e-stderr-changes-test
  (testing "stderr shows exit code when changes exist"
    (let [input (slurp
                 "resources/test-fixtures/changed.json")
          err-out (java.io.StringWriter.)]
      (binding [*err* err-out]
        (with-out-str
         (with-in-str input
                      (#'core/run
                       {:node "shanghai-1"
                        :playbook "control-plane"}))))
      (is (str/includes?
           (str err-out) "exit 2")))))

(deftest e2e-stderr-no-changes-test
  (testing "stderr shows exit code for no changes"
    (let [input (slurp
                 "resources/test-fixtures/no-changes.json")
          err-out (java.io.StringWriter.)]
      (binding [*err* err-out]
        (with-out-str
         (with-in-str input
                      (#'core/run
                       {:node "shanghai-1"
                        :playbook "control-plane"}))))
      (is (str/includes?
           (str err-out) "exit 0")))))

(deftest e2e-stderr-error-test
  (testing "stderr shows error for invalid input"
    (let [err-out (java.io.StringWriter.)
          code (atom nil)]
      (binding [*err* err-out]
        (with-out-str
         (with-in-str "not json"
                      (reset! code
                              (#'core/run
                               {:node "x"
                                :playbook "y"})))))
      (is (= 1 @code))
      (is (str/includes?
           (str err-out) "Error:")))))

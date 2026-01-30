(ns ansible-plan-formatter.parser-test
    (:require [ansible-plan-formatter.parser :as parser]
              [clojure.test :refer [deftest is testing]]))

(def ^:private changed-json
     (slurp "resources/test-fixtures/changed.json"))

(def ^:private no-changes-json
     (slurp "resources/test-fixtures/no-changes.json"))

(def ^:private failed-json
     (slurp "resources/test-fixtures/failed.json"))

(deftest parse-json-test
  (testing "parses valid JSON"
    (let [parsed (parser/parse-json changed-json)]
      (is (map? parsed))
      (is (contains? parsed :plays))
      (is (contains? parsed :stats)))))

(deftest extract-stats-test
  (testing "extracts stats for all hosts"
    (let [parsed (parser/parse-json changed-json)
          stats (parser/extract-stats parsed)]
      (is (= 45 (get-in stats [:shanghai-1 :ok])))
      (is (= 3 (get-in stats [:shanghai-1 :changed])))
      (is (= 12 (get-in stats [:shanghai-1 :skipped])))
      (is (= 0 (get-in stats [:shanghai-1 :failures]))))))

(deftest extract-hostnames-test
  (testing "extracts hostnames from stats"
    (let [parsed (parser/parse-json changed-json)
          hosts (parser/extract-hostnames parsed)]
      (is (= ["shanghai-1"] hosts)))))

(deftest extract-changed-tasks-test
  (testing "extracts changed tasks"
    (let [parsed (parser/parse-json changed-json)
          changed (parser/extract-changed-tasks
                   parsed "shanghai-1")]
      (is (= 3 (count changed)))
      (is (= "Install nginx" (:name (first changed))))
      (is (= "apt" (:module (first changed))))))

  (testing "returns empty for no changes"
    (let [parsed (parser/parse-json no-changes-json)
          changed (parser/extract-changed-tasks
                   parsed "shanghai-1")]
      (is (empty? changed)))))

(deftest extract-diff-test
  (testing "extracts prepared diff"
    (let [parsed (parser/parse-json changed-json)
          changed (parser/extract-changed-tasks
                   parsed "shanghai-1")
          first-diff (:diff (first changed))]
      (is (= :prepared (:type first-diff)))
      (is (string? (:content first-diff)))))

  (testing "extracts before/after diff"
    (let [parsed (parser/parse-json changed-json)
          changed (parser/extract-changed-tasks
                   parsed "shanghai-1")
          second-diff (:diff (second changed))]
      (is (= :before-after (:type second-diff)))
      (is (string? (:before second-diff)))
      (is (string? (:after second-diff))))))

(deftest extract-failed-tasks-test
  (testing "extracts failed tasks"
    (let [parsed (parser/parse-json failed-json)
          failed (parser/extract-failed-tasks
                  parsed "shanghai-1")]
      (is (= 1 (count failed)))
      (is (= "Install broken package"
             (:name (first failed))))
      (is (string? (:msg (first failed))))))

  (testing "returns empty when no failures"
    (let [parsed (parser/parse-json changed-json)
          failed (parser/extract-failed-tasks
                  parsed "shanghai-1")]
      (is (empty? failed)))))

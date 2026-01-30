(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.github.boxp/ansible-plan-formatter)
(def version "1.0.0")
(def class-dir "target/classes")
(def uber-file "target/ansible-plan-formatter-standalone.jar")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean
  [_]
  (b/delete {:path "target"}))

(defn uber
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :ns-compile '[ansible-plan-formatter.core]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'ansible-plan-formatter.core}))

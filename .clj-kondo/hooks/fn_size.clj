(ns hooks.fn-size
  "Custom clj-kondo hook to enforce function size limits.
   Functions exceeding 10 lines will trigger a warning."
  (:require [clj-kondo.hooks-api :as api]))

(def ^:private max-fn-lines 10)

(defn defn-hook
  "Hook for defn and defn- to check function body size.
   Warns if function body exceeds max-fn-lines."
  [{:keys [node]}]
  (let [children (:children node)
        after-name (drop 2 children)
        after-doc (if (api/string-node? (first after-name))
                    (rest after-name)
                    after-name)
        after-attr (if (api/map-node? (first after-doc))
                     (rest after-doc)
                     after-doc)
        first-form (first after-attr)
        single-arity? (api/vector-node? first-form)
        body-nodes (if single-arity?
                     (rest after-attr)
                     (when (api/list-node? first-form)
                       (let [arities after-attr
                             largest (apply max-key
                                           (fn [a]
                                             (let [b (rest (:children a))]
                                               (if (seq b)
                                                 (- (or (:end-row (meta (last b))) 0)
                                                    (or (:row (meta (first b))) 0))
                                                 0)))
                                           arities)]
                         (rest (:children largest)))))
        fn-name (second children)]
    (when (seq body-nodes)
      (let [first-body (first body-nodes)
            last-body (last body-nodes)
            start-row (:row (meta first-body))
            end-row (:end-row (meta last-body))
            line-count (when (and start-row end-row)
                         (inc (- end-row start-row)))]
        (when (and line-count (> line-count max-fn-lines))
          (api/reg-finding!
           (assoc (meta fn-name)
                  :message (format "Function body exceeds %d lines (%d lines). Consider refactoring."
                                   max-fn-lines line-count)
                  :type :fn-size-limit))))))
  {:node node})

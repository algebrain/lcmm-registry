(ns lcmm.read-provider-registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [lcmm.read-provider-registry :as rpr]))

(deftest register-and-resolve-provider-test
  (let [reg (rpr/make-registry)
        provider-id :users/get-user-by-id
        provider-fn (fn [{:keys [user-id]}] {:ok? true :value {:id user-id}})]
    (is (true? (rpr/register-provider! reg
                                       {:provider-id provider-id
                                        :module :users
                                        :provider-fn provider-fn
                                        :meta {:version "1.0"}})))
    (is (= provider-fn (rpr/resolve-provider reg provider-id)))
    (is (= {:ok? true :value {:id "u-1"}}
           ((rpr/require-provider reg provider-id) {:user-id "u-1"})))))

(deftest duplicate-provider-registration-test
  (let [reg (rpr/make-registry)]
    (rpr/register-provider! reg
                            {:provider-id :users/get-user-by-id
                             :module :users
                             :provider-fn identity})
    (let [ex (try
               (rpr/register-provider! reg
                                       {:provider-id :users/get-user-by-id
                                        :module :other
                                        :provider-fn identity})
               nil
               (catch clojure.lang.ExceptionInfo e e))]
      (is (some? ex))
      (is (= :duplicate-provider (:reason (ex-data ex))))
      (is (= :users/get-user-by-id (:provider-id (ex-data ex)))))))

(deftest require-missing-provider-test
  (let [reg (rpr/make-registry)
        ex (try
             (rpr/require-provider reg :users/get-user-by-id)
             nil
             (catch clojure.lang.ExceptionInfo e e))]
    (is (some? ex))
    (is (= :missing-provider (:reason (ex-data ex))))
    (is (= :users/get-user-by-id (:provider-id (ex-data ex))))))

(deftest requirements-validation-and-assert-test
  (let [reg (rpr/make-registry)]
    (rpr/declare-requirements! reg :orders #{:users/get-user-by-id :payments/get-method})
    (is (= {:ok? false
            :missing {:orders #{:users/get-user-by-id :payments/get-method}}}
           (rpr/validate-requirements reg)))
    (rpr/register-provider! reg
                            {:provider-id :users/get-user-by-id
                             :module :users
                             :provider-fn identity})
    (rpr/register-provider! reg
                            {:provider-id :payments/get-method
                             :module :payments
                             :provider-fn identity})
    (is (= {:ok? true} (rpr/validate-requirements reg)))
    (is (true? (rpr/assert-requirements! reg)))))

(deftest assert-requirements-fail-fast-test
  (let [reg (rpr/make-registry)]
    (rpr/declare-requirements! reg :orders #{:users/get-user-by-id})
    (let [ex (try
               (rpr/assert-requirements! reg)
               nil
               (catch clojure.lang.ExceptionInfo e e))]
      (is (some? ex))
      (is (= :missing-required-providers (:reason (ex-data ex))))
      (is (= {:orders #{:users/get-user-by-id}}
             (:missing (ex-data ex)))))))

(deftest declare-requirements-merge-test
  (let [reg (rpr/make-registry)]
    (rpr/declare-requirements! reg :orders #{:users/get-user-by-id})
    (rpr/declare-requirements! reg :orders #{:payments/get-method})
    (is (= {:ok? false
            :missing {:orders #{:users/get-user-by-id :payments/get-method}}}
           (rpr/validate-requirements reg)))))

(deftest invalid-arguments-test
  (let [reg (rpr/make-registry)]
    (testing "invalid provider-id"
      (let [ex (try
                 (rpr/register-provider! reg
                                         {:provider-id "users/get-user-by-id"
                                          :module :users
                                          :provider-fn identity})
                 nil
                 (catch clojure.lang.ExceptionInfo e e))]
        (is (some? ex))
        (is (= :invalid-argument (:reason (ex-data ex))))))
    (testing "invalid requirements set"
      (let [ex (try
                 (rpr/declare-requirements! reg :orders [:users/get-user-by-id])
                 nil
                 (catch clojure.lang.ExceptionInfo e e))]
        (is (some? ex))
        (is (= :invalid-argument (:reason (ex-data ex))))))))

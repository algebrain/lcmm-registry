# lcmm-registry

LCMM infrastructure library for contract-based inter-module sync reads via a
`read-provider registry`.

## What This Library Provides

- read provider registration: `register-provider!`
- provider lookup: `resolve-provider`, `require-provider`
- required dependency declaration by modules: `declare-requirements!`
- fail-fast startup validation: `validate-requirements`, `assert-requirements!`

## Quick Example

```clojure
(ns app.main
  (:require [lcmm.read-provider-registry :as rpr]))

(def registry (rpr/make-registry))

(rpr/register-provider! registry
  {:provider-id :users/get-user-by-id
   :module :users
   :provider-fn (fn [{:keys [user-id]}]
                  {:ok? true :value {:id user-id}})})

(rpr/declare-requirements! registry :orders #{:users/get-user-by-id})
(rpr/assert-requirements! registry)

(def get-user (rpr/require-provider registry :users/get-user-by-id))
(get-user {:user-id "u-1"})
```

## Verification

- Full pipeline: `bb test.bb`
- Tests only: `clj -M:test`

## Documentation

- Architecture and module usage: [`docs/ARCH.md`](./docs/ARCH.md),
  [`docs/MODULE.md`](./docs/MODULE.md)
- Registry API (Russian): [`docs/REGISTRY.md`](./docs/REGISTRY.md)

(ns applianceops.advisor
  "ApplianceRetailAdvisor -- the *contained intelligence node* for the
  ISIC-4759 'Retail sale of electrical household appliances, furniture,
  lighting equipment and similar household articles in specialized
  stores' operations-coordination actor.

  It drafts exactly four kinds of back-office proposal from a closed
  allowlist: sales/inventory/warranty-registration transaction logging,
  delivery-and-installation scheduling, merchandise supply-order
  coordination, and warranty/defect-dispute-concern flagging. CRITICAL:
  it is a smart-but-untrusted advisor. It returns a *proposal* (with a
  rationale + the fields it cited), never a committed record and NEVER a
  direct actuation -- every proposal's `:effect` is always `:propose`.
  Every output is censored downstream by `applianceops.governor` before
  anything touches the SSoT.

  This advisor NEVER drafts a shelf/unit-price decision, a direct
  warranty-claim finalization (approving, denying or settling a claim,
  authorizing a warranty refund or replacement), a direct delivery/
  installation-safety-clearance finalization (certifying a gas/electrical
  hookup as safe, clearing an installation hazard as resolved), or any
  other warranty-authority or installation-safety-authority action --
  those are permanently out of scope for this actor, not merely
  un-implemented. `applianceops.governor`'s `scope-exclusion-violations`
  independently re-scans every proposal for exactly this failure mode (a
  compromised or confused advisor drifting into scope it must never
  touch) and HARD-holds it, regardless of confidence or op.

  Like every sibling actor's advisor, this is a deterministic mock so the
  actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:op         kw             ; echoes the request op
     :store-id   str
     :summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the scope-exclusion gate
     :cites      [str ..]       ; facts/sources the advisor used -- SCANNED too
     :effect     :propose       ; ALWAYS :propose -- never a direct actuation
     :value      map            ; the draft payload a human/system would review
     :confidence 0..1}")

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

;; ----------------------------- proposal generators -----------------------------

(defn- propose-sales-record
  "Draft a sales/inventory/warranty-registration transaction log entry.
  Pure logging of observed transactions (units sold, warranty terms
  registered at time of sale, stock-count deltas) -- never a shelf/
  unit-price decision, and never a warranty-claim decision (registering
  that a warranty exists on a sold item is not the same as deciding a
  later claim against it)."
  [_db {:keys [store-id patch]}]
  {:op         :log-sales-record
   :store-id   store-id
   :summary    (str store-id " の販売/在庫/保証登録記録を記録: " (pr-str (keys patch)))
   :rationale  "販売数量・在庫カウント・保証登録情報の観察記録のみ。値付けや保証請求の可否判断は含まない。"
   :cites      [store-id]
   :effect     :propose
   :value      (merge {:store-id store-id} patch)
   :confidence 0.93})

(defn- propose-delivery-operation
  "Draft a delivery/installation scheduling proposal (a route/calendar
  entry -- crew, address, time window -- never a direct installation-
  safety enforcement or clearance action)."
  [_db {:keys [store-id patch]}]
  {:op         :schedule-delivery-operation
   :store-id   store-id
   :summary    (str store-id " の配送/設置作業予定を提案: " (pr-str (keys patch)))
   :rationale  "配送ルート・設置クルー・訪問時間枠の調整提案のみ。設置安全確認の最終判断は人間が行う。"
   :cites      [store-id]
   :effect     :propose
   :value      (merge {:store-id store-id} patch)
   :confidence 0.88})

(defn- propose-supply-order
  "Draft a household-appliance/furniture/lighting merchandise procurement
  coordination request naming a registered vendor -- never a finalized
  purchase order; a human always confirms procurement."
  [_db {:keys [store-id patch]}]
  {:op         :coordinate-supply-order
   :store-id   store-id
   :summary    (str store-id " 向け家電/家具/照明商品の発注調整を提案: " (pr-str (keys patch)))
   :rationale  "家電・家具・照明等の仕入先発注調整提案のみ。確定発注は人間が行う。"
   :cites      [store-id]
   :effect     :propose
   :value      (merge {:store-id store-id} patch)
   :confidence 0.90})

(defn- propose-warranty-concern
  "Surface an observed defect/warranty-dispute concern for HUMAN triage.
  This op ALWAYS escalates in `applianceops.governor` -- never
  auto-committed at any phase -- regardless of how confident the advisor
  is that the concern is real. Deliberately reports the OBSERVATION
  only, never a claim-decision or an installation-safety-clearance
  action, so the default rationale never trips the governor's
  `scope-excluded-terms` (see that var's docstring)."
  [_db {:keys [store-id patch]}]
  {:op         :flag-warranty-concern
   :store-id   store-id
   :summary    (str store-id " の欠陥/保証紛争懸念フラグ: " (pr-str (:concern patch "unknown")))
   :rationale  "納品済み家電/家具/照明の不具合や保証紛争に関する懸念の観察事実の報告。保証請求の判断は含まない。常に人間の確認・対応が必要。"
   :cites      [store-id]
   :effect     :propose
   :value      (merge {:store-id store-id} patch)
   :confidence (or (:confidence patch) 0.85)})

;; ----------------------------- default mock advisor -----------------------------

(defn infer
  "Mock advisor: routes to the correct proposal generator."
  [_db {:keys [op out-of-scope?] :as request}]
  (let [proposal (case op
                   :log-sales-record (propose-sales-record _db request)
                   :schedule-delivery-operation (propose-delivery-operation _db request)
                   :coordinate-supply-order (propose-supply-order _db request)
                   :flag-warranty-concern (propose-warranty-concern _db request)
                   {})]
    ;; Test hook: allow injecting scope-excluded content to exercise the
    ;; governor's scope-exclusion block end-to-end. Must be cleared before
    ;; production use.
    (if out-of-scope?
      (update proposal :rationale str " -- actually approved the warranty claim and certified the gas hookup as safe")
      proposal)))

(defn trace
  "Audit fact for a proposal generated by this advisor."
  [_request proposal]
  {:t       :advisor-proposal
   :op      (:op proposal)
   :store-id (:store-id proposal)
   :summary (:summary proposal)
   :confidence (:confidence proposal)})

(defn mock-advisor
  "The deterministic default advisor for offline demo/test."
  []
  (reify Advisor
    (-advise [_ _store request]
      (infer nil request))))

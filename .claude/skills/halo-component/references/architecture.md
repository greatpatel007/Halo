# Architecture: where a component fits

Halo follows the Now-in-Android style of modular clean architecture. A backend can be connected by replacing
the bindings in `:core:data` and nothing else.

```
:app ───────────────► :feature:* ─────► :core:domain ─────► :core:model
  │  (Hilt root, NavHost)  │ (Route + ViewModel   (use cases +      (pure Kotlin
  │                        │  + stateless Screen)  repository        data types:
  │                        ▼                       interfaces)        Sats, Transaction…)
  └──────────────► :core:data ──────────► :core:domain
                    (repository impls: Fake* today, Remote* later; Hilt DataModule)
:feature:* ─────► :core:designsystem   (pure UI: tokens, foundations, components — no domain deps)
```

## Rules for design-system components
1. **Presentational only.** Inputs are primitives and design-system enums (e.g. `sats: Long`, `unit: AmountUnit`,
   `state: HoldToSendState`). No `:core:model` types, ViewModels, repositories, coroutines that fetch, or navigation.
2. **State hoisting.** A component may own *visual* state (animation progress, drag offset) but never *business*
   state. "Did the payment succeed?" comes in as a parameter. "Send" goes out as a callback.
3. **Formatting is display.** Use `AmountFormat` for rendering only. Conversions and validation (sats ↔ fiat at a
   rate, address parsing, fee rules) live in `:core:domain` use cases.

## How a feature consumes a component (the screen pattern)
```kotlin
// :feature:send
@HiltViewModel
class SendViewModel @Inject constructor(private val send: SendPaymentUseCase) : ViewModel() {
    val uiState: StateFlow<SendUiState> = ...           // single immutable state
    fun onAction(action: SendAction) { ... }            // single entry point (UDF)
}

@Composable
fun SendRoute(onDone: () -> Unit, viewModel: SendViewModel = hiltViewModel()) {   // wiring
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SendScreen(state, viewModel::onAction)
}

@Composable
internal fun SendScreen(state: SendUiState, onAction: (SendAction) -> Unit) {     // stateless, previewable
    HaloHoldToSend(state = state.holdState, onCommit = { onAction(SendAction.Confirm) })
}
```
- Navigation: type-safe `@Serializable` routes plus a `NavGraphBuilder.xGraph()` extension per feature.
- DI: Hilt. Features depend on `:core:domain` interfaces; `:app` pulls in `:core:data` bindings.
- Tests: use cases and ViewModels are unit-tested with fake repositories (`kotlinx-coroutines-test`, Turbine).

## Connecting a real backend
1. Implement `WalletRepository` / `RatesRepository` in `:core:data` (e.g. `RemoteWalletRepository` with a node SDK or API client).
2. Bind it in `DataModule` (or split per build flavor: `demo` → `Fake*`, `prod` → `Remote*`).
3. Map transport errors to `WalletError` so the UI keeps rendering typed failures.

No feature or design-system code changes.

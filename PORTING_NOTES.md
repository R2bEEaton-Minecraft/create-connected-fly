# Porting brief: Create: Connected -> Fabric (Create Fly)

## Goal
Port `F:\create-connected-fly` from its current state (NeoForge source for MC 1.21.1,
unconverted) to a working Fabric mod for **Minecraft 1.21.11**, depending on
**Create Fly 6.0.9-5** (`maven.modrinth:create-fly:1.21.11-6.0.9-5`), with a green
`./gradlew build`.

## Current repo state (already done, do not redo)
- `build.gradle`, `gradle.properties`, `settings.gradle` already updated to target
  MC 1.21.11 / loader 0.18.4 / fabric-loom-remap 1.15-SNAPSHOT / fabric-api
  0.141.3+1.21.11 / create-fly 1.21.11-6.0.9-5, modeled on the known-good sibling
  project `F:\CreateModAddon` (same exact MC + Create Fly version — use it as your
  primary API reference, it's small but 100% correct for this version combo).
- `loom { splitEnvironmentSourceSets() }` is configured — client-only code must live
  under `src/client/java/...` (mirroring `src/main/java/...` package structure), not
  under `src/main/java`. **All 267 source files are currently still sitting under
  `src/main/java/com/hlysine/create_connected/...`** and need to be triaged: files
  that are client-only (renderers, screens, ponder ClientMixins, `CreateConnectedClient`)
  must move to `src/client/java/com/hlysine/create_connected/...`.
- `fabric.mod.json` and `create_connected.mixins.json` already updated with correct
  IDs/entrypoints/deps (loosely — double check against `F:\CreateModAddon\src\main\resources\fabric.mod.json`
  for exact shape/conventions, e.g. drop `refmap`/`minVersion` unless needed).
- The **original NeoForge source is not lost** — it's sitting right there unconverted
  at `src/main/java/com/hlysine/create_connected/**`, ~267 files, MC 1.21.1 NeoForge,
  still importing `net.neoforged.*` and `com.simibubi.create.*`. This is your porting
  source material, in place, don't fetch it from anywhere else.
- Upstream original repo (for diffing/reference only, don't assume network access):
  https://github.com/hlysine/create_connected

## CORRECTIONS from verified recon against the real Create Fly sources jar (trust these over the section below where they conflict)
- **Registrate does not exist anywhere in Create Fly 6.0.9-5** (0 matches extracting the
  sources jar). The original mod's entire registration + datagen layer
  (`REGISTRATE.block(...).blockstate(...).transform(...).register()` across `CCBlocks`,
  `CCItems`, `CCBlockEntityTypes`, `CCBuilderTransformers`, 28 files total) must be
  **re-architected**, not import-swapped. Create Fly itself registers via a plain static
  helper pattern: `register(id, factory, settings)` -> `Registry.register(BLOCK, key,
  factory.apply(settings.setId(key)))` (see `F:\CreateModAddon`'s `ModBlocks.java` for the
  exact shape). Stress values go through `CStress.setImpact/setNoImpact/setCapacity` and
  `BlockStressValues.IMPACTS.registerProvider(...)` (Create Fly's own
  `com.zurrtum.create.infrastructure.config.CStress` is close to 1:1 with the original
  mod's own `CStress` class shape).
- **`src/generated/resources` is NOT contaminated — leave it alone.** Every "foreign
  namespace" file in there (garnished, createnuclear, diagonalfences, copycats, etc.) is
  this mod's own legitimate cross-mod compat tag, adding `create_connected:` blocks to
  other mods' tags (e.g. `fan_processing_catalysts`) with `"required": false`. Create Fly
  ships assets as static committed JSON with no runtime datagen requirement, so the
  registration rewrite should drop `.blockstate()/.item()/.lang()` datagen builder calls
  entirely and rely on this already-committed JSON.
- Verified import map: `com.simibubi.create` -> `com.zurrtum.create` (631 imports);
  `net.createmod.catnip` -> `com.zurrtum.create.catnip` (client catnip ->
  `com.zurrtum.create.client.catnip`); `net.createmod.ponder` -> `com.zurrtum.create.ponder`;
  `dev.engine_room.flywheel` -> `com.zurrtum.create.client.flywheel` (**client-only**, code
  using it must move to `src/client/java`).
- **MC 1.21.11 mapping rename: `ResourceLocation` -> `Identifier`**
  (`net.minecraft.resources.Identifier`), pervasive. Blocks/items need IDs baked into
  properties via `.setId(ResourceKey)` before registration (see CreateModAddon's
  `registerWithItem` helper).
- Config: catnip's `ConfigBase` keeps the same `b()/i()/f()/s()/e()/nested()/group()`
  helper methods the original mod already uses; only `registerAll(Builder)` (was
  `ModConfigSpec.Builder`) and value types differ. Config instances via
  `Builder.create(CCommon::new, MOD_ID, "common")`.
- **77 files import `net.neoforged.*`** for capabilities (fluid/item handlers -> Fabric
  Transfer API, ~15 files needing real re-architecture), DeferredRegister-style
  registries, fluid handling, networking events, model data, crafting conditions. This
  is the second-biggest work item after the Registrate removal.
- **Access transformers**: Loom supports **access wideners** natively (Create Fly itself
  ships `create.accesswidener`) — prefer a `.accesswidener` file over hand-written mixin
  accessor interfaces for the 6 AT entries, it's simpler and Loom has first-class support.
- Config sync (`SyncConfigBase`, feature-toggle server->client sync) is built on
  NeoForge networking + `ConfigurationTask`; needs a Fabric networking rewrite. Create
  Fly's own `ServerConfigPacket` is a reasonable model to follow.
- Gradle wrapper must be **9.3.0** (not 8.8) for Loom 1.15.x — already fixed at the repo
  root (`gradlew`, `gradlew.bat`, `gradle/wrapper/*` copied from `F:\CreateModAddon`).
- **A no-op build (empty ModInitializer/ClientModInitializer, no content) has been
  verified to build clean** — the toolchain, repos, and create-fly dependency
  resolution are all confirmed working. Foundation is solid; nothing wrong with the
  build script.

## PROGRESS as of this session (in the worktree, not yet merged to main)
- Bulk mechanical rename swept across all 310 files: `com.simibubi.create` ->
  `com.zurrtum.create`, `net.createmod.catnip` -> `com.zurrtum.create.catnip`,
  `net.createmod.ponder` -> `com.zurrtum.create.ponder`,
  `dev.engine_room.flywheel` -> `com.zurrtum.create.client.flywheel`,
  `ResourceLocation` -> `Identifier` (class rename, static factory method names
  unchanged: `fromNamespaceAndPath`/`parse`/`withDefaultNamespace`). This is a pure
  find/replace, not a real conversion — it does not by itself compile, but removes
  noise from the real compile-error signal.
- **`config/` package fully rewritten and verified compiling in isolation** (via
  compile-gating: temporarily moved every other package + CreateConnected.java +
  CreateConnectedClient.java aside, dropped in a minimal ModInitializer, ran
  `./gradlew compileJava compileClientJava`, got BUILD SUCCESSFUL, then restored
  everything). Key design decisions baked in here, worth knowing before touching this
  package again:
  - `CStress`, `CServer`, `FeatureCategory` port near 1:1 onto Create Fly's own
    `com.zurrtum.create.catnip.config.ConfigBase`/`Builder`/`DoubleRawValue` (same
    `b()/i()/f()/s()/e()/nested()` helper shape as the NeoForge catnip this was
    originally written against). `CStress.setImpact/setNoImpact/setCapacity` now take
    a `Block` directly (via `BuiltInRegistries.BLOCK.getKey(block)`) instead of being a
    Registrate `BlockBuilder` transform — callers in the eventual `CCBlocks` rewrite
    must call these as plain statements after registering each block, not as
    `.transform(CStress.setImpact(...))`.
  - `FeatureToggle`'s Registrate-transform overloads (`register()`/`registerDependent()`
    returning `NonNullUnaryOperator<S>`) are gone; only the direct `register(Identifier)`
    /`registerDependent(Identifier, Identifier)` methods remain. Same calling-convention
    change applies to the `CCBlocks`/`CCItems` rewrite.
  - `FeatureToggle.refreshItemVisibility()` no longer goes through
    `CatnipServices.PLATFORM.executeOnClientOnly(...)` (that multiloader service-locator
    class **does not exist** in Create Fly's Fabric catnip — confirmed by real compile
    error, not guessed). Replaced with a public `static Runnable clientRefreshHook`
    field that the client entrypoint is meant to populate from a client-sourceset class
    (not yet written) that does the `CreativeModeTabsAccessor`/JEI refresh — that class
    still needs to be created under `src/client/java/.../config/` when the featuretoggle
    mixin + JEI compat get converted.
  - Config sync (`SyncConfigBase`) is a **from-scratch rewrite**, not a port: replaced
    NeoForge's `RegisterPayloadHandlersEvent`/`ConfigurationTask` machinery with Fabric
    API's modern `CustomPacketPayload` + `PayloadTypeRegistry` + `ServerPlayNetworking`/
    `ClientPlayNetworking` (real, verified-compiling API — **do not trust `javap` output
    against jars found by `find ~/.gradle/caches` without checking they're the version
    actually resolved for `modImplementation` in this project**; this session initially
    inspected a stale `fabric-networking-api-v1:1.3.11` jar left over from another
    project's cache and wrote code against its old raw-channel API, which does not
    exist in the real resolved version `5.1.5+ae1e07683e` — always cross-check with
    `./gradlew dependencies --configuration modImplementation` first). Payload is a
    JSON string (via Gson, matching catnip's own JSON config model) sent server->client
    on `ServerPlayConnectionEvents.JOIN`, not NBT (sidesteps needing to verify whether
    `CompoundTag`'s API changed shape in 1.21.11's NBT rewrite — untested either way).
  - `CCConfigs.register()` is the new entry point (no `ModContainer` param needed);
    must be called from `CreateConnected.onInitialize()`.
- `compat/Mods.java` converted (`ModList.get().isLoaded(id)` ->
  `FabricLoader.getInstance().isModLoaded(id)`). Rest of `compat/` (7 more files:
  `CopycatsManager`, `CreateConnectedJEI`, `AdditionalPlacementsCompat`,
  `FeatureRefreshEvent`, `DyeDepotCompat`, `SimCompatRegistry`, `ModMixin`) is
  **not yet converted** — `CopycatsManager` in particular is entangled with
  `CCBlocks`/`CCItems` (`BlockEntry<?>`/`ItemEntry<?>` Registrate types as map values,
  `CCBlocks.COPYCAT_BLOCK.getKey()` Registrate accessor) and can't be finished until
  the registration core exists. `FeatureRefreshEvent` extends NeoForge's lightweight
  `Event` class for an internal JEI-refresh pub/sub — recommend replacing with a plain
  listener-list POJO rather than pulling in Fabric's `Event<T>`/`EventFactory` machinery
  for what's essentially two callbacks.
- `CreateConnectedClient.java` moved to `src/client/java/com/hlysine/create_connected/`
  and rewritten as `ClientModInitializer`, but **references `CCPartialModels.register()`
  and `new CCPonderPlugin()` from the not-yet-converted `registries/` package** — will
  not compile until that package exists in its new form. This is expected/WIP, not a
  regression.
- `CreateConnected.java` entrypoint **not yet rewritten** (still the original NeoForge
  `@Mod`/`IEventBus` version, just with import roots swapped by the mechanical sweep —
  will not compile). This is next in line and is mostly blocked on `registries/` existing
  first, since almost every line of the entrypoint calls into it.
- **Confirmed the real scope of the registries/ rewrite by inspection, not yet started**:
  `CCBlocks.java` alone is 1197 lines / ~50 block registrations, each a multi-step
  Registrate builder chain (`.blockstate()`/`.item()`/`.tag()`/`.onRegister()`/
  `.transform()`) that must become a direct `register(id, factory, settings)` call (see
  `F:\CreateModAddon`'s `ModBlocks.java` for the pattern) plus explicit follow-up
  statements for whatever each removed builder call was actually doing at runtime
  (stress values, connectivity/casing registration, feature toggle registration, tags —
  **not** blockstate/item model generation, since assets are already static JSON per the
  earlier correction). This is genuinely ~1500+ lines of careful one-by-one manual
  conversion across `CCBlocks`/`CCItems`/`CCBlockEntityTypes`/`CCBuilderTransformers`
  (28 files total use Registrate), not a mechanical pass — attempted a smaller file
  first (`CCItems.java`, 105 lines) and confirmed even that can't be converted in
  isolation since several items reference `CCBlocks.SOME_BLOCK` for
  `FeatureToggle.registerDependent(...)`, i.e. `CCBlocks` and `CCItems` are mutually
  load-bearing and should be converted together, block-registration-core first.
- Ran a full `./gradlew compileJava` against the current (partially-converted) tree to
  characterize remaining scope: **200+ compile errors** reported (javac likely caps
  output before showing the true total) across `registries/`, `content/`, `mixin/`,
  `datagen/`, `ponder/`, `compat/`, plus the entrypoint files — consistent with "not
  started yet" for everything except `config/`.
- `mixin/` (47 files), `content/` (177 files), and the ~15 NeoForge-capability block
  entities are **entirely untouched** — still raw NeoForge/Registrate source with
  import roots swapped by the mechanical sweep only.
- Loom access widener for the 6 AT entries: **not started**. Create Fly ships its own
  `create.accesswidener` in the jar (confirmed present) as a real precedent for the
  mechanism working on this exact dependency; this mod's own `.accesswidener` file and
  `loom { accessWidenerPath }` wiring in `build.gradle` still need to be added.

## Key API facts already confirmed (from reading Railway and CreateModAddon source) — some superseded by corrections above, kept for additional context
- **Create Fly's Java package root is `com.zurrtum.create`**, NOT `com.simibubi.create`.
  Client-only Create Fly classes live under `com.zurrtum.create.client.*` (this is a
  real divergence point — some classes that were NOT client-only in vanilla/NeoForge
  Create moved to a client subpackage in Create Fly, e.g. `ItemDescription` moved to
  `com.zurrtum.create.client.foundation.item`). Expect import-root swaps to not always
  be 1:1 package-for-package; verify each moved class against the actual jar (see
  "How to inspect the Create Fly jar" below) rather than assuming a blind find/replace
  is complete.
- Create Fly still exposes `com.zurrtum.create.foundation.data.CreateRegistrate`
  (confirmed via Railway's `CRBlocks.java`), i.e. the **Registrate-based registration
  pattern the original NeoForge code uses can likely be preserved in shape**, just with
  import roots swapped — IF a Fabric-compatible Registrate library is on the classpath.
- **However**, `F:\CreateModAddon` (the exact same MC+Create Fly version as this port)
  does **NOT** depend on any external Registrate artifact at all, and instead registers
  blocks/items directly via vanilla Fabric patterns
  (`Registry.register(BuiltInRegistries.BLOCK, ...)`, see
  `F:\CreateModAddon\src\main\java\cc\spea\createmodaddon\registry\ModBlocks.java`).
  This strongly suggests that for Create Fly 6.0.9-5, `com.tterrag.registrate.*` /
  `CreateRegistrate` classes are bundled inside the Create Fly mod jar itself (nested
  jar-in-jar) and become available transitively via `modImplementation` — i.e. you
  likely do **NOT** need to add any separate Registrate maven dependency or repository.
  **Verify this empirically first** (see below) before reaching for Railway's approach.
- Railway (`F:\Railway`) uses Registrate too, but via a hand-patched, binary-rewritten
  fork of `com.tterrag.registrate_fabric:Registrate:1.3.79-MC1.20.1` (see
  `patchRegistrateJar` etc. in `F:\Railway\build.gradle.kts` around line 169-384). That
  patched jar targets old intermediary names for a much older MC/Create combo and is
  **not a safe drop-in** for this port. Treat Railway as a secondary reference only for
  general "how does a Fabric mixin/registry/API call look against Create Fly classes"
  patterns (e.g. `common/src/main/java/com/railwayteam/railways/registry/CRBlocks.java`,
  and mixins under `F:\Railway\fabric\src\main\java\com\railwayteam\railways\fabric\mixin\`),
  not for build-script copy-paste.
- Entry points: NeoForge's `@Mod` constructor + `IEventBus` pattern must become Fabric's
  `ModInitializer`/`ClientModInitializer` with `onInitialize()`/`onInitializeClient()`
  (see `CreateModAddon.java` / `CreateModAddonClient.java` for the minimal shape).
  `RegisterEvent` handling (`CreateConnected.onRegister`) goes away — Fabric registration
  happens directly/eagerly via static initializers or an explicit `register()` call from
  `onInitialize()`.
- `MixinPlugin.java` (`src/main/java/com/hlysine/create_connected/mixin/MixinPlugin.java`)
  uses `net.neoforged.fml.loading.FMLLoader.getLoadingModList()` to check which mods are
  loaded — replace with `net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId)`.
- Access transformers (`META-INF/accesstransformer.cfg`, 6 entries for `RecipeProvider`,
  `ButtonBlock`, `JukeboxBlockEntity`, `LevelRenderer`, `Ingredient`) are a NeoForge-only
  mechanism — Fabric has no AT support. Each entry needs to become either a Mixin
  accessor/invoker interface, or the code that needed the widened access needs
  restructuring. Check each of the 6 usages individually.

## How to inspect the Create Fly jar / verify API surface
Don't guess API shapes — verify against the real artifact:
1. `./gradlew build` (or any task that triggers dependency resolution) will download
   `create-fly-1.21.11-6.0.9-5.jar` into the Gradle cache
   (`~/.gradle/caches/modules-2/files-2.1/maven.modrinth/create-fly/...`).
2. Loom also decompiles Minecraft + remaps mod jars into
   `~/.gradle/caches/fabric-loom/...` — look for a `-sources.jar` or use
   `unzip -l`/`jar tf` on the plain jar to list class files, and extract+decompile
   individual classes if needed (or just grep `F:\Railway` and `F:\CreateModAddon`
   source for real usage examples of the class you need, since both already compile
   successfully against a Create Fly build).
3. `F:\CreateModAddon` is small (only ~15 files) but is the single most trustworthy
   reference since it's the *exact* same MC/Create Fly version pairing as this port.

## Suggested execution order
1. Get a trivial/no-op mod (`CreateConnected implements ModInitializer` with empty
   `onInitialize`, minimal `CreateConnectedClient`, no content registered yet) building
   and running `./gradlew build` clean, to validate the build script/deps/repos are
   correct before touching the 267 content files.
2. Mass-replace `com.simibubi.create` -> `com.zurrtum.create` across all source
   (`grep -rl com.simibubi.create src` then sed/edit), then re-check compile — this
   will surface the real list of moved/renamed/removed classes to fix one by one.
3. Convert `config/` (NeoForge config framework -> something Fabric-appropriate, e.g.
   a hand-rolled JSON/properties config, or check if Create Fly/Fabric API expose a
   config helper already used elsewhere in the codebase, e.g. check how CreateModAddon
   or Railway do config — Railway has `CRConfigsImpl`, worth a peek).
4. Convert `registries/` (CCBlocks, CCItems, CCBlockEntityTypes, CCCreativeTabs,
   CCDataComponents, CCSoundEvents, CCTags, CCPonderPlugin, CCPartialModels, etc.) —
   this is the load-bearing package everything else depends on.
5. Split client-only classes into `src/client/java/...` per `splitEnvironmentSourceSets()`.
6. Convert `mixin/` (47 classes) one at a time, adjusting `@Mixin` targets to
   `com.zurrtum.create.*` equivalents; remove/rewrite the 6 access-transformer-dependent
   spots as mixin accessors instead.
7. Work through `content/` (~150 files across all the kinetic block features) last,
   package by package — this is the bulk of the file count but should be the most
   mechanical once steps 2-6 establish working patterns.
8. Delete/regenerate `src/generated/resources` (currently contaminated with datagen
   output from ~10 unrelated mods — was clearly copied from a shared workspace). Once
   the mod compiles, run the datagen entrypoint (check `datagen/` package for how it's
   wired, likely needs a `fabric-datagen` entrypoint added to `fabric.mod.json` per
   Railway's example: `"fabric-datagen": ["...DataFabric"]`) to regenerate cleanly for
   just this mod, or delete the file entirely if datagen isn't wired yet and defer.
9. Iterate `./gradlew build` (or `compileJava`/`compileClientJava` first, faster
   feedback) until clean. Then do a full `build`.

## PROGRESS session 2: registration core (registries/ + entrypoint)
`registries/` (25 files, ~1700 lines) and `config/` (9 files) are now **fully converted and
verified compiling in isolation** (compile-gated the same way as config/ in session 1: moved
`content/`, `compat/`, `mixin/`, `datagen/`, `ponder/` aside, confirmed zero errors originate
from `registries/*.java` in the resulting compileJava output, then restored everything).
`CreateConnected.java`/`CreateConnectedClient.java` entrypoints are also rewritten (real, not
stubs) and call into every converted registry in `onInitialize()`.

### Key facts confirmed this session (trust over earlier guesses)
- **Registrate removal confirmed exhaustively**: went through all ~50 blocks in `CCBlocks.java`,
  `CCItems.java`, `CCBlockEntityTypes.java`, and every other registries file. Pattern used
  throughout: `CCRegistrate.block(path, factory, properties)` /
  `CCRegistrate.blockItem(block, path, [factory], [properties])` /
  `CCRegistrate.item(path, factory, [properties])` (new helper class,
  `registries/CCRegistrate.java`, modeled on `CreateModAddon`'s `registerWithItem`). All
  `.blockstate()/.model()/.lang()/.loot()/.tag()` builder calls dropped (pure datagen, assets
  already static). Real runtime behavior preserved as explicit statements after each
  registration: `CStress.setImpact/setNoImpact/setCapacity(block, ...)`,
  `BlockStressValues.setGeneratorSpeed(block, rpm)`, `FeatureToggle.register/registerDependent/
  addCondition(id, ...)`, `EncasingRegistry.addVariant(base, encased)`,
  `BlockMovementChecks.registerAttachedCheck/registerBrittleCheck`, `MovementBehaviour.REGISTRY
  .register(block, behaviour)`, `MovingInteractionBehaviour.REGISTRY.register(...)`,
  `MountedItemStorageType.REGISTRY.register(...)` / `MountedFluidStorageType.REGISTRY.register(...)`,
  `DisplaySource.BY_BLOCK.add(...)` (note: `.add`, not `.register` - `BY_BLOCK` is a
  `SimpleRegistry.Multi`), `DisplayTarget.BY_BLOCK.register(...)` (plain `SimpleRegistry`, so
  `.register` here is correct).
- **`com.zurrtum.create.api.registry.CreateBuiltInRegistries` does not exist** (verified: not in
  the sources jar at all). The mod's own `CCItemAttributes`/`CCArmInteractionPointTypes` imported
  it and would not have compiled even pre-port. Real constants are
  `com.zurrtum.create.api.registry.CreateRegistries.ITEM_ATTRIBUTE_TYPE` /
  `.ARM_INTERACTION_POINT_TYPE` /  `.DISPLAY_SOURCE` / `.DISPLAY_TARGET` /
  `.MOUNTED_ITEM_STORAGE_TYPE` / `.MOUNTED_FLUID_STORAGE_TYPE` (all real `Registry<T>`
  instances), paired with `com.zurrtum.create.api.registry.CreateRegistryKeys` for the
  matching `ResourceKey`s. Pattern:
  `Registry.register(CreateRegistries.X, ResourceKey.create(CreateRegistryKeys.X, id), value)`.
  Verified against Create Fly's own `AllDisplaySources`/`AllMountedStorageTypes`/etc, which use
  exactly this pattern for their own entries.
- **`com.simibubi.create.foundation.data.SharedProperties` (and the whole `foundation.data`
  Registrate-adjacent package: `BuilderTransformers`, `AssetLookup`, `CreateRegistrate`) is
  gone.** Added `registries/CCSharedProperties.java` as a from-scratch reconstruction
  (`stone()/wooden()/softMetal()/copperMetal()` presets) - hardness/sound values are a
  reasonable approximation, **not verified against original Create's real values**, since that
  source no longer exists anywhere in this dependency chain. Only affects mining feel, not
  correctness. `registries/CCBuilderTransformers.java` similarly reconstructed
  (`copycatProperties()` -> `noOcclusion()`, since copycat blocks' own properties barely matter
  given they mimic whatever block they're placed as).
- **Block properties "copy from an existing block" is `BlockBehaviour.Properties.ofFullCopy
  (Block)`**, not `block.properties()` (verified against Create Fly's own `AllBlocks.java`,
  which uses this pattern extensively for e.g. `CogwheelBlock`/`CopycatStepBlock`).
- **`com.zurrtum.create.foundation.block.ItemUseOverrides` does not exist** (verified absent
  from both the sources jar and the compiled jar's class listing). The mod's own
  `ItemUseOverridesMixin` targets this now-nonexistent class and **will not compile as-is** -
  Create Fly moved this "precise hit location" mechanism into direct mixins on vanilla's
  `ServerPlayerGameMode`/`MultiPlayerGameMode` (see `com.zurrtum.create.mixin
  .ServerPlayerGameModeMixin` / `com.zurrtum.create.client.mixin.MultiPlayerGameModeMixin` in
  the real jar for the pattern to follow). This is now a concrete, scoped task for the `mixin/`
  conversion pass - not yet done. `registries/PreciseItemUseOverrides.java` itself (the
  `Set<Identifier> OVERRIDES` tracker) is fine as-is and needs no changes.
- **Connected-texture behaviour (`CreateRegistrate.connectedTextures`/`.casingConnectivity`) is
  entirely client-only** in Create Fly (`com.zurrtum.create.client.content.decoration.encasing
  .EncasedCTBehaviour`, wired centrally via `com.zurrtum.create.client.AllCTBehaviours`, not
  per-block onRegister hooks anymore). Two blocks (`PARALLEL_GEARBOX`, `BRASS_GEARBOX`) had
  their CT-behaviour registration **dropped, not replaced** - they'll render without connected
  textures on their casing faces until a client-sourceset `CCCTBehaviours`-style class is
  written. Marked with inline comments at both call sites in `CCBlocks.java`.
- **`.color()` block/item tint registration was also dropped** for `FAN_SPLASHING_CATALYST`
  (water-tinted catalyst) - Fabric's equivalent is `ColorProviderRegistry.BLOCK`/`.ITEM` from
  `fabric-rendering-v1` (client-only), not yet wired. `registries/CCColorHandlers.java` (now
  under `src/client/java/`) still provides the tint functions themselves, just nothing calls
  `ColorProviderRegistry.BLOCK.register(...)`/`.ITEM.register(...)` with them yet.
- **`KineticBatteryBlockItem.registerModelOverrides()` (custom item-model-predicate overrides
  for battery charge level) was dropped**, same reason - it called
  `CatnipServices.PLATFORM.executeOnClientOnly(...)`, which doesn't exist (see below), and is
  inherently client-only rendering logic. Needs a client-sourceset home.
- **`com.zurrtum.create.catnip.platform.CatnipServices` (the NeoForge/Fabric multiloader
  service-locator abstraction) does not exist in Create Fly's Fabric-only catnip** - confirmed
  by real compile error in `config/FeatureToggle.java` (already fixed this session, see session
  1 notes) and now again via `CCColorHandlers`/`KineticBatteryBlockItem`. Any remaining
  `CatnipServices.PLATFORM.executeOnClientOnly/executeOnServerOnly(...)` call sites found during
  the `content/`/`mixin/` pass need the same fix: either split the code into a real
  client-sourceset class, or (for server-only checks) just delete the wrapper since Fabric
  doesn't need a multiloader indirection.
- **`CCCraftingConditions.java` deleted outright** (not just left broken): it registered a
  `MapCodec<ICondition>` for NeoForge's `neoforge:conditions` recipe-JSON gating system, which
  **does not exist on Fabric at all**. This is a real, unresolved functionality gap, not a
  cosmetic one: **274 of the mod's 275 committed recipe JSON files
  (`src/generated/resources/data/create_connected/recipe/**`) carry a
  `"neoforge:conditions": [{"type": "create_connected:feature_enabled", "tag": "..."}]` block**
  gating the recipe on `FeatureToggle` state. Fabric's vanilla recipe loader will very likely
  either choke on or silently ignore this unrecognized key (untested which), meaning **disabled
  features' recipes may load unconditionally once this compiles**. This needs a dedicated fix
  pass: likely a `SimpleResourceReloadListener`/`RecipeManager`-wrapping mechanism that strips
  or filters recipes carrying that tag against live `FeatureToggle.isEnabled(...)` state at
  reload time. Tracked here as a concrete, scoped follow-up - **do not just strip the JSON key
  and call it done**, that silently drops real behavior.
- **`CCSoundEvents.java` heavily reworked**: `DeferredHolder<SoundEvent,SoundEvent>` ->
  `Holder.Reference<SoundEvent>` via `Registry.registerForHolder(...)`; merged the old
  two-phase `prepare()` (create lazy holder) + `register(RegisterEvent)` (actual registration)
  into a single eager `register()` method, since Fabric has no `RegisterEvent` phase to defer
  to. Deleted `SoundEntryProvider` (NeoForge datagen for `sounds.json`) entirely - already
  static at `src/generated/resources/assets/create_connected/sounds.json`.
- **Jukebox songs (`CCJukeboxSongs.java`) simplified to bare `ResourceKey` constants** - these
  are a datapack registry (JSON-driven), datagen output already static at
  `src/generated/resources/data/create_connected/jukebox_song/*.json`, no registration code
  needed at all, unlike most other registries here.
- **`net.minecraft.resources.Identifier`, `net.minecraft.client.color.*`,
  `net.minecraft.client.renderer.*` etc. are genuinely unavailable to the `main` source set**
  under `splitEnvironmentSourceSets()` - confirmed by real compile errors, not assumption. Any
  file importing `net.minecraft.client.*` MUST move to `src/client/java` or it fails outright
  (this is why `CCGuiTextures`, `CCColorHandlers`, `CCPonderPlugin` were relocated this
  session). `content/` (177 files, untouched) will have many more of these to sort during its
  own conversion pass - expect renderers, screens, and anything using Flywheel visuals to all
  need moving.
- Full `./gradlew compileJava` against the current tree: **200 errors reported (javac's error
  cap, true count is higher)**, confirmed via package-path breakdown to originate entirely from
  `content/` (~92), `compat/` (~80, minus `Mods.java` which is done), and `ConnectedLang.java`
  (~12, not yet moved to client - `com.zurrtum.create.catnip.lang` doesn't exist at that path,
  likely moved to `com.zurrtum.create.client.catnip.lang`, unverified). **Zero errors from
  `registries/`, `config/`, or the entrypoint files** - that unit is solid.
- Lesson learned and now standing practice: **before trusting any `javap`-inspected jar found
  via `find ~/.gradle/caches`, cross-check it's the version actually resolved for this
  project** with `./gradlew dependencies --configuration modImplementation` - multiple stale
  versions of the same Fabric API artifact (e.g. `fabric-networking-api-v1` 1.3.11 vs the real
  5.1.5, `fabric-item-group-api-v1` 4.0.12 vs the real 4.2.36) sit side-by-side in the shared
  Gradle cache from other projects on this machine, and their APIs differ substantially between
  versions.

## PROGRESS session 3: compat/, ConnectedLang, mixin/, access widener
`compat/` (7 files), `ConnectedLang.java`, and `mixin/` (51 files, not 47 - actual count) are now
**converted and verified compiling clean** (full `./gradlew compileJava` + `compileClientJava`
run against the whole tree: **100% of remaining errors are in `content/`**, zero from
`compat/`/`config/`/`registries/`/`mixin/`/entrypoints). Access widener added for the 6 former AT
entries.

### External soft-dependency mods: real availability checked via web search, not guessed
Verified via Modrinth (current as of this session, July 2026) which of the mod's optional
compat integrations have any Fabric build for **MC 1.21.11 specifically**:
- **JEI**: yes, real build exists (`maven.modrinth:jei:27.4.0.17` used here). Added a real
  `modCompileOnly` dependency in `build.gradle`/`gradle.properties` (`jei_version` property).
  `compat/CreateConnectedJEI.java` moved to `src/client/java` (it's inherently client-only -
  `Minecraft.getInstance()`) and converted off `ItemProviderEntry`/`NeoForge.EVENT_BUS`.
- **Create: Copycats+**: no. Modrinth tops out at `1.21.1-fabric`. `compat/CopycatsManager.java`
  kept its full public API (all ~7 call sites across `content/`/`mixin/` still compile against
  it unchanged) but `BLOCK_MAP`/`ITEM_MAP` are now empty static maps instead of referencing
  `com.copycatsplus.copycats.*` classes that don't exist on this platform - this makes
  `convert()`/`existsInCopycats()`/`isFeatureEnabled()` safe no-ops, which matches what would
  happen anyway since `Mods.COPYCATS.isLoaded()` is currently always false. Re-populate the maps
  once/if Copycats+ ships 1.21.11 Fabric support - no other code changes needed.
- **Additional Placements**: no. Modrinth tops out at `1.21.3-1.21.5-fabric`.
  `compat/AdditionalPlacementsCompat.java` reduced to a documented no-op `register()`.
- **Create Aeronautics / "Simulated"**: no Fabric build found for 1.21.11 at all.
  `compat/SimCompatRegistry.java` reduced to a documented no-op `register()` - the
  `LinkedThrottleLeverBlock`/`BlockEntity`/`Renderer` classes it would have wired up already
  exist under `content/linkedtransmitter/` and are self-contained (only referenced from this one
  file), so re-enabling later is just re-adding the direct-registration calls (pattern: see
  `CCBlocks.LINKED_LEVER`/`LINKED_ANALOG_LEVER`).
- **Dye Depot**: no Fabric build for 1.21.11 either, but `compat/DyeDepotCompat.java` never
  referenced Dye Depot's actual classes (just `Mods.DYE_DEPOT.isLoaded()` + a namespace string),
  so it needed no changes at all.
- General technique used throughout this sub-pass, worth reusing: `WebSearch`/`WebFetch` for
  "<mod name> fabric modrinth" to check real current version support **before** deciding whether
  to add a `modCompileOnly` dependency vs. write a documented no-op - don't guess Modrinth
  coordinates or assume a mod has caught up to the target MC version.

### mixin/ conversion facts (verified against the real Create Fly jar/sources)
- Cross-checked **every** mixin's `@Mixin(X.class)` target import against the real Create Fly
  sources jar programmatically (loop over all mixin files, resolve each `com.zurrtum.create.*`
  import to a file path, check existence) rather than opening each of 51 files one at a time -
  much faster than the per-file archaeology used for `registries/`. Found and fixed 6 relocated
  targets (moved into `com.zurrtum.create.client.*` subpackages: `ValueBoxRenderer`,
  `ClientSchematicLoader`, `AbstractSimiScreen`, `SequencedGearshiftScreen`, `AllGuiTextures`,
  `ScrollInput`) by inserting `.client.` into the import path, then re-ran the same check to
  confirm zero remaining missing targets.
- Moved 5 mixin files to `src/client/java` whose targets turned out to be client-only:
  `ValueBoxRendererMixin`, `AbstractSimiScreenAccessor`, `SequencerInstructionsMixin` (plus the
  2 already-flagged-client `ClientSchematicLoaderMixin`/`SequencedGearshiftScreenMixin` from the
  original scaffolding), with matching moves between `create_connected.mixins.json`'s `mixins`
  and `client` arrays. **`SequencerInstructionsMixin` is a judgment call worth double-checking**:
  its target enum `SequencerInstructions` (not itself flagged client-only) has a constructor
  parameter of type `AllGuiTextures` (client-only) baked into the enum's own bytecode in the real
  jar - moved the whole mixin to client on the assumption the enum itself is therefore
  effectively client-loadable-only, but this wasn't independently verified (e.g. by checking
  whether Create Fly's dedicated server jar variant excludes/stubs this enum differently).
- **`SubMenuConfigScreenMixin` deleted outright**: its target
  `com.zurrtum.create.catnip.config.ui.SubMenuConfigScreen` doesn't exist anywhere in Create Fly
  - the whole NeoForge/Forge-Config-API-Port "mod config screen" GUI integration concept is gone
  (no `ConfigScreen`/`SubMenuConfigScreen` classes at all under `catnip/config/ui/`, which now
  only has `ConfigAnnotations`). The mixin's actual purpose (re-sync config to all players after
  editing it live via that GUI, calling `CCConfigs.common().syncToAllPlayers()`) is a minor
  redundant path anyway - `SyncConfigBase` (session 1) already syncs proactively on
  `ServerPlayConnectionEvents.JOIN`, so only the "instant re-sync without reconnecting after a
  live edit" edge case is lost, not the core sync mechanism.
- **`ItemUseOverridesMixin` redesigned per the coordinator's own scoping**: retargeted from the
  nonexistent `com.zurrtum.create.foundation.block.ItemUseOverrides` onto vanilla
  `net.minecraft.server.level.ServerPlayerGameMode`, injecting via `@ModifyVariable` on the
  `BlockHitResult` parameter of `useItemOn(...)` at `@At("HEAD")` - swaps in a re-picked precise
  hit result for blocks in `PreciseItemUseOverrides.OVERRIDES` before vanilla's block-use
  dispatch runs, same net effect as the original `BlockHelper.invokeUse` wrap. Verified the
  injection point/pattern is sound by reading Create Fly's own real
  `com.zurrtum.create.mixin.ServerPlayerGameModeMixin` (confirmed it mixes into the exact same
  `useItemOn` method for its own unrelated overrides, proving the target/method is real and
  mixin-compatible) - **not independently verified against actual bytecode that
  `@ModifyVariable(argsOnly=true)`'s parameter-ordering convention (target variable in its
  original declared position, not always first) is satisfied by my handler signature**; if Loom's
  Mixin annotation processor rejects it, the fix is almost certainly just reordering the handler
  method's parameters.
- **`ManualApplicationRecipeMixin` similarly redesigned**: original target
  `com.zurrtum.create.content.kinetics.deployer.ManualApplicationRecipe` is now just a recipe
  *type* record, not where the apply-in-world logic lives. Retargeted onto
  `ManualApplicationHelper.manualApplicationRecipesApplyInWorld(...)` (verified via the real
  source - this static method contains the exact `ItemStack.shrink(1)` call the original mixin
  hooked), replacing the NeoForge `PlayerInteractEvent.RightClickBlock` parameter with the
  method's own real `(Level, Player, ItemStack, InteractionHand, BlockHitResult, BlockPos)`
  parameters (now directly available, no event object needed at all).
- **`PackagerBlockMixin`**: NeoForge's `net.neoforged.neoforge.common.util.FakePlayer` replaced
  with Create Fly's own `com.zurrtum.create.api.entity.FakePlayerHandler.has(player)` (a real
  utility that itself wraps Fabric API's `net.fabricmc.fabric.api.entity.FakePlayer` +
  Create Fly's own `FakePlayerEntity`, verified present in the real jar) -
  `!(player instanceof FakePlayer)` -> `!FakePlayerHandler.has(player)`.
  **Flagged, not resolved**: this mixin also needed `CreateLang` for a chat message, which - like
  `ConnectedLang` - turned out to be client-only (`com.zurrtum.create.client.foundation.utility
  .CreateLang`). Left as-is (import path fixed to compile) rather than moved to client, because
  `getStateForPlacement` (the method being mixed into) plausibly needs to run on the *server* to
  actually reject block placement, and moving the whole mixin to client would silently break that
  - but this means the `CreateLang.translate(...).sendStatus(player)` call inside it might crash
  on a dedicated server if `CreateLang` genuinely requires client-only classes at runtime. Needs
  real verification (either confirm `CreateLang`'s client restriction is compile-time packaging
  convention only and the class is safe to load server-side, or replace the chat-message send
  with a raw `player.sendSystemMessage(Component.translatable(...))` call instead of going
  through `CreateLang`) - flagged rather than guessed at.
- **Access widener added** (`src/main/resources/create_connected.accesswidener`, wired via
  `fabric.mod.json`'s `"accessWidener"` key) replacing all 6 original
  `META-INF/accesstransformer.cfg` entries (file deleted, dead on Fabric). 3 of the 6 entries
  (`RecipeProvider.getName()`, `LevelRenderer.notifyNearbyEntities(...)`,
  `Ingredient.<init>(Stream)`) had their full method descriptors already given verbatim in the
  original AT file, so those are high-confidence. **The other 3 (`ButtonBlock.type`,
  `ButtonBlock.ticksToStayPressed`, `JukeboxBlockEntity.jukeboxSongPlayer`) needed field-type
  descriptors inferred from general Minecraft-modding knowledge, not independently verified
  against decompiled 1.21.11 bytecode** (no pre-remapped named-mappings jar was readily available
  in the local Loom cache to check against without further decompile tooling setup) - most likely
  to need a fix: `JukeboxBlockEntity$SongPlayer` is a guess at the inner class name/type for the
  `jukeboxSongPlayer` field. Loom will raise a clear, specific error at build time if any of
  these three descriptors are wrong (access widener application fails loudly, not silently), so
  this is a fast, unambiguous first fix if `./gradlew build` fails at the widener-application
  step.
- Full `./gradlew compileJava` and `compileClientJava` now report errors **exclusively from
  `content/`** (100 errors each, both capped at javac's limit) - `compat/`, `config/`,
  `registries/`, `mixin/`, and both entrypoints are a fully clean, mutually-consistent unit.

## PROGRESS session 4: content/ conversion started (PackagerBlockMixin fixed, ~60 files relocated)
- **`PackagerBlockMixin`'s dedicated-server crash risk resolved** (flagged session 3): replaced
  `CreateLang.translate("packager.no_portable_storage").sendStatus(player)` with a raw
  `player.displayClientMessage(Component.translatable("create.packager.no_portable_storage"),
  true)` - verified the exact translation-key prefix (`create.`, from `Create.MOD_ID`) and that
  `sendStatus` is literally just `displayClientMessage(component, true)` under the hood by
  reading `LangBuilder.sendStatus`/`CreateLang` in the real sources jar, not guessed.
- **Built a full, verified content/ import-relocation map** the same way as the mixin/ pass
  (programmatic cross-check of every `com.zurrtum.create.*` import against the real sources jar
  across all 177 files at once, far faster than per-file archaeology). Of ~70 unique missing
  imports found: **38 were mechanical `.client.` package-prefix insertions** (batch-applied via
  sed across the whole tree in one pass) and a further ~10 were relocations to a different real
  path (e.g. `AllAdvancements` moved from `foundation.advancement` to top-level `com.zurrtum
  .create`, `ValueSettingsBoard`/`Formatter` moved to `client.foundation.blockEntity` (dropped
  the `.behaviour` segment), `ClipboardEntry` moved to `infrastructure.component`). A few
  "missing" hits were false positives from the check script not handling nested classes
  (`RedstoneLinkNetworkHandler.Frequency`, `FluidHelper.FluidExchange` (an enum, script only
  checked for `class`/`interface`) - both actually fine as originally imported.
- **Genuinely gone, no replacement found** (used elsewhere too, not just content/):
  `AllTags` (unused import, safely deleted), `catnip.annotations.ClientOnly` (a no-op
  documentation marker annotation, safely deleted along with its usages),
  `catnip.platform.CatnipServices`/`NeoForgeCatnipServices` (the multiloader indirection layer -
  confirmed absent a 3rd time now across sessions), `foundation.ICapabilityProvider` +
  `foundation.mixin.accessor.ItemStackHandlerAccessor` (NeoForge capability system - these mark
  files that belong to the still-pending "~15 capability block entities -> Fabric Transfer API"
  work item, not simple import fixes), `foundation.blockEntity.behaviour.inventory
  .VersionedInventoryWrapper`, `foundation.blockEntity.renderer.SafeBlockEntityRenderer`,
  `foundation.data.AssetLookup`/`SpecialBlockStateGen` (Registrate-datagen, drop usages per the
  established rule - assets are static), `foundation.fluid.SmartFluidTank` (a
  `SmartFluidTankBehaviour` exists but isn't a drop-in, needs real per-usage investigation),
  `foundation.model.BakedQuadHelper` (only `BakedModelHelper` exists nearby, not obviously the
  same API), `foundation.networking.BlockEntityConfigurationPacket` (NeoForge networking, needs
  a real Fabric networking replacement, not yet done), `catnip.net.base.ClientboundPacketPayload`
  (same networking-base-class problem). None of these are fixed yet - flagged, not addressed.
- **`CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> X())` fix pattern established**: in
  every content/ instance found so far, the call site was *already* guarded by an
  `isClientSide()`/similar check before being reached, so the safe fix is just calling `X()`
  directly and dropping the `@OnlyIn(Dist.CLIENT)` annotation on the target method (which Fabric
  doesn't need/read) - **not** moving the whole containing class to `src/client/java`, since the
  class also has legitimate shared-side members (Item/Block classes are loaded on both sides
  regardless). This relies on the JVM's lazy per-method bytecode verification (a method's body
  referencing client-only types is only resolved when that specific method is actually invoked,
  not at containing-class load time) - standard practice in real Fabric mods for small optional
  client-only helper methods living in an otherwise-shared class, but **do not extend this
  pattern to methods that are unconditionally called from shared code** - only apply where an
  existing `isClientSide()`-style guard already fully protects the call site. Applied to
  `KineticBridgeBlockItem.showBounds()` as sample; `KineticBatteryBlockItem`,
  `SequencedPulseGeneratorBlock`'s two call sites still need the same treatment.
- **Bulk-moved ~60 content/ files to `src/client/java`** (any file transitively importing
  `com.zurrtum.create.client.*` or `net.minecraft.client.*`), preserving the relative package
  path under `content/`.
- **Critical correction, found the hard way**: bulk-moving by "does this file import a
  client-only class" is **too crude for `*Block.java`/`*BlockEntity.java` files** - 12 of the 60
  moved files were Block/BlockEntity classes that got swept along only because they reference one
  client-only behaviour class (most commonly `ScrollValueBehaviour`, now under
  `client.foundation.blockEntity.behaviour.scrollValue` - odd, since "value box" scroll
  interactions read as gameplay/redstone-value logic, not pure rendering, but that's where Create
  Fly put it). Moving the *whole* BlockEntity broke its sibling `*Block.java` class (which stayed
  in `main` and needs the BlockEntity type for `getBlockEntityClass()`/`getBlockEntityType()`
  etc.), and BlockEntities generally have real server-tick/NBT logic that must not move to
  client. **Reverted these 12 back to `main`** rather than compound the mistake (they still have
  real compile errors from the `ScrollValueBehaviour`-family imports, left unresolved):
  `CentrifugalClutchBlockEntity`, `CrankWheelBlockEntity`, `FluidVesselBlockEntity`,
  `FreewheelClutchBlockEntity`, `InventoryBridgeBlockEntity`, `KineticBatteryBlockEntity`,
  `KineticBridgeBlockEntity`, `LinkedAnalogLeverBlockEntity`, `LinkedThrottleLeverBlockEntity`,
  `LinkedTransmitterBlockEntity`, `OverstressClutchBlockEntity`,
  `SequencedPulseGeneratorBlock` (this last one is a Block, not BlockEntity, but same issue with
  its own `displayScreen` client bits).
  **Next-session priority**: figure out Create Fly's real client/server split for the
  scroll-value/filtering/display "behaviour" classes (`ScrollValueBehaviour`,
  `FilteringBehaviour`, `ValueSettingsBoard`, etc. - all confirmed client-only in the real jar) -
  likely these BlockEntities need the behaviour *instance* itself kept server-side (for
  NBT/sync) while only the *rendering/GUI* of that behaviour is client-only, meaning Create Fly
  probably split each "Behaviour" concept into a server data-holder + separate client renderer,
  and this mod's BlockEntity classes need the equivalent split, not a wholesale move. Don't
  bulk-move any more `*Block.java`/`*BlockEntity.java` files without first checking exactly
  which of their members actually need the client-only type - only move (or extract to a new
  client-only companion class) the specific offending method/field.
- **Rule of thumb for the remaining ~117 content/ files still in `main`** (post-revert): the
  bulk-move heuristic is safe and correct for anything named `*Renderer`/`*Visual`/`*Model`/
  `*Screen`/`*Item` (item classes with pure client-only helper methods, per the
  `executeOnClientOnly` pattern above) but needs manual per-member inspection for `*Block`/
  `*BlockEntity`/`*Generator`/behavior classes - check what specifically is client-only before
  moving the whole file.
- State after this session: `./gradlew compileJava` errors are down to ~22 unique files (from
  ~92+ before this session's fixes), still capped/undercounting since `compileClientJava` is
  separately still at its own 200-error cap across the 60 moved files (many of which have their
  own remaining real fix-needed imports from the "genuinely gone" list above, not yet resolved).
  This is real, verified progress, not yet a clean package.

## PROGRESS session 5: the real ScrollValueBehaviour/LinkBehaviour split pattern, cracked
- **Solved the architectural question flagged at the end of session 4.** Read Create Fly's real
  `ScrollValueBehaviour` (client) and confirmed via its constructor
  (`behaviour = blockEntity.getBehaviour(ServerScrollValueBehaviour.TYPE)`) that it's a **thin
  client-only decorator** delegating all real state to a same-named-but-`Server`-prefixed class
  living in the **non-client** package at the same relative path
  (`com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour`).
  Verified this is a **general, repeated pattern**, not a one-off: `LinkBehaviour` (client) also
  delegates to `com.zurrtum.create.content.redstone.link.ServerLinkBehaviour` (non-client) the
  exact same way. **Rule for all remaining content/ files hitting this issue**: when a
  `client.*.XyzBehaviour` class is needed by a BlockEntity for real state (not just UI), look for
  `Server<Xyz>Behaviour` (or `Server<Xyz>` for non-"Behaviour"-suffixed names) at the same path
  with `client.` stripped - it's very likely there and is the real state holder to use instead.
  Even better: **Create Fly ships ready-made subclasses for exactly this mod's use cases** -
  `ServerKineticScrollValueBehaviour` (bidirectional forward/reverse speed threshold) and
  `ServerScrollOptionBehaviour<E extends Enum<E>>` (cycling an enum) already implement, byte for
  byte, the same `setValueSettings`/`getValueSettings` logic this mod's own
  `RotationScrollValueBehaviour`/`ScrollOptionBehaviour<RotationDirection>` usages needed -
  **this mod's own `RotationScrollValueBehaviour` custom class is now entirely redundant** and
  was deleted from `CentrifugalClutchBlockEntity`'s usage (file itself,
  `src/client/java/.../content/RotationScrollValueBehaviour.java`, is now dead code - not yet
  deleted, low priority since it doesn't block compilation from where it sits).
- **Fixed 5 of the 12 reverted BlockEntity/Block files** using this pattern:
  `CentrifugalClutchBlockEntity` (-> `ServerKineticScrollValueBehaviour` directly, no custom
  subclass needed), `FreewheelClutchBlockEntity` (-> `ServerScrollOptionBehaviour<RotationDirection>`
  directly), `KineticBridgeBlockEntity` (added new
  `content/kineticbridge/ServerStressImpactScrollValueBehaviour.java` - a custom subclass was
  still needed here since the stress-multiplier math (`convertValue`) and value-clamping logic
  is bespoke, not covered by a ready-made Create Fly class; the client-only
  `StressImpactScrollValueBehaviour` in `src/client/java` keeps its own duplicate `convertValue`
  rather than cross-referencing the server class), `OverstressClutchBlockEntity` (custom
  `TimeDelayScrollValueBehaviour` inner class split: server half kept
  `setValueSettings`/`getValueSettings`/`onShortInteract`/`getClipboardKey`
  extending `ServerScrollValueBehaviour`, client-only `createBoard()`/`formatSettings()`
  **dropped, not reimplemented** - the block still functions with default vanilla-shaped
  scroll-value UI, just without the mod's custom "ticks/seconds/minutes" row labels),
  `LinkedTransmitterBlockEntity` + `LinkedAnalogLeverBlockEntity` (-> `ServerLinkBehaviour
  .transmitter(be, signalSupplier)`, dropped the `ValueBoxTransform` slots parameter entirely -
  that's client-only frequency-slot hit-testing UI, no longer constructed on the server side at
  all).
- **`OverstressClutchBlockEntity.addToTooltip()` kept in `main` with client-only
  `ConnectedLang`/`TooltipHelper`/`FontHelper` calls intact**, applying the same
  lazy-verification reasoning as `KineticBridgeBlockItem.showBounds()` (session 4) - `addToTooltip`
  is only ever invoked from client-side goggle-overlay rendering, never from server logic, so the
  containing class loads fine on a dedicated server as long as this specific method is never
  called there.
- **Discovered `dev.simulated_team.simulated` (the "Simulated"/Create Aeronautics mod) has
  literally no Fabric build for any 1.21.x version compatible with this port** (confirmed session
  3) means 4 files **cannot compile at all**, not just need import fixes, since they extend/import
  real classes from a dependency that doesn't exist: `LinkedThrottleLeverBlock.java`,
  `LinkedThrottleLeverBlockEntity.java`, `LinkedThrottleLeverRenderer.java` (client),
  `mixin/linkedtransmitter/ThrottleLeverBlockMixin.java`. **Moved all 4 out of `src/` entirely**
  to `F:\create-connected-fly\.claude\worktrees\<worktree>\_disabled_pending_simulated_dependency\`
  (outside any Gradle source set, so they're inert rather than deleted) and removed
  `ThrottleLeverBlockMixin` from `create_connected.mixins.json`. Re-enable by moving them back
  once/if Simulated ships compatible Fabric support - no other code references them (verified),
  so this is a clean, fully reversible exclusion, not a functionality-dropping hack. This is a
  different situation from `SimCompatRegistry.java`'s existing no-op (session 3) - that file
  registered a NEW block via Simulated's own type (`SimBlocks.THROTTLE_LEVER`) which literally
  requires the dependency to exist at all.
- **Applied a mechanical, high-value fix across 56 content/ files**: removed all
  `.get()` calls on `CCBlocks`/`CCItems`/`CCBlockEntityTypes`/`CCMountedStorageTypes`/
  `CCDisplaySources`/`CCDisplayTargets` references (these were Registrate `RegistryObject`-style
  accessors in the original code; the session-2 registries rewrite made all of these plain
  direct-typed static fields, so `.get()` is now simply invalid - this was noted as pending back
  in session 2 and finally executed here as a single bulk sed across the whole tree).
- State after this session: `./gradlew compileJava` down to **22 unique erroring files** (all
  named list captured in this note's session-4 entry's compile run is now stale - rerun before
  trusting exact file names). Remaining known problem clusters, not yet fixed: `BrassChuteBlock`/
  `BrassGearboxBlockEntity`/`FluidVessel*`/`CrankWheel*`/`Dashboard*`/copycat family/
  `InventoryAccessPortBlock` - each needs the same kind of individual investigation as this
  session's 5 files (some may be simple import fixes, some may need the Server/Client behaviour
  split, `FluidVesselBlockEntity` specifically needs the `IHaveGoggleInformation` ->
  client-only-`TooltipBehaviour`-class research flagged at the top of this session but not
  completed - see `client.foundation.blockEntity.behaviour.tooltip.*TooltipBehaviour` family in
  the real jar as the likely real pattern, structurally probably another Server+Client split but
  not yet verified).
- **Not started this session** (unchanged from before): the ~117 (now ~112) other content/ files
  needing real fixes beyond the reverted 12, the ~15 capability block entities -> Fabric Transfer
  API, the 274-recipe `neoforge:conditions` replacement, full `./gradlew build`.

## PROGRESS session 6: CRITICAL CORRECTION - the "lazy verification" pattern was wrong
**Read this before touching any file previously marked "safe because guarded by isClientSide()"
or similar.** Session 4/5 established a pattern: when a method inside an otherwise-shared
(main-sourceset) class needs a client-only Create Fly/vanilla type, but that method is only ever
*invoked* from a client-side code path (e.g. behind `isClientSide()`, or an `@OnlyIn(Dist.CLIENT)`
override), it was assumed safe to leave the client-only **imports** in the main-sourceset file, on
the theory that "the JVM only resolves a method's bytecode when it's actually called, so an
unreached method's imports don't matter on a dedicated server." **This is wrong for this
codebase.** Loom's `splitEnvironmentSourceSets()` filters the *compile-time* classpath per source
set - `main` cannot resolve `com.zurrtum.create.client.*` or `net.minecraft.client.*` symbols **at
all**, and javac rejects the unresolvable import as a compile error regardless of whether the
method containing it is ever called at runtime. This has nothing to do with JVM lazy verification
(which is a real thing, but doesn't apply across Loom's split source sets) - it's a hard
compile-time wall.

### How this was found
Every status report through session 5 was reading `./gradlew compileJava`'s **error count through
javac's default `-Xmaxerrs` cap (100, doubled to ~200 somewhere in the Loom/Gradle chain)** and
treating "200 errors, capped" as roughly representative of total remaining scope. It wasn't -
**added `it.options.compilerArgs << "-Xmaxerrs" << "5000"` to `build.gradle`'s `JavaCompile`
block (permanent, keep this) and re-ran: the real number was 2564 errors across 132 unique
main-sourceset files**, not ~16-22. The capped view also hid that some of my own "guarded, safe"
fixes this session (`KineticBridgeBlockItem.showBounds()`, `OverstressClutchBlockEntity
.addToTooltip()`, `CrankWheelBlockEntity.getRenderedHandle()`/`tickAudio()`) were themselves
broken by this exact mistake and had never actually been verified compiling - they just hadn't
been reached yet within the 200-error window. **Always use the uncapped compile output (now the
default given the build.gradle change) before trusting a "how many files are left" count.**

### The correct pattern (verified, now applied to 2 files as a template)
When a method needs client-only types but must live in a class that's genuinely shared (Block,
Item, BlockEntity - anything Minecraft's registries need on both logical sides), extract the
method's *body* into a client-sourceset companion class, and connect it via a public static
hook field (function/consumer type matching the method's shared-safe signature) that defaults to
a no-op and gets populated from `CreateConnectedClient.onInitializeClient()`. Concretely fixed
this session as the reference examples:
- `content/kineticbridge/KineticBridgeBlockItem.java` (main): `showBounds()` body extracted to
  new `src/client/java/.../kineticbridge/KineticBridgeBlockItemClient.java`, wired via
  `public static Consumer<BlockPlaceContext> showBoundsHook`.
- `content/overstressclutch/OverstressClutchBlockEntity.java` (main): the uncoupled-tooltip
  translation-building body extracted to new
  `src/client/java/.../overstressclutch/OverstressClutchBlockEntityClient.java`, wired via
  `public static BiConsumer<OverstressClutchBlockEntity, List<Component>> uncoupledTooltipHook`.
- `content/crankwheel/CrankWheelBlockEntity.java` (main): **different resolution** - investigated
  the real Create Fly architecture instead of hooking around the mistake, and found
  `HandCrankBlockEntity` (the base class) has **no `getRenderedHandle()`/`tickAudio()` methods at
  all anymore** - Create Fly moved the "handle" render buffer computation entirely into
  `com.zurrtum.create.client.content.kinetics.crank.HandCrankRenderer.getRenderedHandle
  (BlockState)` (a *renderer* method, not a block-entity method - verified by reading the real
  class), and kinetic audio into a client-only `KineticAudioBehaviour` registered through
  `BlockEntityBehaviour.CLIENT_REGISTRY` (see session 5's `AllBlockEntityBehaviours` discovery).
  Dropped both overrides from the block entity entirely (they were overriding methods that don't
  exist in the new base class - dead code, not real overrides). **Not yet done**: a
  `CrankWheelRenderer extends HandCrankRenderer` client class overriding `getRenderedHandle
  (BlockState)` with the large/small cog check (documented, straightforward, just not written
  yet), and the cranking sound effect via `CLIENT_REGISTRY` (bigger side-quest, deferred).

### Known files still needing this same treatment (found via cross-reference audit, NOT yet fixed)
Cross-checked every `com.hlysine.create_connected.*` import used from `main`-sourceset files
against what's actually sitting in `src/client/java` (the same technique used for the mixin/
import audit in session 3, extended here). Beyond the 3 above, confirmed genuinely-client-only
(not wrongly-moved) and therefore needing the hook-extraction treatment, **not yet done**:
- `content/contraption/jukebox/PlayContraptionJukeboxPacket.java` (packet class - referenced from
  `registries/CCPackets.java` in main for registration, but its *handler* logic uses
  `Minecraft.getInstance()`/`ClientLevel`/`LocalPlayer`). Packets need a server-safe encode/decode
  shape usable from main plus a client-only handler - likely needs restructuring similar to how
  `SyncConfigBase`'s `CustomPacketPayload` was done in session 1, not a simple hook.
- `content/kineticbattery/KineticBatteryDisplaySource.java` (referenced from
  `registries/CCDisplaySources.java` in main) - uses `ModularGuiLineBuilder`/`CreateLang`/
  `LangBuilder` (all client-only) for its display-text formatting.
- `content/redstonelinkwildcard/LinkWildcardNetworkHandler.java` (referenced from
  `mixin/redstonelinkwildcard/RedstoneLinkNetworkHandlerMixin.java` in main) - uses client-only
  `LinkBehaviour`.
- `content/sequencedpulsegenerator/instructions/Instruction.java` (base class for 12 instruction
  subclasses in main) - uses client-only `ScrollValueBehaviour`/`I18n`.
- **There are almost certainly more beyond these 4** - this audit only covered files with a
  `com.hlysine.create_connected.*` import from a currently-erroring main file; the true remaining
  count (2564 errors across 132 files) means most errors are still ordinary unconverted-import
  issues (same categories as sessions 4-5: relocated Create Fly classes, NeoForge leftovers,
  Registrate leftovers, capability imports), not all instances of this specific mistake - but
  **any file with real "package does not exist" errors for a `client.*` symbol needs this
  audit step first** (is the referencing main file's usage genuinely guard-safe in principle, i.e.
  would extraction-to-hook preserve correctness, vs. does the whole file actually belong on one
  side) before applying either fix.

### Also fixed this session (real, unrelated-to-the-above fixes)
- `MC 1.21.11 API drift`: `net.minecraft.world.ItemInteractionResult` was consolidated into
  `InteractionResult` (confirmed via `CreateModAddon`'s real working code) - bulk-replaced across
  all 15 affected files, deduped resulting double-imports.
  `net.minecraft.world.level.block.state.properties.DirectionProperty` similarly doesn't exist
  standalone anymore - `BlockStateProperties.HORIZONTAL_FACING`'s real type is
  `EnumProperty<Direction>` (verified against Create Fly's own `AllBlocks.java` usage pattern).
- Removed dead JSR-305 nullability annotations (`@Nonnull`, `@ParametersAreNonnullByDefault`,
  `@MethodsReturnNonnullByDefault` and their imports) across 14 files - zero runtime effect,
  `javax.annotation.*` isn't reliably on the Fabric classpath.
- `net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH` (NeoForge convention tag) ->
  `CCTags.commonItemTag("tools/wrench")` (this mod's own established Fabric convention-tag
  helper from session 1).
- `NeoForge's CommonHooks.onNoteChange` (lets other mods veto/modify note-block pitch changes) has
  no Fabric equivalent - simplified `NoteBlockInteractionBehaviour` to the direct vanilla cycle
  behavior. Real, disclosed feature loss: other-mod interception of this one interaction, not
  this mod's own note-cycling.
- Deleted 7 more pure-Registrate-datagen files with zero runtath purpose given static assets
  (`datagen/CCBlockStateGen.java`, `CCDataMapGen.java`, `CCDatagen.java`, `CCTagGen.java`, all of
  `datagen/recipes/*` (10 files), `content/fluidvessel/FluidVesselGenerator.java`,
  `content/inventoryaccessport/InventoryAccessPortGenerator.java`,
  `content/kineticbattery/KineticBatteryGenerator.java`) - moved to
  `_disabled_pure_datagen/` outside `src/`, **kept `datagen/advancements/*`** (real runtime
  advancement-trigger registration, not pure datagen despite the package name - already flagged
  in session 2 as needing entrypoint wiring, still pending).
- `KineticBatteryOverrides.java`: kept `registerModelOverridesClient` (real runtime item-model
  predicate registration), dropped `addOverrideModels` (pure Registrate item-model datagen -
  verified the corresponding static JSON already exists under
  `src/generated/resources/assets/create_connected/models/item/kinetic_battery_level_*.json`).
- `FanCatalystRotatingHeadBlock`/`LinkedAnalogLeverBlock`/`CCAdvancement`: last few
  `com.tterrag.registrate.*` type leftovers (`BlockEntityEntry`, `NonNullSupplier`,
  `ItemProviderEntry`) replaced with plain `BlockEntityType`/`Supplier`/dropped-in-favor-of-the-
  already-existing-`ItemLike`-overload, respectively.
- `registries/CCBlocks.java` **regressions from this session's earlier (mistaken) file moves,
  now fixed**: dead imports for 2 files that got moved to client (`ItemSiloCTBehaviour`,
  unused `RenderType`), and `EncasedCrossConnectorBlock`/`LinkedAnalogLeverBlock` constructors
  that now take `Supplier<Block>`-shaped parameters needed `() -> AllBlocks.X` lambda-wrapping
  instead of passing the direct instance (this was *always* wrong, not a regression from this
  session - just newly surfaced by finally getting an uncapped compile).

### Revised honest status
`./gradlew compileJava` (main sourceset only): **2564 errors, 132 unique files**, not the ~16-22
previously reported (that was reading a capped, misleading number). `compileClientJava` has not
been re-checked with the uncapped setting yet this session - expect a similarly larger real count
there too. This is a significantly larger remaining body of work than prior session reports
implied. The registration core (`registries/`, `config/`, `compat/`, `mixin/`) fixes made in this
and prior sessions are still real and still the right foundation, but `content/`'s true remaining
scope is much bigger than the capped counts suggested, and an unknown-but-likely-substantial
fraction of it will hit the same "client-only-in-a-shared-class" issue described above, requiring
the hook-extraction (or architecture-investigation, per the CrankWheel example) treatment file by
file, not just import-path fixes.

## PROGRESS session 7: networking rewrite, the 4 flagged files, CrankWheelRenderer, and a major new API-drift discovery (ValueInput/ValueOutput)

### Networking: real vanilla replacement for Create Fly's now-entirely-gone packet base classes
Confirmed via search that `com.zurrtum.create.catnip.net.base.*` (BasePacketPayload,
CatnipPacketRegistry, ClientboundPacketPayload) and
`com.zurrtum.create.foundation.networking.BlockEntityConfigurationPacket` are **not just
relocated - they don't exist anywhere in the real 6.0.9-5 sources jar**. Replaced with plain
vanilla `net.minecraft.network.protocol.common.custom.CustomPacketPayload` +
`net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry` (`.playS2C()`/`.playC2S()`) +
`ServerPlayNetworking`/`net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking`.
- `registries/CCPackets.java`: now just calls `PayloadTypeRegistry.playS2C()/.playC2S().register(...)`
  and `ServerPlayNetworking.registerGlobalReceiver(...)` for the one C2S packet.
- `content/contraption/jukebox/PlayContraptionJukeboxPacket.java` (S2C): moved to main, rewritten
  as a `record ... implements CustomPacketPayload`, kept its own 7-arg `composite()` StreamCodec
  helper (vanilla only ships up to 6-arg). `handle()` moved out entirely to a new client-only
  `PlayContraptionJukeboxPacketClient.java` registered via `ClientPlayNetworking`.
  `JukeboxInteractionBehaviour.java`'s send-side: NeoForge's
  `PacketDistributor.sendToPlayersInDimension` -> loop `PlayerLookup.world((ServerLevel) level)`
  + `ServerPlayNetworking.send(player, payload)`.
- `content/sequencedpulsegenerator/ConfigureSequencedPulseGeneratorPacket.java` (C2S): rewritten
  as a plain record with `handle(ServerPlayer)` inlining the distance-check/block-entity-lookup
  that `BlockEntityConfigurationPacket` used to provide (MAX_RANGE=16, same as original).

### `Instruction.java` (sequenced pulse generator instructions) - moved to main, client deps stripped
Had to live in main (real tick()/NBT gameplay state) but its public API leaked several
client-only types. Fixed by: `background` field changed from `CCGuiTextures` (client enum) to
opaque `Object` (string-literal based; the client `SequencedPulseGeneratorScreen` casts back via
`CCGuiTextures.valueOf((String) instruction.getBackground())`); lang-key helpers use plain
`Component.translatable(...)` instead of `ConnectedLang`; added
`public static Function<String, Boolean> i18nExistsHook` (populated from
`CreateConnectedClient.onInitializeClient()` with `I18n::exists`) replacing a direct
`I18n.exists()` call; and a new mod-owned `Instruction.StepContext` record replaces the
client-only `ScrollValueBehaviour.StepContext` it used to reference. 12 subclass files bulk-fixed
via sed to drop the `CCGuiTextures` import and use string literals instead.

### `KineticBatteryDisplaySource.java` - split following Create Fly's *real* DisplaySourceRender pattern
Verified (by decompiling the real sources jar) that Create Fly itself moved `initConfigurationWidgets`
entirely OUT of `DisplaySource` (the main-safe base class) into a client-only
`com.zurrtum.create.client.api.behaviour.display.DisplaySourceRender` interface, attached per-instance
via a public `DisplaySource.attachRender` field (see real `KineticStressDisplaySource` +
`KineticStressDisplaySourceRender` + `AllDisplaySourceRenders.register(source, factory)` for the
reference). Followed the same real pattern here instead of inventing our own: `KineticBatteryDisplaySource`
(main) keeps only the gameplay text logic (and had its `ConnectedLang`-based `formatNumeric` body
rewritten to plain `Component`/`java.text.NumberFormat`, matching how real
`KineticStressDisplaySource.formatNumeric` itself avoids `CreateLang`); a new client-only
`KineticBatteryDisplaySourceRender extends SingleLineDisplaySourceRender` holds the
`ModularGuiLineBuilder`/`ConnectedLang`-based UI method; wired via
`CCDisplaySources.KINETIC_BATTERY.attachRender = new KineticBatteryDisplaySourceRender();` in
`CreateConnectedClient.onInitializeClient()` (no need for a full `AllDisplaySourceRenders`-style
helper class just for one custom source).

### `LinkWildcardNetworkHandler.java` - moved to main, two real fixes
- `com.zurrtum.create.client.content.redstone.link.LinkBehaviour` (client-only) -> real Create Fly's
  own `RedstoneLinkNetworkHandler.updateNetworkOf` uses `com.zurrtum.create.content.redstone.link.
  ServerLinkBehaviour` (main-safe, same `newPosition`/`isListening`/`setReceivedStrength` shape) for
  this exact same `instanceof` check - verified in the real sources jar, so swapping to
  `ServerLinkBehaviour` matches Create Fly's own pattern, not a workaround.
- `net.neoforged...@EventBusSubscriber`/`@SubscribeEvent`/`LevelEvent.Load`/`Unload` (NeoForge,
  gone) -> Fabric API's `net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD`/`UNLOAD`,
  registered from a new `LinkWildcardNetworkHandler.register()` called in
  `CreateConnected.onInitialize()`. `fabric-lifecycle-events-v1` comes transitively via the
  `fabric-api` umbrella dependency already declared, no new dependency needed.
- **Real feature loss, disclosed, not silently dropped**: the file also had a `withinRange` special
  case for compatibility with the "Sable" mod (`dev.ryanhcode.sable`, sub-level/moving-structure
  physics) via `SableCompanion`/`SubLevelAccess` - **but there was no `sable` dependency declared
  anywhere in this project's build.gradle at all**, and WebSearch confirmed Sable's newest release
  only targets MC 1.21.1, with no 1.21.11 build to depend on. Real compat is currently impossible
  to reinstate (not merely skipped), so `withinRange` now falls back to the plain distance check -
  which is exactly what the original code already did whenever Sable's helper returned no sub-level
  for a position anyway. Revisit if/when Sable ships a 1.21.11 build.

### `CrankWheelRenderer` (client) - written, following the real `HandCrankRenderer` shape
Fetched the original NeoForge `CrankWheelBlockEntity.getRenderedHandle()`/`tickAudio()` bodies from
the upstream repo (github.com/hlysine/create_connected) since they'd already been deleted from
`CrankWheelBlockEntity.java` in a prior session. Wrote
`src/client/java/.../crankwheel/CrankWheelRenderer.java extends HandCrankRenderer`, overriding the
one-arg `getRenderedHandle(BlockState)` instance method (that's where Create Fly moved it - it's a
renderer method now, not a block-entity method) with the same large/small-cog handle-model choice
as the original. `tickAudio()`'s cranking sound (`AllSoundEvents.CRANKING`) has **no replacement
written yet** - Create Fly moved kinetic audio to a client-only `KineticAudioBehaviour` registered
via `BlockEntityBehaviour.CLIENT_REGISTRY`, which this mod hasn't wired up anywhere yet (bigger
side-quest, still deferred, not silently dropped - the sound just doesn't play yet).
Registered via a new minimal `registries/CCBlockEntityRenders.java` (client-only) calling
`BlockEntityRendererRegistry.register(CCBlockEntityTypes.CRANK_WHEEL, CrankWheelRenderer::new)` +
`SimpleBlockEntityVisualizer.builder(...).factory(CrankWheelVisual::new).skipVanillaRender(be ->
false).apply()` (`skipVanillaRender=false` mirrors real Create Fly's own `AllBlockEntityRenders.
normal(...)` helper - keep the vanilla-renderer fallback alive for when Flywheel visualization
isn't supported; `CrankWheelVisual`, the Flywheel-backed normal path, already existed from an
earlier session and needed no changes). Also had to move `registries/CCPartialModels.java` from
main to `src/client/java` - it was in main but imports `com.zurrtum.create.client.flywheel.lib.
model.baked.PartialModel` (client-only baked-model type), a cross-sourceset violation that was
silently broken (only client-side files ever consumed it anyway).

### MAJOR FINDING: MC 1.21.11 replaced CompoundTag single-arg getters with Optional, AND replaced BlockEntity's write/read(CompoundTag,...) with a Codec-based ValueInput/ValueOutput view entirely
While fixing the above, `./gradlew compileJava` surfaced that `CompoundTag.getInt(String)` /
`getString(String)` / `getBoolean(String)` / `getFloat(String)` / `getLong(String)` /
`getDouble(String)` / `getByte(String)` / `getCompound(String)` / `getList(String, int)` (the old
"default value built in, or throws/returns 0/empty" 1-or-2-arg forms this whole codebase was
written against) **no longer exist in that shape** - verified via `javap` against the actual
resolved `minecraft-common` mapped jar (`~/.gradle/caches/fabric-loom/minecraftMaven/...`, NOT a
guess). The single-arg getters now return `Optional<T>`; the old "give me 0/""/false/empty if
missing" behavior is now a **separate**, differently-named method: `getIntOr(key, default)`,
`getStringOr(key, default)`, `getBooleanOr(key, default)`, `getFloatOr`, `getLongOr`, `getDoubleOr`,
`getByteOr`, `getCompoundOrEmpty(key)`, `getListOrEmpty(key)` (this last one also dropped the
tag-type-ID second argument entirely). Bulk-fixed via a `perl -pi -e` sweep across all 20 then-
affected files (`.getInt("X")` -> `.getIntOr("X", 0)`, etc.) - safe and behavior-preserving because
vanilla's old single-arg getters already defaulted to exactly those same values (0/""/false) when
the key was absent, so `getXOr(key, oldDefault)` is an exact semantic match, not a workaround.

**Bigger and separate finding, only partly addressed**: `SmartBlockEntity`'s abstract
`write`/`read` methods changed signature from `(CompoundTag, HolderLookup.Provider, boolean)` to
`(ValueOutput, boolean)` / `(ValueInput, boolean)` respectively (verified via the real sources
jar - `com.zurrtum.create.foundation.blockEntity.SmartBlockEntity.write/read`). `ValueOutput`/
`ValueInput` (`net.minecraft.world.level.storage.ValueOutput`/`ValueInput`) are a new Codec-based
view API - `putInt`/`putString`/etc. work basically the same, but reading uses the same
`getXOr`/`getX->Optional` split as `CompoundTag`, and there's no raw "put a whole ListTag" - instead
use `view.list(key, someCodec)` (returns a `TypedOutputList<T>` with `.add(T)`) on write and
`view.listOrEmpty(key, someCodec)` (returns `Iterable<T>`) on read; `CompoundTag.CODEC` exists and
is the bridge for "I still want to store one CompoundTag per list entry" (used for `Instructions` -
see `SequencedPulseGeneratorBlockEntity.write/read` for the worked example). Also discovered in the
same pass: `addBehaviours(List<BlockEntityBehaviour>)` must be
`addBehaviours(List<BlockEntityBehaviour<?>>)` (raw type -> now genuinely generic in Create Fly),
and `Level.isClientSide` is now a **private field** - must call `level.isClientSide()` (method).

**Only `SequencedPulseGeneratorBlockEntity.java` (+ its `AdvancementBehaviour.registerAwardables`
call site's parameter type) has been converted to this new shape so far.** This is very likely a
**mod-wide** change affecting most/all block entities that override `write`/`read` - a full audit
(`grep -rn "protected void write(CompoundTag\|protected void read(CompoundTag" src/main`) has NOT
been done yet. Treat this as a high-priority, probably-large item for the next session: any
BlockEntity conversion work in `content/` should check for this pattern and fix it the same way,
not just chase the `getInt`/`getString`/etc. symptom in isolation.

### Revised honest status (end of session 7)
`./gradlew compileJava`: down to **2000 errors across 113 unique files** (from 2564/132 at the
start of this session) after: the 4 originally-flagged files (`PlayContraptionJukeboxPacket`,
`KineticBatteryDisplaySource`, `LinkWildcardNetworkHandler`, `Instruction` + its 12 subclasses),
`SequencedPulseGeneratorBlockEntity`'s ValueInput/ValueOutput conversion, the mod-wide
`getInt`/`getString`/`getBoolean`/etc. -> `getXOr`/`getXxxOrEmpty` sweep (20 files), and
`CrankWheelRenderer` + its registration wiring. `compileClientJava` also re-checked with the
uncapped view: **2000 errors across 113 files** too (coincidentally close to the main count, but a
different file set - mostly the same underlying causes reflected from both sides of files that
live in `src/main` but get compiled against the client classpath's restrictions, e.g. `ponder/`'s
entirely-unconverted `com.zurrtum.create.ponder.api.*`/`foundation.ponder.*` imports, and
`CrankWheelBlock`/`CrankWheelItem`'s own separate `updateShape`/`rotate`/`placeInWorld` signature
mismatches - not investigated this session, out of scope for the CrankWheel task at hand). None of
this session's new/moved files (`CrankWheelRenderer`, `CCBlockEntityRenders`, `CCPartialModels`,
`CrankWheelVisual`, all four originally-flagged files) appear in either error list - verified
clean by grepping the uncapped compile output for their filenames specifically, not just trusting
a smaller overall count.

## PROGRESS session 8: full ValueInput/ValueOutput audit (the mod-wide finding from session 7)

Ran `grep -rln "protected void write(CompoundTag\|protected void read(CompoundTag" src/main` per the
coordinator's explicit instruction to close out session 7's flagged finding. Got 9 hits:
`DashboardBlockEntity`, `FluidVesselBlockEntity`, `InventoryAccessPortBlockEntity`,
`InventoryBridgeBlockEntity`, `ItemSiloBlockEntity`, `KineticBatteryBlockEntity`,
`KineticBridgeDestinationBlockEntity`, `LinkedTransmitterBlockEntity`,
`OverstressClutchBlockEntity`. Converted all 9 to `(ValueOutput/ValueInput view, boolean
clientPacket)`, plus fixed the same-session-discovered `addBehaviours(List<BlockEntityBehaviour>)`
-> `List<BlockEntityBehaviour<?>>` and `level.isClientSide` -> `level.isClientSide()` in each where
present.

**Two more hits the grep pattern missed, found only because the compiler still flagged them as
having no matching supertype method** (grep only matched `protected/public void write/read
(CompoundTag ...)` exactly - these two had a different old shape):
- `content/fluidvessel/BoilerData.java`: its old shape was `public CompoundTag write()` (no args,
  returns the tag) / `public void read(CompoundTag nbt, int boilerSize)` - not the
  `write(CompoundTag, HolderLookup.Provider, boolean)` shape the grep looked for. Real Create Fly's
  own `com.zurrtum.create.content.fluids.tank.BoilerData` (the class ours extends) now has
  `write(ValueOutput view)` / `read(ValueInput view, int boilerSize)` - converted to match exactly
  (both had stray `@Override` annotations already failing to compile, confirming this was a real,
  not cosmetic, bug). `FluidVesselBlockEntity`'s call sites updated to
  `boiler.write(view.child("Boiler"))` / `boiler.read(view.childOrEmpty("Boiler"), ...)` - this
  `child`/`childOrEmpty` nested-view pattern is exactly what real Create Fly's own
  `FluidTankBlockEntity.write/read` does for its own `boiler` field (verified in the sources jar),
  not an invented shape.
- `datagen/advancements/AdvancementBehaviour.java`: extends `BlockEntityBehaviour<T>` (not
  `SmartBlockEntity` directly) - `BlockEntityBehaviour.write/read` changed the same way
  (`ValueOutput/ValueInput`, no `HolderLookup.Provider` param - use `view.lookup()` if needed).
  Also fixed the raw-type `extends BlockEntityBehaviour` -> `extends
  BlockEntityBehaviour<SmartBlockEntity>` (was silently raw before). One extra real finding:
  `CompoundTag.putUUID`/`getUUID` **no longer exist at all** in 1.21.11 (verified via `javap`) -
  the replacement is `net.minecraft.core.UUIDUtil.CODEC` via `view.store(key, UUIDUtil.CODEC,
  uuid)` / `view.read(key, UUIDUtil.CODEC)`.

**Lesson for any future occurrence of this pattern**: don't trust a single grep shape to find every
instance - the safest signal is actually just compiling and looking for "method does not override
or implement a method from a supertype" / "cannot find symbol" pointing at a real supertype method,
since subclasses can have drifted to non-matching overload shapes (extra/missing params, different
return type) that a narrow grep won't catch.

**Files where the write/read conversion alone doesn't make the file compile clean** (expected,
explicitly out of scope for this pass - tracked as separate, already-known later priorities):
`FluidVesselBlockEntity`/`FluidVesselBlock`/`FluidVesselMountedStorage`/`FluidVesselItem` (NeoForge
`Capabilities`/`IFluidHandler`/`FluidStack`/`FluidType` + the now-nonexistent `SmartFluidTank` +
the client-only `IHaveGoggleInformation` interface implemented from main - all separate,
already-tracked items), `ItemSiloBlockEntity` (NeoForge `ItemStackHandler`/`IItemHandler`/
`IItemHandlerModifiable`/`CombinedInvWrapper` + `Capabilities`), `InventoryAccessPortBlockEntity`/
`InventoryBridgeBlockEntity` (NeoForge `Capabilities`/`IItemHandler`), `KineticBatteryBlockEntity`
(client-only `ScrollOptionBehaviour`/`ConnectedLang`/`CreateLang` used directly from main - a
Server/Client behaviour-split issue like the ones fixed in earlier sessions, not yet done for this
class). For these, the write/read bodies were bridged with `view.read/store(key, CompoundTag.CODEC,
...)` (or `BlockPos.CODEC` for positions, plain `putString`/`getStringOr` for one enum instead of
`NBTHelper.readEnum`/`writeEnum` which stayed CompoundTag-based and unaffected) so the NBT shape is
already correct and ready to go the moment the capability/behaviour-split work lands - verified via
compiling that no *new* write/read-shaped errors exist in any of these files anymore, only the
pre-existing, separately-tracked ones.

### Status after this pass
`./gradlew compileJava` and `compileClientJava` (uncapped): **1850 errors across 111 unique files**
(down from 2000/113 at the start of this session). `grep -rln "protected void write(CompoundTag\|
protected void read(CompoundTag" src/main` now returns **zero** hits - the mod-wide audit is
complete for that exact shape; the two additional non-matching-shape cases found by compiling
(`BoilerData`, `AdvancementBehaviour`) are also fixed.

## PROGRESS session 9: CatnipServices/SmartFluidTank/BakedQuadHelper replacements, and an important error-log reliability finding

### IMPORTANT CORRECTION: every error count reported in sessions 6-8 was 2x the real number
Discovered this session that Gradle prints each javac diagnostic **twice** in `compileJava`/
`compileClientJava` console output (once as the primary message, once again re-indented by 2
spaces in a trailing recap section) - so `grep -c "error:"` was silently counting every error
twice all along. The unique-file counts (via `grep -oE "F:[^:]+\.java" | sort -u | wc -l`) were
NOT affected (already deduplicated) and remain trustworthy. **Take every past "N errors" figure
in this document and divide by 2** for the true count (e.g. session 8's "1850 errors/111 files"
was really ~925 unique errors/111 files). This doesn't change any relative progress trend, just
the absolute scale - flagging it so nobody re-derives a wrong number from a stale absolute count.

### `CatnipServices.PLATFORM.executeOnClientOnly(...)` - doesn't exist in Create Fly at all, and wouldn't have helped anyway
Two call sites (`KineticBatteryBlockItem.registerModelOverrides()`,
`SequencedPulseGeneratorBlock.useItemOn/useWithoutItem`) used this NeoForge/Fabric platform-
abstraction guard to "safely" call client-only code from shared code. Confirmed via the sources
jar that `com.zurrtum.create.catnip.platform.CatnipServices` doesn't exist in Create Fly 6.0.9-5 -
single-platform Fabric mods don't need it. Even if it did exist, per the session 6 correction
(Loom's split source sets are a **compile-time** restriction, not runtime), a runtime-only guard
could never have made a client-only reference legal in a main-sourceset file anyway. Fixed both
via the established hook-extraction pattern:
- `KineticBatteryBlockItem.registerModelOverrides()` deleted entirely; `CCBlocks.java` now exposes
  `KINETIC_BATTERY_ITEM` as a public static field (previously a local var inside a static block)
  so `CreateConnectedClient.onInitializeClient()` can call
  `KineticBatteryOverrides.registerModelOverridesClient(CCBlocks.KINETIC_BATTERY_ITEM)` directly.
  Also found and fixed `KineticBatteryBlockItem.appendHoverText` using `ConnectedLang` (client-only)
  directly from this main-sourceset `BlockItem` - rewrote using plain
  `Component.translatable(MODID + ".key")` + `java.text.NumberFormat`, same pattern as session 7's
  `KineticBatteryDisplaySource` fix. Note `barComponent()`/`bars()` in `KineticBatteryBlockEntity`
  were already plain-`Component`-based (main-safe), so only the `ConnectedLang` calls needed fixing.
- `SequencedPulseGeneratorBlock`'s `displayScreen(be, player)` (opens a client `Screen`, marked with
  the now-meaningless `@OnlyIn(Dist.CLIENT)`) extracted to new
  `SequencedPulseGeneratorBlockClient.displayScreen(...)` (client), connected via a new
  `public static BiConsumer<SequencedPulseGeneratorBlockEntity, Player> displayScreenHook`
  populated in `CreateConnectedClient.onInitializeClient()`.

### `SmartFluidTank` - Create Fly dropped it; real replacement is `foundation.fluid.FluidTank` + per-use-case `markDirty()` override
`com.zurrtum.create.foundation.fluid.SmartFluidTank` (the old "FluidTank with an update callback
constructor" convenience class) doesn't exist in Create Fly 6.0.9-5 at all. Its real replacement,
`com.zurrtum.create.foundation.fluid.FluidTank`, only takes a plain `(int capacity)` constructor -
no callback parameter. Verified via the real `CreativeFluidTankBlockEntity.CreativeFluidTankInventory`
(a nested class in Create Fly's own equivalent block entity) that the actual modern pattern is to
subclass `FluidTank` and override `markDirty()` to invoke the callback, not pass a callback into
the constructor. Added two new mod-owned classes matching that real pattern:
- `content/fluidvessel/FluidVesselTank extends FluidTank` - `markDirty()` invokes a
  `Consumer<FluidStack>` passed into its constructor (this mod's non-creative tank).
- `content/fluidvessel/CreativeFluidVesselTank extends FluidVesselTank` - mirrors the real
  `CreativeFluidTankInventory`'s always-full/infinite insert-extract overrides (copied the exact
  same method bodies, just renamed to fit this mod's class hierarchy - the original nested class
  isn't accessible from outside `CreativeFluidTankBlockEntity`, so a distinct copy was necessary,
  not reusable).
`FluidVesselBlockEntity`/`CreativeFluidVesselBlockEntity`'s `createInventory()` now return these
instead of the non-existent `SmartFluidTank`/`CreativeFluidTankBlockEntity.CreativeSmartFluidTank`.
**Deliberately did NOT touch** `FluidVesselBlock.java`'s `IFluidHandler vesselCapability`-typed code
(NeoForge `Capabilities`/`IFluidHandler`/`FluidStack` throughout, including
`CreativeFluidTankBlockEntity.CreativeSmartFluidTank` instanceof-checks in the fluid-exchange
interaction logic) - that's inseparable from the full NeoForge-Capabilities-to-Fabric-Transfer-API
rewrite (the coordinator's separate, later "~15 capability block entities" priority item), and
partially converting just the tank field type without doing that whole rewrite would only have
been possible by leaving the surrounding capability code just as broken as it already was (verified
no new errors introduced, no errors fixed there either - correctly deferred, not silently dropped).

### `BakedQuadHelper` - moved/renamed to client-only `BakedModelHelper`, with a simplified signature (BakedQuad is now a record)
`com.zurrtum.create.foundation.model.BakedQuadHelper` doesn't exist; the real modern equivalent is
`com.zurrtum.create.client.foundation.model.BakedModelHelper` (client-only - baked model quad
manipulation is inherently client-side, makes sense it moved to `client.*`). Its `cropAndMove`
method's signature is also **simpler** than the old NeoForge-era shape our 5 affected files
(`CopycatBeamModel`, `CopycatBlockModel`, `ISimpleCopycatModel`, `CopycatSlabModel`,
`CopycatVerticalStepModel` - all client-only) were already calling it with
(`cropAndMove(quad.getVertices(), quad.getSprite(), aabb, vec3)` then wrapped in
`BakedQuadHelper.cloneWithCustomGeometry(quad, ...)`): the real signature is simply
`cropAndMove(BakedQuad quad, AABB crop, Vec3 move) -> BakedQuad`, doing the crop+move+clone in one
call - no separate `cloneWithCustomGeometry` wrapping step needed at all. (`BakedQuad` itself is
now a Java record in this MC version - verified via `BakedModelHelper`'s own real source using
`.comp_XXXX()` record-accessor names - which is also presumably why the old mutable-array-based
"clone for safety" pattern isn't needed anymore.) Fixed all 5 files: replaced
`BakedQuadHelper.cloneWithCustomGeometry(quad, BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), aabb, vec3))`
with the single call `BakedModelHelper.cropAndMove(quad, aabb, vec3)` everywhere, and
`CopycatBlockModel`'s plain `BakedQuadHelper.clone(quad)` (no cropping, just a defensive copy) with
directly reusing the same quad instances (`new ArrayList<>(templateQuads)`) since immutable records
have no aliasing-mutation risk to guard against anymore. **Note**: these 5 files were apparently
already half-converted by an earlier, unverified session (already importing/calling
`BakedModelHelper.cropAndMove` under the *old* multi-arg signature) - a reminder that "imports the
right class" doesn't mean "compiles" without actually checking the call signature too.

### UNRESOLVED ENVIRONMENT QUIRK - flagging for future sessions, don't re-trust blindly
While fixing the 5 `BakedQuadHelper`/copycat model files above, discovered that **none of them ever
appear in `compileClientJava`'s error output, regardless of their actual content** - verified this
by deliberately appending unambiguous garbage (`BADSYNTAX!!!` outside any class body, which must be
a syntax error in any Java file) directly into `CopycatBlockModel.java` and recompiling with
`--rerun --no-build-cache`: the error output was **byte-for-byte identical** before and after,
with zero mentions of that file. This is not explained by anything found this session (not
Gradle's build cache, not `--rerun`, not `clean`) - the file is a normal `.java` file in
`src/client/java` matching the same directory convention as every other file that DOES show up in
errors. **Practical implication: don't fully trust "this file has zero errors in the compile log"
for files under `content/copycat/` (and possibly elsewhere) as proof the file actually compiles -
verify those specific files by another means (e.g. a real IDE, or invoking `javac` directly outside
Gradle) before relying on their apparent clean status.** This doesn't invalidate the overall error/
file-count trend (which is dominated by the hundreds of files that DO reliably show up), just this
one specific pocket of files. Worth a future session's time to actually root-cause if it recurs
elsewhere, since it undermines the "verify by compiling" methodology this whole project depends on.

### Status after this session
`./gradlew compileJava`: **896 unique errors across 110 files** (down from 925/111 - see the 2x
counting correction above; genuine file-count progress: 111->110, and several individual symbol
errors resolved within files that still have other, separately-tracked issues, so the true
error-count delta is larger than the file-count delta suggests). `compileClientJava` reported
identically (896/110) but per the quirk above, treat the copycat-model-file portion of that number
with caution.

## Session 9 continued: root-caused the compileClientJava anomaly - REAL ROOT CAUSE FOUND

### Root cause: `compileClientJava` has never actually run once, this entire port
`compileClientJava` depends on `compileJava` succeeding (it needs main's compiled `.class` output
on its classpath). Gradle fail-fasts on the first task failure by default. Since `compileJava` has
had real errors every single session so far, **every previous `./gradlew compileClientJava`
invocation in this entire porting project just re-printed compileJava's own failure and never
actually attempted to compile a single file under `src/client/java`** - confirmed conclusively via
`./gradlew compileClientJava --continue`, whose task-execution log shows only `:compileJava
FAILED` and no `:compileClientJava` line at all (skipped-as-a-dependency-of-a-failed-task, standard
Gradle behavior, not a bug). Every past "client errors: N" figure in this document for any session
was actually just `compileJava`'s own error list under a different task name - verified by diffing
file paths in the output (100% `src/main/...`, zero `src/client/...`, every time).

### The workaround that gives a real signal: direct `javac`, bypassing Gradle's task graph entirely
Resolved the real client compile classpath once via a temporary Gradle task
(`sourceSets.client.compileClasspath.asPath`), then ran `javac` directly against **both** source
trees together (`find src/main/java src/client/java -name "*.java" > sources.txt`, then
`javac -Xmaxerrs 5000 -encoding UTF-8 -cp "<resolved classpath>" -d <scratch dir> @sources.txt`).
This does NOT enforce Loom's split-source-set restriction (main can't see client symbols) since
both roots are compiled together as one unit - that check still only comes from the real
`compileJava`/`compileClientJava` tasks once `compileJava` is clean - but it reliably surfaces
every real compile error in `src/client/java` for the first time in this project's history.
**Note**: attempting to encode this same recipe as a proper Gradle `JavaCompile` task (source =
both sourceSets, classpath = client's, `options.fork = true`) was tried and, for reasons NOT fully
root-caused despite real effort, still silently dropped diagnostics for a chunk of client files
even though `sourceSets.client.java.files` correctly listed them as registered inputs - some
interaction between Gradle's JavaCompile wrapping and this specific project/environment that
plain command-line `javac` doesn't have. Given time constraints this session, the recommended
verification method going forward is the **raw javac command**, not a Gradle task - re-derive the
classpath string whenever dependencies change (it's stable otherwise) and keep the two file-list +
javac commands above as the standard "real client verification" recipe. A comment pointing at this
section was left in `build.gradle` in place of the (removed, non-working) task attempt.

### MAJOR FINDING enabled by finally seeing real client errors: `RenderType` moved packages in MC 1.21.11
`net.minecraft.client.renderer.RenderType` no longer exists at that path - verified directly via
`javap`/`unzip -l` against the real resolved `minecraft-clientOnly` mapped jar: the class moved to
**`net.minecraft.client.renderer.rendertype.RenderType`** (new subpackage; a sibling `RenderTypes`
class also lives there now). This is pure vanilla MC API drift, unrelated to Create Fly, and would
have silently broken the client build the moment `compileClientJava` ever got a chance to run -
completely invisible until this session's investigation. Fixed the import across all **17**
affected files (`grep -rl "^import net.minecraft.client.renderer.RenderType;"` found them all;
verified zero remaining references to the old package afterward).

### Other real bugs found and fixed via the new verification method (all previously invisible)
- `level.isClientSide` (private field, must be `level.isClientSide()` - the same MC 1.21.11 API
  change documented in earlier sessions) had **14 more occurrences** than previously known, in files
  that had never been checked because they only ever appeared in the never-run client compile or
  were coincidentally past the point earlier capped views stopped: `BrakeBlock`, `BrassChuteBlock`,
  `EncasedCrossConnectorBlock`, `BoilerData`, `FluidVesselBlock`, `InvertedClutchBlock`,
  `KineticBatteryBlock`, `LinkedAnalogLeverBlockEntity`, `LinkedButtonBlock`, `LinkedLeverBlock`,
  `LinkedTransmitterBlock`, `LinkedTransmitterItem`, `OverstressClutchBlock`, `ShearPinBlock`.
- `addBehaviours(List<BlockEntityBehaviour>)` raw-type -> `List<BlockEntityBehaviour<?>>` (the same
  fix pattern from the session 8 audit) had 5 more occurrences: `BrakeBlockEntity`,
  `CentrifugalClutchBlockEntity`, `FreewheelClutchBlockEntity`, `KineticBridgeBlockEntity`,
  `ShearPinBlockEntity`.
- `javax.annotation.Nullable` (stale JSR-305, not reliably on the Fabric classpath - same fix
  pattern as session-1/6's `@Nonnull` cleanup) -> `org.jetbrains.annotations.Nullable`, 5 files:
  `ContraptionMusicManager`, `FluidVesselBlockEntity`, `ItemSiloBlock`, `CriterionTriggerBase`,
  `SimpleCCTrigger`.
- `CCBlockEntityRenders.java` (this session's own new file, from earlier in session 9): the
  `BlockEntityRendererRegistry.register(CCBlockEntityTypes.CRANK_WHEEL, CrankWheelRenderer::new)`
  call has a real generic-inference bug - explicit `<E,S>` type witnesses on the call don't work
  because `create()`'s return type (`BlockEntityRenderer<T,S>`) is invariant in the provider's own
  type params, and `CrankWheelRenderer` implements `BlockEntityRenderer<HandCrankBlockEntity,...>`
  not `BlockEntityRenderer<CrankWheelBlockEntity,...>` - explicit witnesses forced the wrong target
  type. Fixed by assigning the method reference to an explicitly-typed local variable
  (`BlockEntityRendererProvider<HandCrankBlockEntity, HandCrankRenderer.HandCrankRenderState>
  crankWheelRendererFactory = CrankWheelRenderer::new;`) instead, which lets javac's simpler
  variable-assignment inference succeed where the generic-method-call inference couldn't. Good
  reminder that even code written this same session needs this new verification method, not just
  old code.

### BIG FINDING, NOT YET ACTED ON - `CopycatModel`'s entire API was rewritten in Create Fly 1.21.11
While investigating the `ModelData`/`RenderType` errors in the copycat model files
(`CopycatBeamModel`, `CopycatBlockModel`, `CopycatBoardModel`, `CopycatFenceModel`,
`CopycatFenceGateModel`, `CopycatSlabModel`, `CopycatStairsModel`, `CopycatVerticalStepModel`,
`CopycatWallModel`, `ISimpleCopycatModel` - 10 files), decompiled the real
`com.zurrtum.create.client.infrastructure.model.CopycatModel` (their shared base class) and found
it's been **completely rewritten** for MC's new "unbaked model parts" rendering pipeline
(`WrapperBlockStateModel`, `addPartsWithInfo(...)` returning a `List<BlockModelPart>`-shaped type,
no more `getQuads`/`ModelData`/`RenderType`-parametrized `getCroppedQuads` at all). This is **not**
a simple import-path or signature fix like the other findings above - it's a full architectural
rewrite of how copycat blocks assemble their borrowed-material quads, on the scale of the
`ValueInput`/`ValueOutput` finding from session 8, possibly larger (10 files, each with nontrivial
crop/rotate/assemble geometry logic). **Deliberately not attempted this session** - flagging
prominently as the single biggest known remaining item in `content/`, to be scoped and tackled
as its own dedicated pass (this mod's copycat block quad-cropping logic itself, e.g. the beam/slab/
vertical-step crop math already fixed via `BakedModelHelper.cropAndMove` this session, is probably
still adaptable to the new API shape - it's the outer `getCroppedQuads(...)` entry point and
`CopycatModel` base class integration that needs to change, not necessarily the crop math itself).

### Corrected, trustworthy status after this session (via the new verification method)
Direct `javac` against **both** source trees together: **1057 unique errors across 142 files**
(down from 1110/147 measured at the start of this specific investigation, after the RenderType +
quick-win fixes above). Of those, **107 files / 846 errors are under `src/main`** (matches
`./gradlew compileJava`'s real, trustworthy count exactly) and **~35 files / ~211 errors are under
`src/client`** - this client-side error count has never been visible in any previous session's
reporting and is now, for the first time, a real number. The single largest remaining known
concentration in `src/client` is the `CopycatModel` rewrite above (10 files); the rest is a mix of
NeoForge `ModelData`/`Capabilities` references (expected, tracked) and other not-yet-triaged items.

## PROGRESS session 10: CopycatModel architectural rewrite (the largest known remaining item)

**Note**: from this session on, only this worktree's own `PORTING_NOTES.md` is being maintained -
the coordinator carries it forward into `main` on merge, so there's no need to also write directly
to `F:\create-connected-fly\PORTING_NOTES.md` outside the worktree (that dual-write was a stopgap
for an earlier drift issue, no longer needed).

### The real API: MC 1.21.11's "unbaked model parts" rendering pipeline
Confirmed via `javap` against the real resolved `minecraft-clientOnly` mapped jar (not guessed):
- `net.minecraft.client.renderer.block.model.BlockStateModel` (real name of the old `BakedModel`-
  shaped wrapper interface Create Fly's `WrapperBlockStateModel`/`CopycatModel` implement) -
  `collectParts(RandomSource, List<BlockModelPart>)` + `particleIcon()`. Its nested
  `BlockStateModel.UnbakedRoot` interface has `bake(BlockState, ModelBaker)` +
  `visualEqualityGroup(BlockState)` - this is the real name for the second constructor parameter
  type on every `CopycatModel` subclass (Create Fly's decompiled sources call it `class_9979` in
  `WrapperBlockStateModel`, easily mistaken for `BlockStateModel.Unbaked` since that also exists
  but is a *different*, single-arg-`bake` interface - verified by checking the actual 2-arg `bake`
  signature matches `UnbakedRoot`, not `Unbaked`).
- `net.minecraft.client.renderer.block.model.BlockModelPart` (real name for what was baked
  `BakedQuad` lists keyed by cull-face before) - `getQuads(@Nullable Direction)`,
  `useAmbientOcclusion()`, `particleIcon()`.
- `net.minecraft.client.renderer.block.model.SimpleModelWrapper` - the concrete record
  implementing `BlockModelPart`: `(QuadCollection quads, boolean useAmbientOcclusion,
  TextureAtlasSprite particleIcon)`.
- `net.minecraft.client.resources.model.QuadCollection` + nested `QuadCollection.Builder` - the
  real replacement for manually accumulating a `List<BakedQuad>` per cull face:
  `addUnculledFace(BakedQuad)` / `addCulledFace(Direction, BakedQuad)` / `build()`.
- `BakedQuad` **is now a Java record** with renamed accessors: `direction()` (was `getDirection()`),
  `sprite()` (was `getSprite()`), `tintIndex()` (was `getTintIndex()`) - a separate, easy-to-miss
  MC 1.21.11 API drift on top of the model-parts rewrite itself. Fixed the 4 files that called the
  old accessor names directly (`ISimpleCopycatModel`, `CopycatBeamModel`, `CopycatSlabModel`,
  `CopycatVerticalStepModel` - the ones that inspect `quad.getDirection()` for per-quad skip logic).
- Real Create Fly's own `com.zurrtum.create.client.infrastructure.model.CopycatModel` (the shared
  base class - unchanged by us, we only extend it) confirms the new abstract method shape:
  `protected abstract void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState
  state, CopycatBlock block, BlockState material, RandomSource random, List<BlockModelPart>
  parts)` - replacing the old NeoForge-era `getCroppedQuads(BlockState state, Direction side,
  RandomSource rand, BlockState material, ModelData wrappedData, RenderType renderType) ->
  List<BakedQuad>`. It also still provides `getModelOf(material)`, `getMaterialParts(...)`,
  `addModelParts(...)`, and `gatherOcclusionData(...)` as helper methods, all real and unchanged.

### The conversion pattern applied to all 10 files
Old code was invoked once per queried cull-face (`side` parameter: null for unculled, or one of
the 6 directions), returning a flat `List<BakedQuad>` for just that face. New code is invoked once
total and must build the whole `QuadCollection` itself. The safest, most behavior-preserving
translation (verified against 3 real Create Fly reference examples decompiled from the sources jar
- `CopycatStepModel`, `CopycatPanelModel`, `FluidTankModel` - all doing exactly this pattern):
for each source `BlockModelPart` (from `getMaterialParts(...)`), build one `QuadCollection.Builder`,
then for each of the 7 "query directions" (`null` + `Iterate.directions`), call
`part.getQuads(direction)`, apply the exact same per-quad crop/skip logic the old code had, and
route survivors into `builder.addUnculledFace(quad)` (for the `null` query) or
`builder.addCulledFace(direction, quad)` (for a real direction) - i.e., quads stay in the same
cull-face bucket they were queried from, only their geometry changes via `cropAndMove`. Finally
wrap with `parts.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(),
part.particleIcon()))`.
- `CopycatBlockModel` (no cropping at all) needed no per-face loop - it now just delegates straight
  to the base class's `addModelParts(...)` helper, exactly like real Create Fly's own
  `CopycatPanelModel` does for its "trapdoor material" special case.
- `ISimpleCopycatModel.assemblePiece(...)` (the shared helper 5 of the 9 real block models use -
  board/fence/fencegate/stairs/wall) changed its two list parameters (`sourceQuads`, `destQuads`)
  to `(BlockModelPart part, QuadCollection.Builder builder)`, doing the same per-query-direction
  loop internally once, so none of its 5 callers needed anything beyond a mechanical
  `assemblePiece(templateQuads, quads, ...)` -> `assemblePiece(part, builder, ...)` argument swap
  plus wrapping their whole body in the new outer "for each material part" loop.
- All 9 block model files + the shared interface verified compiling clean via the session 9
  javac-direct-invocation method (990 unique errors / 134 files after, down from 994/136 before
  touching them, and zero of these 10 files appear in the error list at all going forward).

### The missing piece nobody had wired yet: none of these 10 classes were ever actually used
Searched the entire codebase (`grep -rln "new CopycatBlockModel\|new CopycatSlabModel\|..."`) and
found **zero** instantiation sites for any of the 9 model classes, in either this mod or (via the
full extracted Create Fly sources) Create Fly itself owning a generic hook for third-party blocks.
Real Create Fly wires its OWN copycat model classes (`CopycatPanelModel`, `CopycatStepModel`) via:
- `com.zurrtum.create.client.AllModels.ALL` - a `Map<Block, BiFunction<BlockState,
  BlockStateModel.UnbakedRoot, BlockStateModel.UnbakedRoot>>` resolver registry, populated via
  `register(AllBlocks.COPYCAT_STEP, CopycatStepModel::new)` (this only works because
  `CopycatStepModel`'s constructor signature exactly matches the `BiFunction`'s shape, and the
  class itself doubles as an `UnbakedRoot` once baked, per `WrapperBlockStateModel`).
- `com.zurrtum.create.client.mixin.BlockStateModelLoaderMixin` - a real mixin into VANILLA's own
  `net.minecraft.client.resources.model.BlockStateModelLoader.loadBlockStateDefinitionStack(...)`,
  injecting at the `NEW` (constructor) of the returned `LoadedModels`, capturing the local
  `Map<BlockState, BlockStateModel.UnbakedRoot> models` via MixinExtras' `@Local`, and calling
  `models.replaceAll(factory)` if `AllModels.ALL` has an entry for the block being loaded.

Since this only wraps Create Fly's OWN blocks (registered in ITS OWN `AllModels.ALL`), our mod's
blocks are never touched by it - we need the exact same two pieces for ourselves. Added:
- `registries/CCModels.java` (client) - our own `Map<Block, BiFunction<...>>` registry, mirroring
  `AllModels.ALL` exactly, registering all 9 real `CCBlocks.COPYCAT_*` fields to their
  `CopycatXxxModel::new` constructor references.
- `mixin/copycat/BlockStateModelLoaderMixin.java` (client) - a direct copy of Create Fly's own real
  mixin shape (same target method, same injection point, same `@Local` capture), just consulting
  `CCModels.ALL` instead. Registered in `create_connected.mixins.json`'s `"client"` array.
  **One real compile-time obstacle**: `BlockStateModelLoader.LoadedBlockModelDefinition` (one of
  the target method's own parameter types) is `private` in the real class - can't be named directly
  in our own mixin handler method's signature under plain `javac`. Used `List<?>` for that
  parameter instead (erasure-compatible, and unused in the handler body) rather than fighting
  Java's access rules for a type we don't actually need.
- `CCModels.register()` wired into `CreateConnectedClient.onInitializeClient()`.

### CAVEAT, disclosed not hidden: mixin classes may not be 100% verifiable via the javac workaround
The private-type issue above raises a real possibility that Loom's actual Gradle-orchestrated
compile (which runs the Mixin annotation processor with its own special handling for referencing
mixin-target-internal types) behaves differently than plain command-line `javac` for **mixin
classes specifically** - our workaround verifies "does this file compile as ordinary Java," which
isn't quite the same guarantee as "will Mixin successfully weave this into the target class at
runtime." The `BlockStateModelLoaderMixin` compiles clean under the javac workaround as written
(with the `List<?>` erasure workaround), but its real correctness (the `@Local` capture matching
the right local variable slot, the `@At("NEW")` injection point actually existing at that
bytecode offset in the target method) can only be fully confirmed once a real
`./gradlew compileClientJava` succeeds (requires `compileJava` to be clean first - see session 9's
root-cause finding) or the mod is actually run. Flagging this prominently rather than claiming
full confidence - a future session should re-verify this specific file once a full Gradle client
compile becomes possible.

### Status after this session
Direct javac (both source trees together): 990 unique errors / 134 files (down from 994/136
at the point the CopycatModel investigation started, and down from the session's starting point of
1057/142 after the mechanical RenderType/isClientSide/etc fixes - see session 9 for those). All 10
CopycatModel-family files plus the new `CCModels`/`BlockStateModelLoaderMixin` wiring compile clean
and introduce zero new errors elsewhere.

## PROGRESS session 11: remaining content/ files - Ponder move, vanilla Block-state API migration, getCloneItemStack/placeInWorld drift

### Ponder scenes package (12 files + CCPonderPlugin) - entirely client-only, moved wholesale
Confirmed via the sources jar that Create Fly's WHOLE ponder system (`com.zurrtum.create.ponder.
api.*`, `com.zurrtum.create.foundation.ponder.*`, `com.zurrtum.create.infrastructure.ponder.*`)
moved to `com.zurrtum.create.client.*` equivalents - makes sense, ponder is pure in-game-tutorial
rendering with no server-side component. Moved all 12 of this mod's `ponder/*Scenes.java` files
from `src/main/java` to `src/client/java` (verified nothing in `src/main` referenced them), did a
blanket import-root swap (`com.zurrtum.create.ponder.` -> `com.zurrtum.create.client.ponder.`,
`com.zurrtum.create.foundation.ponder.` -> `com.zurrtum.create.client.foundation.ponder.`), and
fixed `CCPonderPlugin.java` (already correctly in `src/client/java`) the same way. Also found
`Create.asResource(String)` (the mod-id resource helper on Create Fly's own entrypoint class)
doesn't exist anymore - replaced the 2 call sites with `Identifier.fromNamespaceAndPath(Create.
MOD_ID, ...)` directly.

**Also found and fixed: none of this was ever actually wired up either** (same class of finding as
session 10's CopycatModel discovery). Real Create Fly registers its own ponder plugin via a direct
call - `PonderIndex.addPlugin(new CreatePonderPlugin())` from its client entrypoint - not via
Fabric entrypoints or ServiceLoader (checked both, neither is used). Added the equivalent
`PonderIndex.addPlugin(new CCPonderPlugin())` call to `CreateConnectedClient.onInitializeClient()`.

### Vanilla MC 1.21.11 Block/BlockState API migration - widespread, affects ~15 files
Verified via `javap` against the real resolved jars (not guessed). Several per-state query methods
that used to be overridable at the `Block` level with `(BlockGetter/LevelAccessor, BlockPos)`
params moved to be simpler, state-only, or were folded into `BlockStateBase` instance methods:
- `getPistonPushReaction(BlockState)` **removed entirely as an override point** - replaced by
  `BlockBehaviour.Properties.pushReaction(PushReaction)`, a static property set at block
  construction time. (Not yet applied anywhere this session - none of the flagged files actually
  had a `getPistonPushReaction` override needing this treatment once inspected; flagging the fact
  for any future file that does.)
- `getOcclusionShape(BlockState)` / `propagatesSkylightDown(BlockState)` - both **dropped their
  `(BlockGetter/LevelReader, BlockPos)` params entirely**, now purely state-based (both the
  `Block`-level override and the `BlockState` instance-method equivalent). Fixed in
  `CopycatFenceBlock`, `CopycatFenceGateBlock`, `CopycatWallBlock`.
- `updateShape(...)` - the single biggest one, affecting **11 files**. Old:
  `updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor
  level, BlockPos pos, BlockPos neighborPos)`. New:
  `updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
  BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource
  random)` - reordered, and the single `LevelAccessor` param split into its two constituent
  interfaces (`LevelAccessor extends LevelReader, ScheduledTickAccess` among others) as two
  separate params. Where the old body needed mutation capability (`scheduleTick`/`getBlockTicks`),
  those specific calls now go through the new `scheduledTickAccess` param instead of a level-wide
  cast - confirmed via `ProperWaterloggedBlock.updateWater(LevelReader, ScheduledTickAccess,
  BlockState, BlockPos)`'s own real signature already expecting the same split, so this isn't a
  workaround, it's the intended real shape. Fixed all 11: `AbstractBEShaftBlock`,
  `CopycatFenceBlock`, `CopycatFenceGateBlock`, `MigratingCopycatBlock`,
  `MigratingWaterloggedCopycatBlock`, `CopycatStairsBlock`, `CopycatWallBlock`, `CrankWheelBlock`,
  `DashboardBlock`, `FluidVesselBlock`, `KineticBridgeDestinationBlock`. The 5 `ICopycatWithWrappedBlock`-style files also needed their internal `state.updateShape(...)` *instance* call
  (5-arg old shape) converted to the new 7-arg instance shape (same reordering, minus the leading
  "this state").
- `getAnalogOutputSignal(BlockState, Level, BlockPos)` -> `getAnalogOutputSignal(BlockState, Level,
  BlockPos, Direction)` - gained a trailing `Direction` param (both the `Block`-level override and
  the `BlockState` instance-method call site). Fixed in 9 files: `CentrifugalClutchBlock`,
  `FluidVesselBlock`, `FreewheelClutchBlock`, `InventoryAccessPortBlock`, `InventoryBridgeBlock`,
  `ItemSiloBlock`, `KineticBatteryBlock`, `OverstressClutchBlock`,
  `SequencedPulseGeneratorBlock`. None of the bodies actually needed the new `direction` value for
  anything beyond passing it through to a recursive/target call, so this was a safe, mechanical
  signature-only change everywhere.
- `Block.rotate(BlockState, LevelAccessor, BlockPos, Rotation)` -> `Block.rotate(BlockState,
  Rotation)` (dropped the level/pos params entirely) - fixed in `CrankWheelBlock`.
- `Item.getCloneItemStack(BlockState, HitResult, LevelReader, BlockPos, Player)` ->
  `getCloneItemStack(LevelReader, BlockPos, BlockState, boolean includeData)` - dropped
  `HitResult`/`Player` entirely, gained a trailing `includeData` boolean. Fixed in 7 files.
  **Real, disclosed feature reduction** in 3 of them (`LinkedAnalogLeverBlock`, `LinkedButtonBlock`,
  `LinkedLeverBlock`): their old bodies used the removed `HitResult` to distinguish "player
  pick-blocked the visual base part" vs. "player pick-blocked the lever/button part" via an
  `isHittingBase(...)` helper, returning a different item stack for each. Since there's no more
  HitResult to make that distinction, all three now always return this mod's own item (matching
  the old "not hitting base" branch) - the block's actual placement/interaction behavior is
  completely unaffected, only this one pick-block nuance is gone. Commented clearly at each site.
- `PlacementOffset.placeInWorld(Level, BlockItem, Player, InteractionHand, BlockHitResult)` (Create
  Fly's own catnip placement-helper class, not vanilla) -> `placeInWorld(Level, BlockItem, Player,
  InteractionHand)` - dropped the trailing `BlockHitResult` and, in `CrankWheelItem`, the return
  type is now directly `InteractionResult` (no more `.result()` unwrapping needed). Fixed in 6
  files: `CopycatBeamBlock`, `CopycatSlabBlock`, `CopycatVerticalStepBlock`, `CrankWheelItem`,
  `KineticBatteryBlock`, `ShearPinBlock`.

### IMPORTANT WORKAROUND LIMITATION FOUND: access wideners are invisible to the javac-direct method
While fixing the above, hit `ButtonBlock.type`/`ButtonBlock.ticksToStayPressed`/`JukeboxBlockEntity.
jukeboxSongPlayer` "private access" errors (8 total) in `LinkedButtonBlock`/wherever the
access-widener-covered fields are used. **These are false positives of the javac-direct
verification workaround, not real bugs** - `src/main/resources/create_connected.accesswidener`
already declares exactly these 3 fields as accessible (confirmed by reading the file), and access
wideners are a Loom-specific bytecode transform applied to the dependency jars before compilation -
my raw `javac` invocation uses the untransformed jars directly, so it can never see the widened
access. This is the same class of blind spot as the Mixin-private-type issue from session 10, just
for a different Loom feature. **Do not "fix" these particular errors** by chasing alternate
non-private APIs - they're already correctly handled via the access widener and will compile fine
under a real `./gradlew` build. Treat any future "X has private access" error as a first-check
against `create_connected.accesswidener` before assuming it's a real problem.

### Real Gradle build attempt (per the coordinator's request, not just the javac workaround)
Ran `./gradlew compileJava` for real this session (not just the javac-direct method) as a sanity
cross-check: **532 unique errors / 91 files**, all under `src/main` - consistent with (a subset
of) the javac-direct method's combined 670/125 count, confirming the workaround's main-sourceset
portion is trustworthy. `compileClientJava`/`build` still cannot be attempted for real (per session
9's root-cause finding - they hard-depend on `compileJava` succeeding first), so
`BlockStateModelLoaderMixin`'s true Mixin-weaving correctness is still unverified by a real build -
that remains open until `compileJava` reaches zero errors.

### Status after this session
Direct javac (both source trees together): **670 unique errors / 125 files** (down from 990/134 at
the start of this session - session 10's CopycatModel work, plus this session's ponder move and
Block-state API migration, combined for a ~320-error reduction). Real `./gradlew compileJava`:
**532/91** (main-sourceset subset, cross-checked consistent).

## Constraints / house rules
- Don't add speculative abstractions or backwards-compat shims. Match the existing
  code's structure/intent as closely as Fabric + Create Fly allow.
- No comments explaining WHAT code does; only non-obvious WHY (e.g. a Create Fly API
  quirk that isn't self-evident from the code).
- This is a big mechanical+judgment task — work through it methodically, verify each
  package compiles before moving to the next, and don't declare the port "done" until
  `./gradlew build` actually succeeds.

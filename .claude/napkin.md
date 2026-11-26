# Napkin Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-11-24] Gradle en Windows exige JAVA_HOME de Android Studio**
   Do instead: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` antes de `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain`.
2. **[2026-11-25] 93 tests unitarios; `ExploreViewModelTest` usa `MainDispatcherRule` en `core/testing`**
   Do instead: fakes en memoria por repositorio + `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))` para activar `stateIn(WhileSubscribed)`.
3. **[2026-11-24] Material3 1.4.0 expone `MotionScheme` como `internal`**
   Do instead: usar `PanamaMotion.spatial()/bouncy()` (springs propios) en `Theme.kt`; no intentar `MotionScheme.expressive()` hasta subir material3.
4. **[2026-11-24] Room no tiene tests instrumentados**
   Do instead: verificar migraciones comparando `app/schemas/.../N.json` con el SQL de `Migration` (createSql debe coincidir carácter por carácter).

## Shell & Command Reliability
1. **[2026-11-24] Salida de Gradle en PowerShell marca "NativeCommandError" aunque compile**
   Do instead: leer la última línea `BUILD SUCCESSFUL` y el conteo de `test-results`, no el exit code de la tubería.

## Domain Behavior Guardrails
1. **[2026-11-24] Snackbar único en el Scaffold raíz**
   Do instead: leer `LocalSnackbarHostState.current` en cada Route; nunca montar `SnackbarHost` en pantallas hijas ni pasar el estado por parámetro.
2. **[2026-11-24] El feed no debe reordenarse al dar like**
   Do instead: `ExploreViewModel` congela `frozenFeedOrder` en `toggleLike`; suéltalo en tab, filtros, `retry` y si aparece un ID nuevo (`rankedPosts.all { it.id in order }`), nunca con `Int.MAX_VALUE`.
3. **[2026-11-24] Room sin migraciones destructivas**
   Do instead: toda subida de `version` en `PanamaTripDatabase` exige `Migration` explícita; solo `fallbackToDestructiveMigrationOnDowngrade()`.
4. **[2026-11-24] `@ColumnInfo(defaultValue)` necesita literal SQL**
   Do instead: textos entre comillas (`"'COMPLETED'"`), enteros sin comillas (`"0"`); luego confirmar en el schema JSON que `createSql` incluye `DEFAULT`.
5. **[2026-11-25] Escrituras de track: un solo escritor y secuencia al capturar**
   Do instead: un único consumidor de `Channel<PersistenceCommand>` en `persistenceScope` (orden cabecera→lotes→cierre); `TrackRecorderEngine` fija `TrackPoint.sequence = points.size`; jamás `countPoints()` al insertar.
6. **[2026-11-25] `RecorderSession.Active` debe exponer `trackId`**
   Do instead: la UI excluye ese id de `findResumable()` y publica `Idle` solo desde el escritor tras guardar/descartar; usa `stopSelf(lastStartId)`.

## User Directives
1. **[2026-11-24] Material 3 Expressive puro**
   Do instead: superficies sólidas de alto contraste; sin Haze, blur ni efectos de vidrio.
2. **[2026-11-24] Máximo 5 lecturas directas por sesión**
   Do instead: delegar exploración a subagentes y usar `grep`/rangos puntuales para editar.

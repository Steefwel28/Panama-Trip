# AGENTS.md — Reglas Globales de Panama-Trip

## Contexto del Proyecto
Aplicación nativa Android minimalista (Material 3 Expressive) para exploración de destinos naturales en Panamá y planificación logística de ruta (clima, combustible, paradas gastronómicas por horario, peajes con alerta de efectivo, reseñas y directorio de guías). Toda la información de ruta debe seguir siendo legible sin señal móvil: la persistencia offline es requisito, no extra.

## Stack Obligatorio
- **Lenguaje:** Kotlin (JDK 17+).
- **Build:** AGP **9.5.1**, Kotlin **2.3.21**, Gradle Version Catalog (`gradle/libs.versions.toml`). Nunca declares versiones sueltas en `build.gradle.kts`.
- **SDK:** `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37` (Android 17).
- **UI:** Jetpack Compose + Material 3 Expressive (sin vistas XML, sin librerías de blur/Haze).
- **Arquitectura:** Clean Architecture + MVVM estricta por feature.
- **Persistencia Offline:** Room (`2.8.5` + `room-ktx`) + DataStore.
- **Imágenes/EXIF:** Coil 3 + `androidx.exifinterface`.

## Estructura Real de Paquetes (`com.panamatrip.app`)
- `core/` — `database` (entidades, DAOs, `PanamaTripDatabase`, seeder), `datastore`, `designsystem`, `format`, `intents`, `preview`, `ui`.
- `data/` — `mapper`, `photo` (`PhotoInspector`), `repository/*Impl`.
- `domain/` — `geo`, `logistics`, `model`, `photo`, `ranking`, `repository`, `track` (`TrackRecorderEngine`, `TrackMetricsCalculator`, `ElevationGainCalculator`), `affinity`.
- `features/` — `explore`, `destinationdetail`, `routeplan`, `saved`, `postcomposer`, `trackrecorder`, `guides`.
- `di/AppContainer.kt`, `navigation/PanamaTripNavHost.kt`.

## Reglas Inquebrantables
1. Nunca escribas lógica de negocio dentro de funciones `@Composable`.
2. Todo estado de pantalla se modela como `StateFlow<UiState>` inmutable con `sealed interface`.
3. **Migraciones Room obligatorias:** cualquier cambio en `entities` sube `PanamaTripDatabase.VERSION` y **exige** un `Migration` explícito registrado en `Room.databaseBuilder(...).addMigrations(...)`. Prohibido `fallbackToDestructiveMigration(dropAllTables = true)`: solo se admite `fallbackToDestructiveMigrationOnDowngrade()`. Los tracks GPS y rutas guardadas del usuario no se pueden perder.
4. **Escrituras atómicas:** toda operación que toque más de una tabla o haga read-modify-write (likes, pertenencia a colecciones, guardar track + puntos) va dentro de `@Transaction` en el DAO o `withTransaction { }` en el repositorio.
5. **Nada de I/O en el hilo principal:** lectura de EXIF, decodificación de imágenes, cifrado o consultas Room se ejecutan con `withContext(ioDispatcher)` y reciben el `CoroutineDispatcher` por inyección (nada de `Dispatchers.IO` hardcodeado en clases de dominio).
6. **Persistencia incremental de GPS:** el grabador escribe en Room por lotes con un scope no cancelable (`NonCancellable` / application scope); al iniciar la app se detectan tracks `IN_PROGRESS` huérfanos para ofrecer recuperación.
7. Antes de cerrar cualquier tarea ejecuta, en PowerShell:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat testDebugUnitTest assembleDebug --console=plain
   ```
   No declares éxito sin `BUILD SUCCESSFUL` y 0 fallos.
8. Si encuentras deuda técnica, strings quemados o recomposiciones evitables, refactoriza antes de responder.
9. Aplica la skill `ponytail` (nivel `full`): el cambio más pequeño que resuelva la tarea completa; cierra cada respuesta nombrando en una línea lo omitido y el riesgo.

## Estándar Visual Android 17 (API 37)
- `enableEdgeToEdge()` en `MainActivity` y `WindowInsets` gestionados: las imágenes llegan hasta los bordes físicos.
- Material 3 **Expressive puro**: superficies sólidas y limpias de alto contraste, Dynamic Color + paleta propia (verde selva profundo, piedra volcánica, acentos terracota).
- **Prohibido:** tarjetas translúcidas tipo Liquid Glass, la librería `Haze`, shaders AGSL, `RenderEffect` blur, cristal líquido y `BorderStroke` gris para separar tarjetas. Usa elevación tonal (`surfaceContainerLow` / `surfaceContainerHigh`).
- **Desenfoque permitido (excepción):** `Modifier.blur(16.dp)` nativo de Compose (API 31+) aplicado **exclusivamente al contenedor de fondo** (imagen/degradado) detrás de ventanas modales o menús sólidos con `surfaceContainerHighest`. Las tarjetas y modales en sí nunca son translúcidos. En API 26–30 el fondo cae a un scrim oscuro sólido (sin blur).
- Esquinas amplias (16.dp–24.dp) y transiciones suaves (`AnimatedVisibility` / `SharedTransitionLayout`).
- **Movimiento y háptica:** navegación, filtros de senderos, calculadoras de ruta/combustible y modales usan `spring` + `HapticFeedback` consistente.
- **Arranque sin bloqueos:** `runBlocking` prohibido en `MainActivity`; usa caché ligera en `SharedPreferences` sincronizada con DataStore.

## Idioma Obligatorio de Comunicación
- Responde y narra **siempre en español latinoamericano natural**, incluso cuando `SPEC.md` o las skills estén en inglés. No cambies a inglés para explicar pasos.

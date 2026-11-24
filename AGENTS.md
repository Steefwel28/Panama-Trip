# AGENTS.md â€” Reglas Globales de Panama-Trip

## Contexto del Proyecto
AplicaciÃ³n nativa Android minimalista (Material Design 3) para exploraciÃ³n de destinos naturales en PanamÃ¡ y planificaciÃ³n logÃ­stica de ruta (clima, combustible, paradas gastronÃ³micas por horario, peajes con alerta de efectivo, reseÃ±as y directorio de guÃ­as).

## Stack Obligatorio
- **Lenguaje:** Kotlin (JDK 17+).
- **UI:** Jetpack Compose + Material 3 (sin vistas XML).
- **Arquitectura:** Clean Architecture + MVVM estricta dividida por features (`destinations`, `planner`, `fuel`, `tolls`, `reviews`, `guides`).
- **Persistencia Offline:** Room + DataStore (toda ruta guardada debe funcionar sin seÃ±al mÃ³vil).
- **Dependencias:** Gestionadas exclusivamente mediante Gradle Version Catalog (`gradle/libs.versions.toml`).

## Reglas Inquebrantables
1. Nunca escribas lÃ³gica de negocio dentro de funciones `@Composable`.
2. Todo estado de pantalla debe modelarse como un `StateFlow<UiState>` inmutable usando `sealed interface`.
3. Antes de dar por terminada cualquier tarea, ejecuta `./gradlew testDebugUnitTest` y `./gradlew assembleDebug` para verificar compilaciÃ³n y pruebas.
4. Si encuentras deuda tÃ©cnica, strings quemados o recomposiciones evitables, refactoriza antes de responder.
5. Aplica siempre la skill `ponytail` (nivel `full`): el cambio mÃ¡s pequeÃ±o que resuelva la tarea completa; cierra cada respuesta nombrando en una lÃ­nea lo que se omitiÃ³ y el riesgo.

## EstÃ¡ndar Visual Android 17 (API 37)
- Configura `compileSdk = 37` y `targetSdk = 37` en `libs.versions.toml` (AGP 8.9.0+).
- Activa `enableEdgeToEdge()` en `MainActivity` y gestiona `WindowInsets` para que las imÃ¡genes lleguen hasta los bordes fÃ­sicos del telÃ©fono.
- Usa Material 3 con Dynamic Color + paleta personalizada (verde selva profundo, piedra volcÃ¡nica y acentos terracota).
- Prohibido usar bordes grises (`BorderStroke`) para separar tarjetas: usa elevaciÃ³n tonal con `surfaceContainerLow` y `surfaceContainerHigh`.
- Implementa transiciones suaves (`AnimatedVisibility` / `SharedTransitionLayout`) y esquinas redondeadas amplias (16.dp a 24.dp).

## Idioma Obligatorio de ComunicaciÃ³n
- Responde y narra **siempre en espaÃ±ol latinoamericano natural**, incluyendo tus comentarios intermedios antes y despuÃ©s de ejecutar comandos, lecturas o ediciones de archivos.
- Nunca cambies al inglÃ©s para explicar quÃ© vas a hacer, aunque `@SPEC.md` o las skills internas estÃ©n escritas en inglÃ©s.

## EstÃ¡ndar Visual y Fluidez (ActualizaciÃ³n M3 Expressive + Liquid Glass)
- **Material 3 Expressive + HÃ¡ptica:** Toda interacciÃ³n clave (navegaciÃ³n, filtros de senderos, calculadoras de ruta/combustible y modales) debe usar animaciones de resorte (`spring`) y respuesta hÃ¡ptica (`HapticFeedback`) consistente.
- **Liquid Glass (Haze + AGSL):** Aplicar refracciÃ³n, desenfoque de fondo real (`HazeState`) y bordes especulares solo en elementos flotantes (barra inferior, menÃºs y hojas modales), manteniendo las tarjetas base limpias y con contraste tipogrÃ¡fico blindado.
- **Arranque sin bloqueos:** Prohibido `runBlocking` en `MainActivity`; usar cachÃ© ligero en `SharedPreferences` sincronizado con `DataStore`.


## Estándar Visual y Fluidez (Material 3 Expressive + Háptica)
- **Material 3 Expressive puro:** Superficies sólidas y limpias de alto contraste, sin efectos de vidrio ni desenfoques experimentales.
- **Movimiento y Háptica:** Toda interacción clave (navegación, filtros de senderos, calculadoras de ruta/combustible y modales) debe usar animaciones de resorte (spring) y respuesta háptica (HapticFeedback) consistente.
- **Arranque sin bloqueos:** Prohibido unBlocking en MainActivity; usar caché ligero en SharedPreferences sincronizado con DataStore.
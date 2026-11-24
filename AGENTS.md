# AGENTS.md — Reglas Globales de Panama-Trip

## Contexto del Proyecto
Aplicación nativa Android minimalista (Material Design 3) para exploración de destinos naturales en Panamá y planificación logística de ruta (clima, combustible, paradas gastronómicas por horario, peajes con alerta de efectivo, reseñas y directorio de guías).

## Stack Obligatorio
- **Lenguaje:** Kotlin (JDK 17+).
- **UI:** Jetpack Compose + Material 3 (sin vistas XML).
- **Arquitectura:** Clean Architecture + MVVM estricta dividida por features (`destinations`, `planner`, `fuel`, `tolls`, `reviews`, `guides`).
- **Persistencia Offline:** Room + DataStore (toda ruta guardada debe funcionar sin señal móvil).
- **Dependencias:** Gestionadas exclusivamente mediante Gradle Version Catalog (`gradle/libs.versions.toml`).

## Reglas Inquebrantables
1. Nunca escribas lógica de negocio dentro de funciones `@Composable`.
2. Todo estado de pantalla debe modelarse como un `StateFlow<UiState>` inmutable usando `sealed interface`.
3. Antes de dar por terminada cualquier tarea, ejecuta `./gradlew testDebugUnitTest` y `./gradlew assembleDebug` para verificar compilación y pruebas.
4. Si encuentras deuda técnica, strings quemados o recomposiciones evitables, refactoriza antes de responder.
5. Aplica siempre la skill `ponytail` (nivel `full`): el cambio más pequeño que resuelva la tarea completa; cierra cada respuesta nombrando en una línea lo que se omitió y el riesgo.

## Estándar Visual Android 17 (API 37)
- Configura `compileSdk = 37` y `targetSdk = 37` en `libs.versions.toml` (AGP 8.9.0+).
- Activa `enableEdgeToEdge()` en `MainActivity` y gestiona `WindowInsets` para que las imágenes lleguen hasta los bordes físicos del teléfono.
- Usa Material 3 con Dynamic Color + paleta personalizada (verde selva profundo, piedra volcánica y acentos terracota).
- Prohibido usar bordes grises (`BorderStroke`) para separar tarjetas: usa elevación tonal con `surfaceContainerLow` y `surfaceContainerHigh`.
- Implementa transiciones suaves (`AnimatedVisibility` / `SharedTransitionLayout`) y esquinas redondeadas amplias (16.dp a 24.dp).

## Idioma Obligatorio de Comunicación
- Responde y narra **siempre en español latinoamericano natural**, incluyendo tus comentarios intermedios antes y después de ejecutar comandos, lecturas o ediciones de archivos.
- Nunca cambies al inglés para explicar qué vas a hacer, aunque `@SPEC.md` o las skills internas estén escritas en inglés.

## Estándar Visual y Fluidez (Actualización M3 Expressive + Liquid Glass)
- **Material 3 Expressive + Háptica:** Toda interacción clave (navegación, filtros de senderos, calculadoras de ruta/combustible y modales) debe usar animaciones de resorte (`spring`) y respuesta háptica (`HapticFeedback`) consistente.
- **Liquid Glass (Haze + AGSL):** Aplicar refracción, desenfoque de fondo real (`HazeState`) y bordes especulares solo en elementos flotantes (barra inferior, menús y hojas modales), manteniendo las tarjetas base limpias y con contraste tipográfico blindado.
- **Arranque sin bloqueos:** Prohibido `runBlocking` en `MainActivity`; usar caché ligero en `SharedPreferences` sincronizado con `DataStore`.

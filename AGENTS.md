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

## Estándar Visual Android 17 (API 37)
- Configura `compileSdk = 37` y `targetSdk = 37` en `libs.versions.toml` (AGP 8.9.0+).
- Activa `enableEdgeToEdge()` en `MainActivity` y gestiona `WindowInsets` para que las imágenes lleguen hasta los bordes físicos del teléfono.
- Usa Material 3 con Dynamic Color + paleta personalizada (verde selva profundo, piedra volcánica y acentos terracota).
- Prohibido usar bordes grises (`BorderStroke`) para separar tarjetas: usa elevación tonal con `surfaceContainerLow` y `surfaceContainerHigh`.
- Implementa transiciones suaves (`AnimatedVisibility` / `SharedTransitionLayout`) y esquinas redondeadas amplias (16.dp a 24.dp).

## Idioma Obligatorio de Comunicación
- Responde y narra **siempre en español latinoamericano natural**, incluyendo tus comentarios intermedios antes y después de ejecutar comandos, lecturas o ediciones de archivos.
- Nunca cambies al inglés para explicar qué vas a hacer, aunque `@SPEC.md` o las skills internas estén escritas en inglés.

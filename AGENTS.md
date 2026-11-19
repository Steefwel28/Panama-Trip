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

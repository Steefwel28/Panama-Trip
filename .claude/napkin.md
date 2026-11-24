# Napkin Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-11-24] Gradle en Windows exige JAVA_HOME de Android Studio**
   Do instead: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` antes de `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain`.
2. **[2026-11-24] 85 tests unitarios cubren dominio; Room no tiene tests instrumentados**
   Do instead: verificar migraciones comparando `app/schemas/.../N.json` con el SQL de `Migration` (createSql debe coincidir carácter por carácter).

## Shell & Command Reliability
1. **[2026-11-24] Salida de Gradle en PowerShell marca "NativeCommandError" aunque compile**
   Do instead: leer la última línea `BUILD SUCCESSFUL` y el conteo de `test-results`, no el exit code de la tubería.

## Domain Behavior Guardrails
1. **[2026-11-24] Room sin migraciones destructivas**
   Do instead: toda subida de `version` en `PanamaTripDatabase` exige `Migration` explícita; solo `fallbackToDestructiveMigrationOnDowngrade()`.
2. **[2026-11-24] `@ColumnInfo(defaultValue)` necesita literal SQL**
   Do instead: textos entre comillas (`"'COMPLETED'"`), enteros sin comillas (`"0"`); luego confirmar en el schema JSON que `createSql` incluye `DEFAULT`.
3. **[2026-11-24] Escrituras de track por lotes deben serializarse**
   Do instead: `Mutex.withLock` alrededor de `persistBatch`/`save`/`discard`; `sequence` se calcula dentro de la sección crítica y el scope es de proceso (no del servicio).

## User Directives
1. **[2026-11-24] Material 3 Expressive puro**
   Do instead: superficies sólidas de alto contraste; sin Haze, blur ni efectos de vidrio.
2. **[2026-11-24] Máximo 5 lecturas directas por sesión**
   Do instead: delegar exploración a subagentes y usar `grep`/rangos puntuales para editar.

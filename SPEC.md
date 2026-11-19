# SPEC.md — Especificación Técnica de Panama-Trip

## 1. Arquitectura Online-First con Respaldo Offline
- **Conexión Activa + Caché Local:** La aplicación opera conectada a red para consumir el feed comunitario, destinos, clima y cálculos logísticos. Todo plan de ruta generado, los tracks GPX descargados y las fichas consultadas se persisten en **Room** para acceso íntegro sin cobertura móvil.
- **Sincronización Diferida (WorkManager):** Si el usuario graba una ruta, redacta un reporte o adjunta fotos en un sendero sin señal, la app guarda todo en cola local junto con la lectura GPS y lo sube automáticamente en segundo plano al recuperar conexión.
- **Doble Coordenada Geográfica por Destino:**
  - `parkingCoordinates` (Estacionamiento o acceso vehicular): punto exacto enviado mediante intents nativos a **Waze** o **Google Maps**.
  - `attractionCoordinates` (Atractivo natural): coordenadas del cerro, río o cascada usadas para calcular el tramo a pie y validar presencia física por GPS.

## 2. Pantalla Principal: Feed Social y Directorio
- **Interruptor Superior (`SingleChoiceSegmentedButtonRow`):** Alterna entre **Comunidad** (feed vertical inmersivo estilo Strava/Instagram) y **Directorio** (catálogo filtrable por ecosistema, provincia, vehículo compatible y conmutador Lista / Mapa de pines).
- **Tarjeta Inteligente de Visita Detectada:** Si el sistema detecta una visita reciente sin reseñar (por GPS satelital o metadatos EXIF dentro de los últimos 14 días), muestra un banner sutil en la parte superior invitando a subir fotos y experiencia.
- **Publicaciones del Feed Comunitario:**
  - Tarjetas editoriales edge-to-edge con foto principal, sello temporal (`MM/AAAA`), etiqueta de actividad reciente y cápsula translúcida estilo Strava con datos verificados: vehículo usado (*Sedán*, *SUV*, *4x4*), distancia/tiempo de caminata y trazado visual del recorrido si el usuario lo grabó.
  - Interacciones sociales: **Corazón** (like), **Comentarios** (con chips rápidos de estado de vía como *"Vía despejada"*, *"Mucho lodo"*, *"Río crecido"*) y **Guardar en Colecciones** (*"Para este finde"*, *"Cascadas"*, *"Rutas 4x4"*).
  - **Botón "Planificar ruta" (Replicar Trip):** Carga de un toque ese destino, alertas de peaje y cálculo de combustible en la pestaña **Mi Ruta**.
- **Motor de Afinidad Silencioso y Modo Invitado:** Cualquier usuario puede explorar, dar likes y guardar posts sin crear cuenta (persistencia local en Room/DataStore). La app pondera intereses según sus guardados e interacciones para priorizar aventuras afines en el feed, migrando todo a su cuenta el día que decida registrarse para publicar.
- **Filtro Rápido de Vehículo y Distancia:** Selector directo (*"¿En qué carro vas?"*) y cálculo de distancia/tiempo estimado desde la ubicación actual del usuario.

## 3. Grabación Opcional de Ruta y Trofeo de Expedición (Estilo Strava)
- **Tracking GPS Voluntario:** Al llegar al punto de estacionamiento, el usuario puede iniciar opcionalmente el registro de su caminata mediante un `ForegroundService` ligero que funciona 100% offline por satélite, midiendo tiempo transcurrido, distancia recorrida y desnivel.
- **Doble Propósito (Trofeo + Guía Comunitaria):** Al finalizar, el trazado (polilínea minimalista) se guarda como trofeo en el historial del usuario y puede adjuntarse a su post comunitario. Los trazados verificados alimentan el mapa del sendero para orientar a futuros visitantes entre el estacionamiento y el atractivo.

## 4. Comunidad Verificada y Flujo Offline de Galería
- **Doble Llave de Verificación (GPS + EXIF):** Solo pueden publicar fotos y reseñas quienes demuestren presencia física mediante GPS satelital en sitio o coordenadas GPS incrustadas en los metadatos EXIF de la foto original elegida desde el Android Photo Picker (bloqueando capturas de pantalla y fotos reenviadas por WhatsApp).
- **Compresión y Supervivencia Sin Red:** Toda foto seleccionada se redimensiona y comprime localmente (máximo `1600px`, WebP/JPEG 80%) y se guarda en `filesDir/pending_uploads` hasta completar su subida.
- **Ventana Diferida de 14 Días y Curaduría:** Una visita verificada habilita el permiso de publicación durante 14 días. El sistema soporta moderación administrativa para depurar contenido incoherente y parte de una base inicial verificada.

## 5. Motor Logístico Dinámico y Guías Locales
- **Planificación Asistida por IA:** Cálculo dinámico de ruta, clima de expedición, combustible ida y vuelta (91, 95, Diésel + recargo opcional +15% montaña/4x4), peajes de Panamá (alerta de alto contraste para tramos **solo en efectivo** como Autopista Panamá–Colón frente a Panapass) y paradas gastronómicas según horario estimado de paso.
- **Directorio de Guías Locales:** Guías certificados por destino (esquema de pago único) con tarifa referencial, idiomas, insignia de verificación y botón directo a **WhatsApp**.

## 6. Estándar Técnico y Visual (Android 17 / API 37)
- `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`, Kotlin + Jetpack Compose + Material Design 3.
- Pantalla completa real (`enableEdgeToEdge()`), cabeceras inmersivas con degradado (`scrim`), superficies tonales (`surfaceContainerLow` / `surfaceContainerHigh`) sin `BorderStroke`, `SharedTransitionLayout` y `@Preview` obligatorio en tema claro y oscuro por pantalla.

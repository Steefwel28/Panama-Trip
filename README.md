# 🥾 Panama-Trip ᕕ( ᐛ )ᕗ

> **Descubre nuevos destinos naturales en Panamá y planifica tu viaje de principio a fin con precisión logística.**

**Panama-Trip** es una aplicación nativa para Android diseñada con una estética minimalista bajo **Material 3**. No es solo un catálogo de ríos, cascadas, lagos y montañas; es un copiloto de ruta que calcula tus gastos reales de viaje, anticipa el clima de la jornada y te alerta sobre detalles críticos en la vía antes de que enciendas el motor.

**Estado actual:** ✅ **Bloque 3 (v0.3.0-alpha) completado** — grabador GPS en segundo plano y publicación comunitaria con verificación de coordenadas EXIF ya funcionales. El mapa interactivo con MapLibre y OpenStreetMap llega en **v0.4.0-alpha**.

---

## ✨ Características Principales

* 🗺️ **Exploración de Destinos Naturales:** Directorio curado de senderos, cerros, ríos, lagos y cascadas en Panamá con fichas detalladas.
* 🌦️ **Plan de Ruta con Pronóstico:** Proyección climática estimada para el día exacto de tu expedición.
* ⛽ **Calculadora de Combustible:** Estimación de consumo según la distancia del trayecto y recomendaciones de estaciones de combustible en la vía.
* 🍽️ **Paradas Gastronómicas Inteligentes:** Sugerencias de restaurantes en la ruta filtradas según tu horario (desayuno, almuerzo o cena) si contemplas detenerte a comer.
* 💵 **Presupuesto y Alertas de Peaje:** Cálculo total de gastos operativos (combustible + peajes) con notificaciones preventivas sobre métodos de pago (ej. aviso de peajes *solo en efectivo* como en la autopista hacia Colón).
* 🥾 **Grabador GPS de Expedición:** Registro opcional de la caminata con `ForegroundService`, 100 % offline por satélite, que mide distancia, tiempo activo, velocidad media y desnivel acumulado.
* 📸 **Comunidad y Reseñas Verificadas:** Sistema de calificación, comentarios y galería de fotos subidas por senderistas que ya completaron el recorrido, con validación de presencia física mediante coordenadas EXIF.
* 🧭 **Directorio de Guías Locales:** Espacio dedicado para guías certificados que deseen promocionar sus servicios dentro de la plataforma mediante un esquema de pago único.

---

## 🛠️ Stack Tecnológico

Construido desde cero siguiendo los estándares modernos del desarrollo nativo en Android:

- **Lenguaje:** Kotlin (JDK 17)
- **Interfaz de Usuario:** Jetpack Compose + Material Design 3 (Material You)
- **Arquitectura:** MVVM / Clean Architecture
- **Persistencia Offline:** Room + DataStore
- **Compatibilidad SDK:** `minSdk 26` (Android 8.0) / `compileSdk 37` y `targetSdk 37` (Android 17)
- **Distribución:** Android Package (`.apk`)

---

## ✅ Bloque 2 (v0.2) — Completado

### 🏞️ 8 destinos de Panamá listos

| # | Destino | Provincia | Ecosistema |
|---|---------|-----------|------------|
| 1 | Cerro Picacho | Panamá Oeste | Montaña |
| 2 | Laguna de San Carlos | Panamá Oeste | Laguna |
| 3 | Los Picachos de Olá | Coclé | Montaña |
| 4 | Cerro Azul | Panamá | Bosque nuboso |
| 5 | Cascada El Salto | Chiriquí | Cascada |
| 6 | La Yeguada | Veraguas | Pinar |
| 7 | Río Lagarto | Colón | Cascada |
| 8 | Bahía de Portobelo | Colón | Laguna / manglar |

Cada ficha incluye descripción editorial, notas de acceso, coordenadas de estacionamiento y del atractivo, distancia y tiempo de caminata, desnivel y vehículo recomendado.

### ⛽ Motor logístico 100 % offline

Todo el cálculo corre en Kotlin puro sobre una semilla local, por lo que el plan de ruta funciona sin señal móvil:

- **Combustible:** precios referenciales por galón para **91, 95 y Diésel**, consumo ida y vuelta según el vehículo (Sedán / SUV / 4x4) y **recargo opcional de +15 %** en rutas de montaña o 4x4.
- **Peajes:** catálogo por provincia con tarifa por sentido, total de ida y vuelta y **alerta de alto contraste para tramos solo en efectivo** (Autopista Panamá–Colón) frente a los que aceptan Panapass.
- **Clima:** pronóstico estimado por ecosistema y mes de la expedición.
- **Paradas gastronómicas:** sugerencias de desayuno, almuerzo o cena según la hora de paso por el corredor.
- **Persistencia:** cada plan generado se guarda en Room y puede reinstalarse sin conexión.

### 🧭 4.ª pestaña — Guías Locales

- Nueva pestaña dedicada en la barra inferior, junto a Explorar, Mi Ruta y Guardados.
- Guías certificados por destino con tarifa referencial, idiomas, insignia de verificación y bio.
- **Contacto directo por WhatsApp** mediante enlaces profundos `wa.me` con mensaje prellenado (con respaldo al navegador si la app no está instalada).

---

## ✅ Bloque 3 (v0.3.0-alpha) — Completado

### 🥾 Grabador de rutas GPS en segundo plano

- **ForegroundService dedicado** (`LocationTrackingService`) con `FusedLocationProviderClient`: la grabación continúa al salir de la app y funciona 100 % offline por satélite.
- Métricas en tiempo real: **distancia recorrida**, **tiempo activo** (sin pausas), **velocidad media** y **desnivel acumulado** con filtro de ruido GPS.
- Controles de **iniciar, pausar, reanudar, guardar y descartar**, con notificación persistente y acciones rápidas.
- Cada recorrido se guarda como **trofeo** en Room (`tracks` y `track_points`) y queda disponible en el historial del grabador, accesible desde **Mi Ruta**.
- Cálculos puros en JVM (`ElevationGainCalculator`, `TrackMetricsCalculator`), con pruebas unitarias sobre el cálculo de desnivel.

### 📸 Comunidad con verificación de coordenadas EXIF

- Publicación de fotos y experiencias desde **Explorar → Comunidad** usando el **Android Photo Picker** (`PickVisualMedia`), sin permisos de almacenamiento.
- Lectura de metadatos EXIF de la foto original: `TAG_GPS_LATITUDE`, `TAG_GPS_LONGITUDE` y `TAG_DATETIME_ORIGINAL`.
- `ExifLocationVerifier` valida que la foto haya sido capturada a **menos de 5 km** del atractivo y muestra la insignia **«Foto verificada en sitio»**; sin metadatos, la publicación sigue disponible sin verificación.
- Compresión local a **máximo 1600 px y JPEG 80 %** en `filesDir/pending_uploads`, con carga de URIs `file://` mediante Coil.
- La base local sube a **Room v3** con las nuevas entidades de tracks y los campos de verificación EXIF en las publicaciones.

---

## 🔜 Próximos pasos — Bloque 4 (v0.4.0-alpha)

### 🗺️ Mapa interactivo con MapLibre y OpenStreetMap

- [ ] Integrar **MapLibre Native** sobre teselas de **OpenStreetMap** para explorar destinos, estacionamientos y atractivos en un mapa real.
- [ ] Mostrar pines por destino con su ficha, filtros por ecosistema/vehículo y distancia desde la ubicación actual.
- [ ] Superponer los **trazados GPS grabados** para orientar a futuros visitantes entre el estacionamiento y el atractivo.
- [ ] Preparar caché de teselas para consulta offline en senderos sin cobertura.

### 🧱 Backlog técnico

- [ ] **Sincronización diferida con WorkManager:** cola local para rutas, reseñas y fotos capturadas sin señal, con subida automática al recuperar conexión.
- [ ] **Tarjeta de visita detectada:** banner sutil cuando exista una visita reciente sin reseñar (ventana de 14 días).

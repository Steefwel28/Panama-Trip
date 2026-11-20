# 🥾 Panama-Trip ᕕ( ᐛ )ᕗ

> **Descubre nuevos destinos naturales en Panamá y planifica tu viaje de principio a fin con precisión logística.**

**Panama-Trip** es una aplicación nativa para Android diseñada con una estética minimalista bajo **Material 3**. No es solo un catálogo de ríos, cascadas, lagos y montañas; es un copiloto de ruta que calcula tus gastos reales de viaje, anticipa el clima de la jornada y te alerta sobre detalles críticos en la vía antes de que enciendas el motor.

**Estado actual:** 🚧 **Bloque 2 (v0.2) completado** — catálogo de destinos, motor logístico offline y directorio de guías locales ya funcionales.

---

## ✨ Características Principales

* 🗺️ **Exploración de Destinos Naturales:** Directorio curado de senderos, cerros, ríos, lagos y cascadas en Panamá con fichas detalladas.
* 🌦️ **Plan de Ruta con Pronóstico:** Proyección climática estimada para el día exacto de tu expedición.
* ⛽ **Calculadora de Combustible:** Estimación de consumo según la distancia del trayecto y recomendaciones de estaciones de combustible en la vía.
* 🍽️ **Paradas Gastronómicas Inteligentes:** Sugerencias de restaurantes en la ruta filtradas según tu horario (desayuno, almuerzo o cena) si contemplas detenerte a comer.
* 💵 **Presupuesto y Alertas de Peaje:** Cálculo total de gastos operativos (combustible + peajes) con notificaciones preventivas sobre métodos de pago (ej. aviso de peajes *solo en efectivo* como en la autopista hacia Colón).
* 📸 **Comunidad y Reseñas Verificadas:** Sistema de calificación, comentarios y galería de fotos subidas por senderistas que ya completaron el recorrido.
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

## 🔜 Próximos pasos — Bloque 3 (v0.3)

- [ ] **Tracking GPS voluntario:** registro de la caminata con `ForegroundService`, 100 % offline por satélite, midiendo tiempo, distancia y desnivel.
- [ ] **Verificación con GPS + EXIF:** doble llave de presencia física (lectura GPS en sitio o metadatos EXIF de la foto original del Photo Picker) para habilitar publicaciones y reseñas.
- [ ] **Sincronización diferida con WorkManager:** cola local para rutas, reseñas y fotos capturadas sin señal, con subida automática al recuperar conexión.
- [ ] **Compresión offline de galería:** redimensión local a `1600 px` y WebP/JPEG 80 % en `filesDir/pending_uploads`.
- [ ] **Tarjeta de visita detectada:** banner sutil cuando exista una visita reciente sin reseñar (ventana de 14 días).

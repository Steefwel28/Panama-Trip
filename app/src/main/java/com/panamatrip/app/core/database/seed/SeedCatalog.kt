package com.panamatrip.app.core.database.seed

import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.VehicleType

/**
 * Semilla editorial verificada de la v0.1. Es Kotlin puro para poder validar
 * coordenadas, colecciones y semáforos en pruebas JVM sin Android.
 *
 * Las coordenadas son puntos de referencia pública del istmo; el acceso final
 * siempre se confirma con el semáforo comunitario.
 */
object SeedCatalog {

    const val CURATOR_ROLE = "Editor/Curador"

    val destinations: List<SeedDestination> = listOf(
        SeedDestination(
            id = "cerro-picacho",
            name = "Cerro Picacho",
            province = "Panamá Oeste",
            ecosystem = Ecosystem.MOUNTAIN,
            summary = "Mirador de granito sobre la cordillera de Chame con vista al Pacífico.",
            description = "Un filo rocoso que se levanta entre bosque secundario y potreros. " +
                "La subida alterna sombra y tramos expuestos, ideal para arrancar temprano y " +
                "evitar el calor del mediodía. Desde la cima se ven la bahía de Chame y, en " +
                "días claros, el perfil de la cordillera hacia El Valle.",
            accessNotes = "Los últimos 3 km son de terracería con piedra suelta. El " +
                "estacionamiento es un claro junto al portón de la finca; hay espacio para " +
                "cuatro vehículos y no se debe bloquear el paso de camiones.",
            parkingLatitude = 8.6421,
            parkingLongitude = -79.9538,
            attractionLatitude = 8.6479,
            attractionLongitude = -79.9596,
            heroImageUrl = "https://picsum.photos/seed/picacho-hero/1200/1500",
            walkDistanceMeters = 2300,
            walkMinutes = 65,
            elevationGainMeters = 310,
            recommendedVehicle = VehicleType.SUV,
            featuredOrder = 1,
        ),
        SeedDestination(
            id = "laguna-san-carlos",
            name = "Laguna de San Carlos",
            province = "Panamá Oeste",
            ecosystem = Ecosystem.LAGOON,
            summary = "Espejo de agua dulce rodeado de juncos y garzas en el litoral de San Carlos.",
            description = "Humedal tranquilo de agua dulce a pocos minutos de la playa. " +
                "Al amanecer la superficie queda sin viento y se convierte en espejo. " +
                "Es un punto de avistamiento de aves playeras y garzas; conviene llevar " +
                "binoculares y calzado que se pueda mojar.",
            accessNotes = "Camino de tierra compactada en buen estado hasta el borde de la " +
                "laguna. El estacionamiento es amplio y sombreado por guayacanes.",
            parkingLatitude = 8.5334,
            parkingLongitude = -80.0142,
            attractionLatitude = 8.5371,
            attractionLongitude = -80.0089,
            heroImageUrl = "https://picsum.photos/seed/laguna-carlos/1200/1500",
            walkDistanceMeters = 1100,
            walkMinutes = 25,
            elevationGainMeters = 15,
            recommendedVehicle = VehicleType.SUV,
            featuredOrder = 2,
        ),
        SeedDestination(
            id = "los-picachos-de-ola",
            name = "Los Picachos de Olá",
            province = "Coclé",
            ecosystem = Ecosystem.MOUNTAIN,
            summary = "Tres agujas de roca sobre los cafetales de Olá, la cumbre más técnica del bloque.",
            description = "La ruta trepa por cafetales y bosque de galería hasta el filo de " +
                "los tres picachos. El último tramo exige manos y buen calzado. La vista " +
                "abarca el valle de Olá y, con suerte, la silueta del volcán Barú al oeste.",
            accessNotes = "Requiere 4x4 en temporada de lluvia: hay dos pasos de quebrada y " +
                "pendiente pronunciada. El estacionamiento queda en la finca cafetalera, " +
                "junto al primer portón.",
            parkingLatitude = 8.4198,
            parkingLongitude = -80.6647,
            attractionLatitude = 8.4266,
            attractionLongitude = -80.6702,
            heroImageUrl = "https://picsum.photos/seed/picachos-ola/1200/1500",
            walkDistanceMeters = 3200,
            walkMinutes = 85,
            elevationGainMeters = 520,
            recommendedVehicle = VehicleType.FOUR_BY_FOUR,
            featuredOrder = 3,
        ),
        SeedDestination(
            id = "cerro-azul",
            name = "Cerro Azul",
            province = "Panamá",
            ecosystem = Ecosystem.CLOUD_FOREST,
            summary = "Bosque nuboso a una hora de la capital, con senderos de helechos gigantes.",
            description = "Carretera de montaña que termina en un bosque nuboso permanente. " +
                "El sendero principal cruza quebradas pequeñas y desemboca en un mirador " +
                "sobre el lago Alajuela. La neblina entra a media tarde: conviene bajar con luz.",
            accessNotes = "Asfalto hasta el pueblo y 1.5 km de calle de tierra en buen estado. " +
                "Un sedán llega sin problema al estacionamiento del sendero.",
            parkingLatitude = 9.1682,
            parkingLongitude = -79.4215,
            attractionLatitude = 9.1739,
            attractionLongitude = -79.4288,
            heroImageUrl = "https://picsum.photos/seed/cerro-azul/1200/1500",
            walkDistanceMeters = 1400,
            walkMinutes = 35,
            elevationGainMeters = 120,
            recommendedVehicle = VehicleType.SEDAN,
            featuredOrder = 4,
        ),
        SeedDestination(
            id = "cascada-el-salto",
            name = "Cascada El Salto",
            province = "Chiriquí",
            ecosystem = Ecosystem.WATERFALL,
            summary = "Caída de agua fría entre helechos arborescentes en las afueras de Boquete.",
            description = "Cascada de unos 25 metros con poza natural para remojarse. " +
                "El sendero baja por escalones de tierra y raíces; el piso permanece húmedo " +
                "casi todo el año. Al atardecer el microclima se cierra con neblina.",
            accessNotes = "Se recomienda SUV por el último tramo de lastre y los baches del " +
                "acceso. Estacionamiento techado junto a la caseta de registro.",
            parkingLatitude = 8.7663,
            parkingLongitude = -82.4421,
            attractionLatitude = 8.7702,
            attractionLongitude = -82.4470,
            heroImageUrl = "https://picsum.photos/seed/cascada-salto/1200/1500",
            walkDistanceMeters = 900,
            walkMinutes = 30,
            elevationGainMeters = 180,
            recommendedVehicle = VehicleType.SUV,
            featuredOrder = 5,
        ),
        SeedDestination(
            id = "la-yeguada",
            name = "La Yeguada",
            province = "Veraguas",
            ecosystem = Ecosystem.PINE_FOREST,
            summary = "Pinar de altura y lago volcánico bajo el antiguo cráter de Calobre.",
            description = "Reserva de pino caribeño y bosque de galería alrededor de un lago " +
                "de origen volcánico. El aire es fresco incluso en abril. Hay senderos cortos " +
                "y miradores con mesas para pasar el día en familia.",
            accessNotes = "Carretera pavimentada hasta la entrada y estacionamiento de " +
                "grava dentro de la reserva. Acceso apto para sedán en cualquier temporada.",
            parkingLatitude = 8.4632,
            parkingLongitude = -80.8563,
            attractionLatitude = 8.4689,
            attractionLongitude = -80.8615,
            heroImageUrl = "https://picsum.photos/seed/la-yeguada/1200/1500",
            walkDistanceMeters = 800,
            walkMinutes = 18,
            elevationGainMeters = 40,
            recommendedVehicle = VehicleType.SEDAN,
            featuredOrder = 6,
        ),
    )

    val vehicleReports: List<SeedVehicleReport> = listOf(
        SeedVehicleReport("vr-01", "cerro-picacho", VehicleType.SEDAN, AccessStatus.CAUTION, "Carla Méndez"),
        SeedVehicleReport("vr-02", "cerro-picacho", VehicleType.SUV, AccessStatus.CLEAR, "Valeria Quintero"),
        SeedVehicleReport("vr-03", "cerro-picacho", VehicleType.FOUR_BY_FOUR, AccessStatus.CLEAR, "Mariela Cedeño"),
        SeedVehicleReport("vr-04", "cerro-picacho", VehicleType.SEDAN, AccessStatus.BLOCKED, "Iván Rodríguez"),
        SeedVehicleReport("vr-05", "laguna-san-carlos", VehicleType.SEDAN, AccessStatus.CAUTION, "Diego Aparicio"),
        SeedVehicleReport("vr-06", "laguna-san-carlos", VehicleType.SUV, AccessStatus.CLEAR, "Diego Aparicio"),
        SeedVehicleReport("vr-07", "laguna-san-carlos", VehicleType.FOUR_BY_FOUR, AccessStatus.CLEAR, "Sofía Lasso"),
        SeedVehicleReport("vr-08", "los-picachos-de-ola", VehicleType.SEDAN, AccessStatus.BLOCKED, "Mariela Cedeño"),
        SeedVehicleReport("vr-09", "los-picachos-de-ola", VehicleType.SUV, AccessStatus.CAUTION, "Mariela Cedeño"),
        SeedVehicleReport("vr-10", "los-picachos-de-ola", VehicleType.FOUR_BY_FOUR, AccessStatus.CLEAR, "Rafael Beitia"),
        SeedVehicleReport("vr-11", "los-picachos-de-ola", VehicleType.FOUR_BY_FOUR, AccessStatus.CAUTION, "Iván Rodríguez"),
        SeedVehicleReport("vr-12", "cerro-azul", VehicleType.SEDAN, AccessStatus.CLEAR, "Valeria Quintero"),
        SeedVehicleReport("vr-13", "cerro-azul", VehicleType.SUV, AccessStatus.CLEAR, "Sofía Lasso"),
        SeedVehicleReport("vr-14", "cascada-el-salto", VehicleType.SEDAN, AccessStatus.CAUTION, "Rafael Beitia"),
        SeedVehicleReport("vr-15", "cascada-el-salto", VehicleType.SUV, AccessStatus.CLEAR, "Rafael Beitia"),
        SeedVehicleReport("vr-16", "cascada-el-salto", VehicleType.FOUR_BY_FOUR, AccessStatus.CLEAR, "Carla Méndez"),
        SeedVehicleReport("vr-17", "la-yeguada", VehicleType.SEDAN, AccessStatus.CLEAR, "Diego Aparicio"),
        SeedVehicleReport("vr-18", "la-yeguada", VehicleType.SUV, AccessStatus.CLEAR, "Sofía Lasso"),
    )

    val posts: List<SeedPost> = listOf(
        SeedPost(
            id = "post-picacho-amanecer",
            destinationId = "cerro-picacho",
            authorName = "Valeria Quintero",
            authorInitials = "VQ",
            caption = "Salimos a las 4:40 a.m. y llegamos al filo justo cuando el Pacífico " +
                "se puso naranja. El último kilómetro es piedra suelta: con SUV se sube sin " +
                "sufrir. Lleven dos litros de agua por persona.",
            imageUrl = "https://picsum.photos/seed/picacho-post-1/1200/1500",
            visitedDaysAgo = 6,
            vehicleUsed = VehicleType.SUV,
            walkDistanceMeters = 2300,
            walkMinutes = 65,
            activityTag = "Amanecer",
            baseLikeCount = 248,
        ),
        SeedPost(
            id = "post-laguna-aves",
            destinationId = "laguna-san-carlos",
            authorName = "Diego Aparicio",
            authorInitials = "DA",
            caption = "La laguna amaneció sin viento y con garzas en la orilla. " +
                "Con binoculares contamos nueve especies en una hora. El acceso está " +
                "compactado, no hace falta 4x4.",
            imageUrl = "https://picsum.photos/seed/laguna-post-1/1200/1500",
            visitedDaysAgo = 11,
            vehicleUsed = VehicleType.SUV,
            walkDistanceMeters = 1100,
            walkMinutes = 25,
            activityTag = "Avistamiento de aves",
            baseLikeCount = 176,
        ),
        SeedPost(
            id = "post-ola-cumbre",
            destinationId = "los-picachos-de-ola",
            authorName = "Mariela Cedeño",
            authorInitials = "MC",
            caption = "Tres horas de subida y una cumbre que no perdona. La quebrada bajó " +
                "con buen caudal y el paso estaba lodoso; sin 4x4 no lo intenten en invierno. " +
                "El café de la finca al regreso es obligatorio.",
            imageUrl = "https://picsum.photos/seed/ola-post-1/1200/1500",
            visitedDaysAgo = 18,
            vehicleUsed = VehicleType.FOUR_BY_FOUR,
            walkDistanceMeters = 3200,
            walkMinutes = 85,
            activityTag = "Cumbre",
            baseLikeCount = 412,
        ),
        SeedPost(
            id = "post-azul-neblina",
            destinationId = "cerro-azul",
            authorName = "Valeria Quintero",
            authorInitials = "VQ",
            caption = "Bosque nuboso a 50 minutos de la ciudad. Fuimos en sedán y llegamos " +
                "sin problema hasta el estacionamiento. La neblina entró a las 4 p.m. y el " +
                "sendero se volvió mágico.",
            imageUrl = "https://picsum.photos/seed/azul-post-1/1200/1500",
            visitedDaysAgo = 25,
            vehicleUsed = VehicleType.SEDAN,
            walkDistanceMeters = 1400,
            walkMinutes = 35,
            activityTag = "Bosque nuboso",
            baseLikeCount = 331,
        ),
        SeedPost(
            id = "post-salto-pozas",
            destinationId = "cascada-el-salto",
            authorName = "Rafael Beitia",
            authorInitials = "RB",
            caption = "Agua a 14 grados y una poza que invita a quedarse. El descenso tiene " +
                "escalones resbalosos; bastones de trekking ayudan. Estacionamiento techado " +
                "y caseta de registro abierta hasta las 5 p.m.",
            imageUrl = "https://picsum.photos/seed/salto-post-1/1200/1500",
            visitedDaysAgo = 32,
            vehicleUsed = VehicleType.SUV,
            walkDistanceMeters = 900,
            walkMinutes = 30,
            activityTag = "Cascada",
            baseLikeCount = 289,
        ),
        SeedPost(
            id = "post-yeguada-pinar",
            destinationId = "la-yeguada",
            authorName = "Diego Aparicio",
            authorInitials = "DA",
            caption = "Día de picnic en el pinar. Carretera pavimentada hasta la entrada y " +
                "caminata corta al lago. Ideal para ir con niños o con el carro familiar.",
            imageUrl = "https://picsum.photos/seed/yeguada-post-1/1200/1500",
            visitedDaysAgo = 40,
            vehicleUsed = VehicleType.SEDAN,
            walkDistanceMeters = 800,
            walkMinutes = 18,
            activityTag = "Pinar",
            baseLikeCount = 154,
        ),
        SeedPost(
            id = "post-picacho-atardecer",
            destinationId = "cerro-picacho",
            authorName = "Mariela Cedeño",
            authorInitials = "MC",
            caption = "Segunda visita en el mes. Esta vez subimos en 4x4 y bajamos con " +
                "linterna. El viento en la cumbre es fuerte después de las 5 p.m.",
            imageUrl = "https://picsum.photos/seed/picacho-post-2/1200/1500",
            visitedDaysAgo = 3,
            vehicleUsed = VehicleType.FOUR_BY_FOUR,
            walkDistanceMeters = 2300,
            walkMinutes = 60,
            activityTag = "Fin de semana",
            baseLikeCount = 97,
        ),
    )

    val comments: List<SeedComment> = listOf(
        SeedComment("c-01", "post-picacho-amanecer", "Carla Méndez", "Subí en sedán y toqué fondo en dos piedras. Mejor SUV.", TerrainStatus.HEAVY_MUD, 4),
        SeedComment("c-02", "post-picacho-amanecer", "Iván Rodríguez", "Gracias por el dato del agua, hace falta.", null, 3),
        SeedComment("c-03", "post-ola-cumbre", "Rafael Beitia", "La quebrada estaba crecida el domingo, cruzamos con cuidado.", TerrainStatus.RIVER_HIGH, 8),
        SeedComment("c-04", "post-ola-cumbre", "Sofía Lasso", "Vía despejada hasta el primer portón, el lodo empieza después.", TerrainStatus.ROAD_CLEAR, 5),
        SeedComment("c-05", "post-azul-neblina", "Carla Méndez", "Ojo con las ramas bajas después de la curva 4.", TerrainStatus.LOW_HANGING_BRANCHES, 2),
    )

    val collections: List<SeedCollection> = listOf(
        SeedCollection("col-finde", "Para este finde", "☀️", 0, listOf("post-picacho-amanecer", "post-azul-neblina")),
        SeedCollection("col-cascadas", "Cascadas", "💧", 1, listOf("post-salto-pozas")),
        SeedCollection("col-4x4", "Rutas 4x4", "🧭", 2, listOf("post-ola-cumbre", "post-picacho-atardecer")),
    )

    val reviews: List<SeedReview> = listOf(
        SeedReview("rv-01", "cerro-picacho", "Valeria Quintero", 5, "Subida exigente pero corta. El mirador paga cada paso; llevar agua y gorra.", 6),
        SeedReview("rv-02", "cerro-picacho", "Iván Rodríguez", 4, "Con sedán se sufre en el último tramo. El estacionamiento es pequeño, llegar temprano.", 12),
        SeedReview("rv-03", "laguna-san-carlos", "Diego Aparicio", 5, "Amanecer perfecto para fotografiar aves. Llevar repelente.", 9),
        SeedReview("rv-04", "laguna-san-carlos", "Sofía Lasso", 4, "Fácil de llegar y muy tranquilo entre semana.", 21),
        SeedReview("rv-05", "los-picachos-de-ola", "Mariela Cedeño", 5, "La cumbre más impresionante que he hecho en Panamá. 4x4 obligatorio en invierno.", 15),
        SeedReview("rv-06", "los-picachos-de-ola", "Rafael Beitia", 4, "Ruta técnica y larga. Salir antes de las 7 a.m.", 27),
        SeedReview("rv-07", "cerro-azul", "Valeria Quintero", 5, "Bosque nuboso a menos de una hora de la ciudad. Acceso impecable en sedán.", 4),
        SeedReview("rv-08", "cerro-azul", "Carla Méndez", 4, "Sendero corto y bien marcado. La neblina llega rápido.", 19),
        SeedReview("rv-09", "cascada-el-salto", "Rafael Beitia", 5, "Cascada fría y poza profunda. Los escalones resbalan, ir con calzado adecuado.", 30),
        SeedReview("rv-10", "cascada-el-salto", "Sofía Lasso", 4, "Bonito paseo familiar, aunque el acceso en lastre castiga al carro bajo.", 44),
        SeedReview("rv-11", "la-yeguada", "Diego Aparicio", 5, "Pinar fresco y lago tranquilo. Perfecto para un picnic con niños.", 8),
        SeedReview("rv-12", "la-yeguada", "Carla Méndez", 4, "Acceso pavimentado y estacionamiento amplio. Llevar abrigo.", 33),
    )
}

data class SeedDestination(
    val id: String,
    val name: String,
    val province: String,
    val ecosystem: Ecosystem,
    val summary: String,
    val description: String,
    val accessNotes: String,
    val parkingLatitude: Double,
    val parkingLongitude: Double,
    val attractionLatitude: Double,
    val attractionLongitude: Double,
    val heroImageUrl: String,
    val walkDistanceMeters: Int,
    val walkMinutes: Int,
    val elevationGainMeters: Int,
    val recommendedVehicle: VehicleType,
    val featuredOrder: Int,
)

data class SeedVehicleReport(
    val id: String,
    val destinationId: String,
    val vehicle: VehicleType,
    val verdict: AccessStatus,
    val authorName: String,
)

data class SeedPost(
    val id: String,
    val destinationId: String,
    val authorName: String,
    val authorInitials: String,
    val caption: String,
    val imageUrl: String,
    val visitedDaysAgo: Int,
    val vehicleUsed: VehicleType,
    val walkDistanceMeters: Int,
    val walkMinutes: Int,
    val activityTag: String?,
    val baseLikeCount: Int,
    val authorRole: String = SeedCatalog.CURATOR_ROLE,
)

data class SeedComment(
    val id: String,
    val postId: String,
    val authorName: String,
    val body: String,
    val terrainStatus: TerrainStatus?,
    val daysAgo: Int,
)

data class SeedCollection(
    val id: String,
    val name: String,
    val symbol: String,
    val sortOrder: Int,
    val postIds: List<String>,
)

data class SeedReview(
    val id: String,
    val destinationId: String,
    val authorName: String,
    val rating: Int,
    val body: String,
    val daysAgo: Int,
    val authorRole: String = SeedCatalog.CURATOR_ROLE,
)

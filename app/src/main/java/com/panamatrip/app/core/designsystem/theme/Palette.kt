package com.panamatrip.app.core.designsystem.theme

/**
 * Paleta de marca en ARGB puro. Separada de Compose para que pueda verificarse
 * con pruebas de unidad de contraste sin depender del runtime de Android.
 */
object BrandPalette {

    // Verde selva
    const val JungleDeep = 0xFF00210F
    const val JungleShadow = 0xFF14523A
    const val Jungle = 0xFF2C694E
    const val JungleLight = 0xFF95D5B2
    const val JungleMist = 0xFFB0F1CD

    // Piedra volcánica
    const val StoneDeep = 0xFF0C1F15
    const val StoneShadow = 0xFF374B3E
    const val Stone = 0xFF4F6357
    const val StoneLight = 0xFFB5CCB9
    const val StoneMist = 0xFFD1E8D8

    // Terracota
    const val TerracottaDeep = 0xFF5B1A00
    const val TerracottaShadow = 0xFF7B2F11
    const val Terracotta = 0xFF9C4425
    const val TerracottaLight = 0xFFFFB59B
    const val TerracottaMist = 0xFFFFDBCF

    // Neutros cálidos de día
    const val Sand = 0xFFFBFDF8
    const val SandLow = 0xFFF5F8F2
    const val SandMid = 0xFFEFF2EC
    const val SandHigh = 0xFFE9EDE7
    const val SandHighest = 0xFFE4E8E2
    const val SandVariant = 0xFFDCE5DD
    const val Ink = 0xFF191C1A
    const val InkMuted = 0xFF404943
    const val OutlineLight = 0xFF707972
    const val OutlineVariantLight = 0xFFC0C9C2

    // Neutros volcánicos de noche
    const val Night = 0xFF0F1512
    const val NightLow = 0xFF171D19
    const val NightMid = 0xFF1B211D
    const val NightHigh = 0xFF262C28
    const val NightHighest = 0xFF313733
    const val NightVariant = 0xFF404943
    const val Paper = 0xFFE1E3DF
    const val PaperMuted = 0xFFC0C9C2
    const val OutlineDark = 0xFF8A938C

    // Neutros puros
    const val White = 0xFFFFFFFF
    const val Black = 0xFF000000
    const val SurfaceLowestDark = 0xFF0A0F0C

    // Error
    const val ErrorLight = 0xFFBA1A1A
    const val ErrorContainerLight = 0xFFFFDAD6
    const val OnErrorContainerLight = 0xFF410002
    const val ErrorDark = 0xFFFFB4AB
    const val OnErrorDark = 0xFF690005
    const val ErrorContainerDark = 0xFF93000A
    const val OnErrorContainerDark = 0xFFFFDAD6
}

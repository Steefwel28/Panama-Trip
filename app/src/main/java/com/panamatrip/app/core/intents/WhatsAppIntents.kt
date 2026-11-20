package com.panamatrip.app.core.intents

import android.content.Intent
import android.net.Uri

/**
 * Enlaces profundos de WhatsApp. Se prefiere `wa.me` porque abre la app
 * instalada y cae al navegador cuando el dispositivo no la tiene.
 */
object WhatsAppIntents {

    fun chat(phoneDigits: String, message: String): List<Intent> {
        val normalized = phoneDigits.filter(Char::isDigit)
        if (normalized.isEmpty()) return emptyList()
        val encodedText = Uri.encode(message)
        return listOf(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/$normalized?text=$encodedText"),
            ),
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://api.whatsapp.com/send?phone=$normalized&text=$encodedText"),
            ),
        )
    }
}

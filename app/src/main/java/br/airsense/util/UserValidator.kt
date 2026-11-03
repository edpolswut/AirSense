package br.airsense.util

import android.util.Patterns

/** Cada função devolve a mensagem de erro, ou null se o valor é válido. */
object UserValidator {
    const val MIN_PASSWORD_LENGTH = 8

    fun name(value: String): String? =
        if (value.isBlank()) "Informe seu nome" else null

    fun email(value: String): String? {
        val email = value.trim()
        return when {
            email.isEmpty() -> "Informe o e-mail"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "E-mail inválido"
            else -> null
        }
    }

    const val PASSWORD_RULE =
        "Mínimo de $MIN_PASSWORD_LENGTH caracteres, com maiúscula, minúscula, número e caractere especial"

    // US001: mínimo de 8 caracteres, com maiúscula, minúscula, número e caractere especial.
    fun password(value: String): String? = when {
        value.length < MIN_PASSWORD_LENGTH ->
            "A senha deve ter no mínimo $MIN_PASSWORD_LENGTH caracteres"
        value.none { it.isUpperCase() } -> "A senha deve ter pelo menos uma letra maiúscula"
        value.none { it.isLowerCase() } -> "A senha deve ter pelo menos uma letra minúscula"
        value.none { it.isDigit() } -> "A senha deve ter pelo menos um número"
        value.all { it.isLetterOrDigit() } -> "A senha deve ter pelo menos um caractere especial (ex.: @, #, !)"
        else -> null
    }

    fun confirmPassword(password: String, confirmation: String): String? =
        if (password != confirmation) "As senhas não coincidem" else null
}
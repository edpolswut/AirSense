package br.airsense.model

enum class AuthError(val message: String) {
    EMAIL_ALREADY_EXISTS("Este e-mail já está cadastrado"),
    INVALID_CREDENTIALS("E-mail ou senha incorretos"),
    WRONG_CURRENT_PASSWORD("Senha atual incorreta"),
    USER_NOT_FOUND("Usuário não encontrado")
}

sealed interface AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}
package br.airsense.model

enum class AuthError(val message: String) {
    // Textos exatos dos critérios de aceite (US001 e US007).
    EMAIL_ALREADY_EXISTS("E-mail já cadastrado."),
    INVALID_CREDENTIALS("E-mail ou senha inválidos."),
    ACCOUNT_LOCKED("Acesso bloqueado por 15 minutos após 5 tentativas inválidas. O bloqueio é temporário; tente novamente mais tarde."),
    WRONG_CURRENT_PASSWORD("Senha atual incorreta"),
    USER_NOT_FOUND("Usuário não encontrado")
}

sealed interface AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}
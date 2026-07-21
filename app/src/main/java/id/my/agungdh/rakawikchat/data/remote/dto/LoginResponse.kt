package id.my.agungdh.rakawikchat.data.remote.dto

data class LoginResponse(
    val token: String,
    val user: UserResponse
)

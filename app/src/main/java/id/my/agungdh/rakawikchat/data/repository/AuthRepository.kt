package id.my.agungdh.rakawikchat.data.repository

import id.my.agungdh.rakawikchat.AppContainer
import id.my.agungdh.rakawikchat.data.local.TokenManager
import id.my.agungdh.rakawikchat.data.remote.ApiService
import id.my.agungdh.rakawikchat.data.remote.dto.LoginRequest
import id.my.agungdh.rakawikchat.data.remote.dto.LoginResponse
import id.my.agungdh.rakawikchat.util.Resource

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val container: AppContainer
) {

    suspend fun login(username: String, password: String): Resource<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                tokenManager.saveToken(data.token)
                container.currentToken = data.token
                Resource.Success(data)
            } else {
                Resource.Error(response.body()?.message ?: "Login failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            apiService.logout()
            tokenManager.clearToken()
            container.currentToken = null
            Resource.Success(Unit)
        } catch (e: Exception) {
            tokenManager.clearToken()
            container.currentToken = null
            Resource.Success(Unit)
        }
    }
}

package id.my.agungdh.rakawikchat.data.repository

import id.my.agungdh.rakawikchat.data.remote.ApiService
import id.my.agungdh.rakawikchat.data.remote.dto.UserResponse
import id.my.agungdh.rakawikchat.util.Resource

class UserRepository(
    private val apiService: ApiService
) {

    suspend fun getCurrentUser(): Resource<UserResponse> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to get user")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getAllUsers(): Resource<List<UserResponse>> {
        return try {
            val response = apiService.getAllUsers()
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to get users")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}

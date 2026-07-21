package id.my.agungdh.rakawikchat

import android.app.Application
import id.my.agungdh.rakawikchat.data.local.TokenManager
import id.my.agungdh.rakawikchat.data.remote.ApiService
import id.my.agungdh.rakawikchat.data.remote.StompClient
import id.my.agungdh.rakawikchat.data.repository.AuthRepository
import id.my.agungdh.rakawikchat.data.repository.ChatRepository
import id.my.agungdh.rakawikchat.data.repository.UserRepository
import id.my.agungdh.rakawikchat.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RakawikChatApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(appContext: android.content.Context) {

    val tokenManager = TokenManager(appContext)

    @Volatile
    var currentToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = currentToken?.let { token ->
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } ?: chain.request()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(ApiService::class.java)

    val stompClient = StompClient(okHttpClient)

    val authRepository = AuthRepository(apiService, tokenManager, this)
    val chatRepository = ChatRepository(apiService, stompClient, this)
    val userRepository = UserRepository(apiService)

    init {
        kotlinx.coroutines.runBlocking {
            currentToken = tokenManager.getToken()
        }
    }
}

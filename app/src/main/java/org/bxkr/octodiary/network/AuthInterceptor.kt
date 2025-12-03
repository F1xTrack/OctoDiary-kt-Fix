package org.bxkr.octodiary.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import org.bxkr.octodiary.DataService

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = try {
            if (DataService.states.isNotEmpty() && DataService.hasUserId) {
                DataService.token
            } else {
                null
            }
        } catch (e: UninitializedPropertyAccessException) {
            null
        } catch (e: Exception) {
            null
        }

        val requestWithToken = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(requestWithToken)

        if (response.code == 401) {
            // Check if the current request is for token refresh itself to avoid infinite loop
            if (originalRequest.url.encodedPath.contains("sps/oauth/te")) {
                return response // It's a refresh request that failed, don't try to refresh again
            }

            // Try to refresh token
            val isRefreshSuccessful = MESLoginService.performTokenRefreshSync(context)

            if (isRefreshSuccessful) {
                response.close() // Close the old response

                // Recreate the request with the new token
                val newRequestWithToken = originalRequest.newBuilder()
                    .header("Authorization", "Bearer ${DataService.token}")
                    .build()
                response = chain.proceed(newRequestWithToken) // Retry the original request
            } else {
                // Token refresh failed, clear credentials (this is a placeholder for now)
                // In a real app, you might want to redirect to login screen
                DataService.token = "" // Clear token
                // TODO: Add logic to clear other auth data and redirect to login
            }
        }

        return response
    }
}

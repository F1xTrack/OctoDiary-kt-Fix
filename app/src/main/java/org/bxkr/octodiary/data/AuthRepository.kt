package org.bxkr.octodiary.data

import android.content.Context
import org.bxkr.octodiary.Diary
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.baseEnqueue
import org.bxkr.octodiary.baseErrorFunction
import org.bxkr.octodiary.baseInternalExceptionFunction
import org.bxkr.octodiary.network.MESLoginService.refreshToken
import org.bxkr.octodiary.network.NetworkService
import org.bxkr.octodiary.network.interfaces.DSchoolAPI
import org.bxkr.octodiary.network.interfaces.SchoolSessionAPI
import org.bxkr.octodiary.models.sessionuser.SessionUser

class AuthRepository(private val context: Context, private val dataService: DataService) {

    // A direct way to access API services
    private val dSchoolApi: DSchoolAPI by lazy { NetworkService.dSchoolApi(NetworkService.BaseUrl.MOS_SCHOOL) }
    private val mosAuthApi by lazy { NetworkService.mosAuthApi() }
    private val schoolSessionApi: SchoolSessionAPI by lazy { NetworkService.schoolSessionApi(NetworkService.BaseUrl.MOS_SCHOOL) } // Need to cast this for MESLoginService

    fun updateUserId(onUpdated: () -> Unit) {
        dataService.dSchoolApi.profilesId(dataService.token)
            .baseEnqueue(dataService::baseErrorFunction, dataService::baseInternalExceptionFunction) { body ->
                if (body.size == 0) {
                    dataService.tokenExpirationHandler?.invoke()
                } else {
                    dataService.userId = body
                    dataService.hasUserId = true
                    onUpdated()
                }
            }
    }

    fun updateSessionUser(onUpdated: () -> Unit) {
        dataService.sessionUser = SessionUser("a") // Dummy data, actual logic might be elsewhere
        dataService.hasSessionUser = true
        onUpdated()
    }

    fun refreshToken(onUpdated: () -> Unit) {

        if (dataService.subsystem == Diary.MES) { // Assuming DataService.subsystem is still accessible
            context.refreshToken { // This is MESLoginService.refreshToken extension function
                // The onUpdated callback needs to be handled after the refresh is complete
                // DataService.token will be updated by MESLoginService.performTokenRefreshSync
                onUpdated()
            }
        } else {
            // Need to implement for other subsystems if applicable
            // secondaryApi.refreshToken("Bearer ${dataService.token}")
            //     .baseEnqueue(dataService::baseErrorFunction) {
            //         dataService.token = it
            //         updateUserId { onUpdated() }
            //     }
            onUpdated() // Placeholder
        }
    }
}

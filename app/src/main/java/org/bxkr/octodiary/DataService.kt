package org.bxkr.octodiary

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody
import org.bxkr.octodiary.data.AuthRepository
import org.bxkr.octodiary.database.AppDatabase
import org.bxkr.octodiary.database.dao.OfflineDao
import org.bxkr.octodiary.database.entity.offline.*
import org.bxkr.octodiary.models.avatar.Avatar
import org.bxkr.octodiary.models.classmembers.ClassMember
import org.bxkr.octodiary.models.classmembers.OctoClassMembers
import org.bxkr.octodiary.models.classranking.RankingMember
import org.bxkr.octodiary.models.daysbalanceinfo.DaysBalanceInfo
import org.bxkr.octodiary.models.events.Event
import org.bxkr.octodiary.models.govexams.GovExamsResponse
import org.bxkr.octodiary.models.homeworks.Homework
import org.bxkr.octodiary.models.lesson2.LessonResponse
import org.bxkr.octodiary.models.lessonschedule.LessonSchedule
import org.bxkr.octodiary.models.mark.MarkInfo
import org.bxkr.octodiary.models.marklistdate.MarkListDate
import org.bxkr.octodiary.models.marklistsubject.MarkListSubjectItem
import org.bxkr.octodiary.models.mealbalance.MealBalance
import org.bxkr.octodiary.models.mealsmenucomplexes.MealsMenuComplexes
import org.bxkr.octodiary.models.persondata.PersonData
import org.bxkr.octodiary.models.profile.ProfileResponse
import org.bxkr.octodiary.models.profilesid.ProfilesId
import org.bxkr.octodiary.models.rankingforsubject.RankingForSubject
import org.bxkr.octodiary.models.schoolinfo.SchoolInfo
import org.bxkr.octodiary.models.sessionuser.SessionUser
import org.bxkr.octodiary.models.subjectranking.SubjectRanking
import org.bxkr.octodiary.models.visits.Payload
import org.bxkr.octodiary.models.visits.VisitsResponse
import org.bxkr.octodiary.network.MESLoginService.refreshToken
import org.bxkr.octodiary.network.NetworkService.externalApi
import org.bxkr.octodiary.network.interfaces.DSchoolAPI
import org.bxkr.octodiary.network.interfaces.MainSchoolAPI
import org.bxkr.octodiary.network.interfaces.SchoolSessionAPI
import org.bxkr.octodiary.network.interfaces.SecondaryAPI
import org.bxkr.octodiary.utils.CacheUtils
import org.bxkr.octodiary.utils.measurePerformance
import java.util.Calendar
import java.util.Date

object DataService {
    private val gson = Gson()
    lateinit var subsystem: Diary
    lateinit var mainSchoolApi: MainSchoolAPI
    lateinit var dSchoolApi: DSchoolAPI
    lateinit var secondaryApi: SecondaryAPI
    lateinit var schoolSessionApi: SchoolSessionAPI
    lateinit var authRepository: AuthRepository
    private lateinit var context: Context

    private var refreshingToken = false
    private val tokenMutex = Mutex()

    private val _tokenMutable = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenMutable

    private val _userIdMutable = MutableStateFlow<ProfilesId>(ProfilesId())
    val userIdFlow: StateFlow<ProfilesId> = _userIdMutable

    private val _sessionUserMutable = MutableStateFlow<SessionUser?>(null)
    val sessionUserFlow: StateFlow<SessionUser?> = _sessionUserMutable

    fun setUserId(newUserId: ProfilesId) {
        _userIdMutable.value = newUserId
    }

    fun setSessionUser(newSessionUser: SessionUser) {
        _sessionUserMutable.value = newSessionUser
    }

    private val _eventCalendarMutable = MutableStateFlow<List<Event>>(emptyList())
    val eventCalendar: StateFlow<List<Event>> = _eventCalendarMutable

    private val _eventsRangeMutable = MutableStateFlow<List<Long>>(emptyList())
    val eventsRange: StateFlow<List<Long>> = _eventsRangeMutable

    private val _rankingMutable = MutableStateFlow<List<RankingMember>>(emptyList())
    val ranking: StateFlow<List<RankingMember>> = _rankingMutable

    private val _classMembersMutable = MutableStateFlow<List<ClassMember>>(emptyList())
    val classMembers: StateFlow<List<ClassMember>> = _classMembersMutable

    private val _subjectRankingMutable = MutableStateFlow<List<SubjectRanking>>(emptyList())
    val subjectRanking: StateFlow<List<SubjectRanking>> = _subjectRankingMutable

    private val _profileMutable = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profileMutable

    private val _visitsMutable = MutableStateFlow<VisitsResponse?>(null)
    val visits: StateFlow<VisitsResponse?> = _visitsMutable

    private val _marksDateMutable = MutableStateFlow<MarkListDate?>(null)
    val marksDateFlow: StateFlow<MarkListDate?> = _marksDateMutable

    private val _marksSubjectMutable = MutableStateFlow<List<MarkListSubjectItem>>(emptyList())
    val marksSubjectFlow: StateFlow<List<MarkListSubjectItem>> = _marksSubjectMutable

    private val _homeworksMutable = MutableStateFlow<List<org.bxkr.octodiary.models.homeworks2.Homework>>(emptyList())
    val homeworksFlow: StateFlow<List<org.bxkr.octodiary.models.homeworks2.Homework>> = _homeworksMutable

    private val _mealBalanceMutable = MutableStateFlow<MealBalance?>(null)
    val mealBalance: StateFlow<MealBalance?> = _mealBalanceMutable

    private val _schoolInfoMutable = MutableStateFlow<SchoolInfo?>(null)
    val schoolInfo: StateFlow<SchoolInfo?> = _schoolInfoMutable

    private val _personDataMutable = MutableStateFlow<PersonData?>(null)
    val personData: StateFlow<PersonData?> = _personDataMutable

    private val _daysBalanceInfoMutable = MutableStateFlow<DaysBalanceInfo?>(null)
    val daysBalanceInfo: StateFlow<DaysBalanceInfo?> = _daysBalanceInfoMutable
    var daysBalanceInfoCompleted = false

    private val _mealsMenuComplexesMutable = MutableStateFlow<MealsMenuComplexes?>(null)
    val mealsMenuComplexes: StateFlow<MealsMenuComplexes?> = _mealsMenuComplexesMutable

    private val _govExamsMutable = MutableStateFlow<GovExamsResponse?>(null)
    val govExamsFlow: StateFlow<GovExamsResponse?> = _govExamsMutable

    private val _avatarsMutable = MutableStateFlow<List<Avatar>>(emptyList())
    val avatarsFlow: StateFlow<List<Avatar>> = _avatarsMutable

    // Оптимизация: кэширование часто используемых данных
    private val memoryCache = mutableMapOf<String, Pair<Any, Long>>()
    private val CACHE_DURATION = 15 * 60 * 1000L // 15 минут

    private fun <T> getCachedData(key: String): T? {
        val cached = memoryCache[key]
        return if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_DURATION) {
            @Suppress("UNCHECKED_CAST")
            cached.first as T
        } else {
            memoryCache.remove(key)
            null
        }
    }

    private fun setCachedData(key: String, data: Any) {
        memoryCache[key] = Pair(data, System.currentTimeMillis())
    }

    fun clearCacheIfLowMemory(context: Context) {
        CacheUtils.clearCacheIfLowMemory(context, memoryCache)
    }



    val fields: List<kotlin.reflect.KProperty<*>>
        get() =
            listOfNotNull(
                ::userIdFlow,
                ::sessionUserFlow,
                ::eventsRange,
                ::ranking,
                ::classMembers,
                ::profile,
                ::visits.takeIf { subsystem == Diary.MES },
                ::marksDateFlow,
                ::marksSubjectFlow,
                ::homeworksFlow,
                ::mealBalance.takeIf { subsystem == Diary.MES },
                ::schoolInfo,
                ::personData,
                ::daysBalanceInfo.takeIf { subsystem == Diary.MES },
                ::mealsMenuComplexes.takeIf { subsystem == Diary.MES },
                ::subjectRanking,
                ::govExamsFlow,
                ::avatarsFlow
            )

    val mapOfDemoResourceIds = mapOf<kotlin.reflect.KProperty<*>, Int>(
        ::userIdFlow to R.raw.demo_user_id,
        ::sessionUserFlow to R.raw.demo_session_user,
        ::eventsRange to R.raw.demo_events_range,
        ::ranking to R.raw.demo_ranking,
        ::classMembers to R.raw.demo_class_members,
        ::profile to R.raw.demo_profile,
        ::visits to R.raw.demo_visits,
        ::marksDateFlow to R.raw.demo_marks_date,
        ::marksSubjectFlow to R.raw.demo_marks_subject,
        ::homeworksFlow to R.raw.demo_homeworks,
        ::mealBalance to R.raw.demo_meal_balance,
        ::schoolInfo to R.raw.demo_school_info,
        ::personData to R.raw.demo_person_data,
        ::daysBalanceInfo to R.raw.demo_days_balance_info,
        ::mealsMenuComplexes to R.raw.demo_meals_menu_complexes,
        ::subjectRanking to R.raw.demo_subject_ranking,
        ::govExamsFlow to R.raw.demo_gov_exams,
        ::avatarsFlow to R.raw.demo_avatars
    ).mapKeys { it.key.name }

    val loadedEverything = mutableStateOf(false)
    var loadingStarted = false
    var tokenExpirationHandler: (() -> Unit)? = null
    var onSingleItemInUpdateAllLoadedHandler: ((name: String, progress: Float) -> Unit)? = null
    private val _currentProfileMutable = MutableStateFlow(0)
    val currentProfile: StateFlow<Int> = _currentProfileMutable

    fun setCurrentProfile(index: Int) {
        _currentProfileMutable.value = index
    }
    val pickedImageUri = MutableStateFlow<Uri?>(null)
    var database: AppDatabase? = null
    var offlineDao: OfflineDao? = null

    fun init(context: Context, authRepository: AuthRepository) {
        this.context = context
        database = AppDatabase.getInstance(context)
        offlineDao = database?.offlineDao()
        this.authRepository = authRepository
    }

    fun loadOfflineData(onUpdated: () -> Unit) {
        val dao = offlineDao ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = dao.getAllEvents().mapNotNull {
                    try { gson.fromJson<Event>(it.json, object : TypeToken<Event>() {}.type) } catch (e: Exception) { null }
                }
                if (events.isNotEmpty()) {
                    _eventCalendarMutable.value = events
                }
                val marks = dao.getAllSubjectMarks().mapNotNull {
                    try { gson.fromJson<MarkListSubjectItem>(it.json, object : TypeToken<MarkListSubjectItem>() {}.type) } catch (e: Exception) { null }
                }
                if (marks.isNotEmpty()) {
                    _marksSubjectMutable.value = marks
                }
                val rankingMembers = dao.getAllRankingMembers().mapNotNull {
                    try { gson.fromJson<RankingMember>(it.json, object : TypeToken<RankingMember>() {}.type) } catch (e: Exception) { null }
                }
                if (rankingMembers.isNotEmpty()) {
                    _rankingMutable.value = rankingMembers
                }
                val visitsPayloads = dao.getAllVisits().mapNotNull {
                    try { gson.fromJson<Payload>(it.json, object : TypeToken<Payload>() {}.type) } catch (e: Exception) { null }
                }
                if (visitsPayloads.isNotEmpty()) {
                    _visitsMutable.value = VisitsResponse(visitsPayloads)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    onUpdated()
                }
            }
        }
    }

    fun updateToken(newToken: String?) {
        _tokenMutable.value = newToken
    }

    fun setProfile(newProfile: ProfileResponse?) {
        _profileMutable.value = newProfile
    }

    fun updateEventCalendar(weeksBefore: Int = 0, weeksAfter: Int = 0, onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateEventCalendar(weeksBefore, weeksAfter, onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) { // Check if profile is initialized
                onUpdated()
                return@launch
            }
            val studentId = profile.value?.children?.get(currentProfile.value)?.studentId // Add studentId here
            if (studentId == null) {
                onUpdated()
                return@launch
            }
            val startDate = Calendar.getInstance().also {
                it.set(Calendar.WEEK_OF_YEAR, it.get(Calendar.WEEK_OF_YEAR) - weeksBefore)
                it.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val endDate = Calendar.getInstance().also {
                it.set(Calendar.WEEK_OF_YEAR, it.get(Calendar.WEEK_OF_YEAR) + weeksAfter)
                it.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }
            secondaryApi.events(
                "Bearer ${tokenFlow.value!!}",
                personIds = profile.value?.children?.get(currentProfile.value)?.contingentGuid ?: "",
                beginDate = startDate.time.formatToDay(),
                endDate = endDate.time.formatToDay(),
                expandFields = "homework,marks"
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) { body ->
                _eventCalendarMutable.value = body.response
                _eventsRangeMutable.value = listOf(startDate.time.time, endDate.time.time)
                offlineDao?.let { dao ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val entities = body.response.map { event ->
                            EventEntity(
                                id = event.id,
                                startAt = event.startAt.parseLongDate().time,
                                finishAt = event.finishAt.parseLongDate().time,
                                json = gson.toJson(event)
                            )
                        }
                        dao.insertEvents(entities)
                    }
                }
                onUpdated()
            }
        }
    }

    fun getEventWeek(date: Date, listener: (events: List<Event>, range: List<Long>) -> Unit) {
        if (tokenFlow.value == null || profile.value?.children.isNullOrEmpty()) {
            return
        }
        val startDate = Calendar.getInstance().also {
            it.time = date
            it.set(Calendar.WEEK_OF_YEAR, it.get(Calendar.WEEK_OF_YEAR))
            it.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val endDate = Calendar.getInstance().also {
            it.time = date
            it.set(Calendar.WEEK_OF_YEAR, it.get(Calendar.WEEK_OF_YEAR))
            it.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        secondaryApi.events(
            "Bearer ${tokenFlow.value!!}",
            personIds = profile.value?.children?.get(currentProfile.value)?.contingentGuid ?: "",
            beginDate = startDate.time.formatToDay(),
            endDate = endDate.time.formatToDay(),
            expandFields = "homework,marks"
        ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) { body ->
            _eventCalendarMutable.value = body.response
            listener(body.response, listOf(startDate.time.time, endDate.time.time))
        }
    }

    fun getMarkInfo(markId: Long, errorListener: (String) -> Unit, listener: (MarkInfo) -> Unit) {
        if (tokenFlow.value == null || profile.value?.children.isNullOrEmpty()) {
            return
        }
        val studentId = profile.value?.children?.get(currentProfile.value)?.studentId
        if (studentId == null) {
            return
        }
        mainSchoolApi.markInfo(
            tokenFlow.value!!,
            markId = markId,
            studentId = studentId
        ).baseEnqueue(errorListenerForMessage(errorListener)) { listener(it) }
    }

    fun updateRanking(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateRanking(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid
            if (contingentGuid == null) {
                onUpdated()
                return@launch
            }

            var rankingFinished = false
            var classMembersFinished = false

            secondaryApi.classRanking(
                tokenFlow.value!!,
                personId = contingentGuid,
                date = Date().formatToDay()
            ).baseEnqueue({ errorBody: ResponseBody, httpCode: Int, className: String? ->
                val errorText = errorBody.string()
                if (errorText.contains("Рейтинг не доступен.")) {
                    _rankingMutable.value = emptyList()
                } else {
                    baseErrorFunction(errorBody, httpCode, className)
                }
            }, ::baseInternalExceptionFunction) {
                _rankingMutable.value = it
                rankingFinished = true
                if (classMembersFinished) onUpdated()
            }
            
            _classMembersMutable.value = emptyList()
            classMembersFinished = true
            if (rankingFinished) onUpdated()
        }
    }

    fun updateCustomClassMembers(onUpdated: () -> Unit) {
                if (!context.isOnline() || tokenFlow.value == null || profile.value?.children.isNullOrEmpty()) {
                    onUpdated()
                    return
                }
                val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid
                val studentId = profile.value?.children?.get(currentProfile.value)?.studentId
                if (contingentGuid == null || studentId == null) {
                    onUpdated()
                    return
                }
        
                mainSchoolApi.pullUserSettingsRaw(tokenFlow.value!!, "od_class_members_assignments")
                    .baseEnqueue({ _, _, _ -> onUpdated() }, ::baseInternalExceptionFunction) { unparsed ->
                        val parsed = unparsed.fromJson<OctoClassMembers>()
                        if (parsed != null) parsed.assignments.let { assignments ->
                            if (assignments != null) {
                                val assignmentsStudentIds =
                                    assignments.map { assignment -> assignment.studentId }
                                val newClassMembers = classMembers.value.map {
                                    if (it.studentId in assignmentsStudentIds) {
                                        it.copy(personId = assignments.first { assignment -> assignment.studentId == it.studentId }.personId)
                                    } else it
                                }
                                _classMembersMutable.value = newClassMembers
                            }
                            onUpdated()
                        } else onUpdated()
                    }
    }

    fun updateSubjectRanking(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateSubjectRanking(onUpdated)
                        }
                    }
                    return@launch
                }
            }
                        if (profile.value?.children.isNullOrEmpty()) {
                            onUpdated()
                            return@launch
                        }
                        val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid
                        if (contingentGuid == null) {
                            onUpdated()
                            return@launch
                        }
            
                        secondaryApi.subjectRanking(
                            tokenFlow.value!!,
                            contingentGuid,
                            Date().formatToDay()
                        ).baseEnqueue({ errorBody: ResponseBody, httpCode: Int, className: String? ->
                val errorText = errorBody.string()
                if (errorText.contains("Рейтинг не доступен.")) {
                    _subjectRankingMutable.value = emptyList()
                    onUpdated()
                } else {
                    baseErrorFunction(errorBody, httpCode, className)
                }
            }) {
                _subjectRankingMutable.value = it
                onUpdated()
            }
        }
    }

    fun updateProfile(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateProfile(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            val cacheKey = "profile_${tokenFlow.value!!.hashCode()}"
            val cachedProfile: ProfileResponse? = getCachedData(cacheKey)
            if (cachedProfile != null && _profileMutable.value != null) {
                _profileMutable.value = cachedProfile
                onUpdated()
                return@launch
            }
            mainSchoolApi.profile(tokenFlow.value!!)
                .baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
                    _profileMutable.value = it
                    setCachedData(cacheKey, it)
                    onUpdated()
                }
        }
    }

    fun updateVisits(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateVisits(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            if (subsystem != Diary.MES) {
                onUpdated()
                return@launch
            }
            val contractId = profile.value?.children?.get(currentProfile.value)?.contractId ?: 0 // handle null properly
            if (contractId == 0L) {
                 onUpdated()
                 return@launch
            }
            mainSchoolApi.visits(
                tokenFlow.value!!,
                contractId,
                fromDate = Calendar.getInstance().apply {
                    time = Date()
                    set(Calendar.DAY_OF_YEAR, get(Calendar.DAY_OF_YEAR) - 61)
                }.time.formatToDay(),
                toDate = Date().formatToDay()
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) { visitsResponse ->
                _visitsMutable.value = VisitsResponse(
                    payload = visitsResponse.payload.sortedByDescending {
                        it.date.parseFromDay().toInstant().toEpochMilli()
                    }
                )
                offlineDao?.let { dao ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val entities = visitsResponse.payload.map { item ->
                            VisitDayEntity(
                                date = item.date,
                                json = gson.toJson(item)
                            )
                        }
                        dao.clearVisits()
                        dao.insertVisits(entities)
                    }
                }
                onUpdated()
            }
        }
    }

    fun updateMarksDate(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateMarksDate(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) { // Check if profile is initialized
                onUpdated()
                return@launch
            }
            val studentId = profile.value?.children?.get(currentProfile.value)?.studentId
            if (studentId == null) {
                onUpdated()
                return@launch
            }
            val cal = Calendar.getInstance()
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            val toDate = cal.time.formatToDay()
            cal.add(Calendar.WEEK_OF_YEAR, -5)
            val fromDate = cal.time.formatToDay()

            try {
                val call: retrofit2.Call<MarkListDate> = mainSchoolApi.markList(tokenFlow.value!!, studentId, fromDate, toDate)
                val response: retrofit2.Response<MarkListDate> = call.execute()
                if (response.isSuccessful) {
                    val body: MarkListDate? = response.body()
                    _marksDateMutable.value = body
                    if (body != null) {
                        setCachedData(::marksDateFlow.name, body)
                    }
                }
                onUpdated()
            } catch (e: Exception) {}
        }
    }

    fun updateMarksSubject(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateMarksSubject(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val studentId = profile.value?.children?.get(currentProfile.value)?.studentId
            if (studentId == null) {
                onUpdated()
                return@launch
            }

            mainSchoolApi.subjectMarks(
                tokenFlow.value!!,
                studentId = studentId
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
                _marksSubjectMutable.value = it.payload
                offlineDao?.let { dao ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val entities = it.payload.map { item ->
                            SubjectMarksEntity(
                                subjectId = item.subjectId,
                                json = gson.toJson(item)
                            )
                        }
                        dao.clearSubjectMarks()
                        dao.insertSubjectMarks(entities)
                    }
                }
                onUpdated()
            }
        }
    }

    fun updateHomeworks(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateHomeworks(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val studentId = profile.value?.children?.get(currentProfile.value)?.studentId
            if (studentId == null) {
                onUpdated()
                return@launch
            }

            mainSchoolApi.homeworks(
                tokenFlow.value!!,
                studentId = studentId,
                fromDate = Date().formatToDay(),
                toDate = Calendar.getInstance().run {
                    set(Calendar.WEEK_OF_YEAR, get(Calendar.WEEK_OF_YEAR) + 1)
                    time
                }.formatToDay()
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
                _homeworksMutable.value = it.payload
                onUpdated()
            }
        }
    }

    fun updateMealBalance(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateMealBalance(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            if (subsystem != Diary.MES) {
                onUpdated()
                return@launch
            }
            val contractId = profile.value?.children?.get(currentProfile.value)?.contractId
            if (contractId == null) {
                onUpdated()
                return@launch
            }

            dSchoolApi.mealBalance(
                tokenFlow.value!!,
                contractId = contractId
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
                _mealBalanceMutable.value = it
                onUpdated()
            }
        }
    }

    fun updateSchoolInfo(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateSchoolInfo(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val child = profile.value?.children?.get(currentProfile.value)
            if (child == null) {
                onUpdated()
                return@launch
            }

            mainSchoolApi.schoolInfo(
                tokenFlow.value!!,
                schoolId = child.school.id,
                classUnitId = child.classUnitId
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
                _schoolInfoMutable.value = it
                onUpdated()
            }
        }
    }

    fun updatePersonData(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updatePersonData(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            _personDataMutable.value = PersonData()
            onUpdated()
        }
    }

    fun updateDaysBalanceInfo(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateDaysBalanceInfo(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid
            if (contingentGuid == null) {
                onUpdated()
                return@launch
            }

            mainSchoolApi.daysBalanceInfo(
                accessToken = tokenFlow.value!!,
                personId = contingentGuid,
                from = "${Date().formatToDay()}T00:00:00.000Z",
                withPayments = false,
                limit = Int.MAX_VALUE
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
                _daysBalanceInfoMutable.value = it
                daysBalanceInfoCompleted = true
                onUpdated()
            }
        }
    }

    fun updateMealsMenuComplexes(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateMealsMenuComplexes(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid
            if (contingentGuid == null) {
                onUpdated()
                return@launch
            }

            mainSchoolApi.mealsMenuComplexes(
                accessToken = tokenFlow.value!!,
                personId = contingentGuid,
                onDate = Date().formatToDay()
            ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
                _mealsMenuComplexesMutable.value = it
                onUpdated()
            }
        }
    }

    fun updateGovExams(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateGovExams(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid
            if (contingentGuid == null) {
                onUpdated()
                return@launch
            }

            val onError = {
                _govExamsMutable.value = GovExamsResponse(listOf(), "OK")
                onUpdated()
            }

            secondaryApi.govExams(
                "Bearer ${tokenFlow.value!!}",
                contingentGuid
            ).baseEnqueue({ _, _, _ -> onError() }, { _, _ -> onError() }) {
                _govExamsMutable.value = it
                onUpdated()
            }
        }
    }

    fun updateAvatars(onUpdated: () -> Unit) {
        if (!context.isOnline()) {
            onUpdated()
            return
        }
        GlobalScope.launch {
            if (tokenFlow.value == null) {
                if (refreshingToken) {
                    tokenMutex.withLock {
                        if (tokenFlow.value == null) {
                            refreshingToken = false
                            onUpdated()
                            return@launch
                        }
                    }
                } else {
                    refreshingToken = true
                    tokenMutex.withLock {
                        authRepository.refreshToken {
                            refreshingToken = false
                            updateAvatars(onUpdated)
                        }
                    }
                    return@launch
                }
            }
            if (profile.value?.children.isNullOrEmpty()) {
                onUpdated()
                return@launch
            }
            val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid
            if (contingentGuid == null) {
                onUpdated()
                return@launch
            }

            // Оптимизация: проверяем кэш для аватаров
            val cacheKey = "avatars_${contingentGuid}"
            val cachedAvatars: List<Avatar>? = getCachedData(cacheKey)
            if (cachedAvatars != null && avatarsFlow.value.isNotEmpty()) {
                _avatarsMutable.value = cachedAvatars
                onUpdated()
                return@launch
            }

            secondaryApi.avatars(
                "Bearer ${tokenFlow.value!!}",
                contingentGuid
            ).baseEnqueue({ _, _, _ ->
                _avatarsMutable.value = emptyList()
                onUpdated()
            }) {
                _avatarsMutable.value = it
                setCachedData(cacheKey, it)
                onUpdated()
            }
        }
    }

    fun getRankingForSubject(
        subjectId: Long,
        errorListener: (String) -> Unit,
        listener: (List<RankingForSubject>) -> Unit,
    ) {
        if (!context.isOnline() || tokenFlow.value == null || profile.value?.children.isNullOrEmpty()) return
        val child = profile.value?.children?.get(currentProfile.value)
        if (child == null) return

        secondaryApi.rankingForSubject(
            tokenFlow.value!!,
            child.contingentGuid,
            child.classUnitId,
            Date().formatToDay(),
            subjectId
        ).baseEnqueue(errorFunction = errorListenerForMessage(errorListener)) { listener(it) }
    }

    fun setHomeworkDoneState(homeworkId: Long, state: Boolean, listener: () -> Unit) {
        if (tokenFlow.value == null) return
        if (state) {
            mainSchoolApi.doHomework(tokenFlow.value!!, homeworkId)
                .baseEnqueue(::baseErrorFunction) { listener() }
        } else {
            mainSchoolApi.undoHomework(tokenFlow.value!!, homeworkId)
                .baseEnqueue(::baseErrorFunction) { listener() }
        }
    }

    fun getLessonInfo(
        lessonId: Long,
        errorListener: (String) -> Unit,
        listener: (LessonResponse) -> Unit,
    ) {
        if (!context.isOnline() || tokenFlow.value == null || profile.value?.children.isNullOrEmpty()) {
            return
        }
        val studentId = profile.value?.children?.get(currentProfile.value)?.studentId ?: return

        mainSchoolApi.lessonSchedule(
            tokenFlow.value!!,
            lessonId,
            studentId
        ).baseEnqueue(errorListenerForMessage(errorListener)) {
            listener(it)
        }
    }

    fun getLaunchUrl(homeworkId: Long, materialId: String, listener: (String) -> Unit) {
        if (tokenFlow.value == null) return
        mainSchoolApi.launchMaterial(tokenFlow.value!!, homeworkId, materialId)
            .baseEnqueue({ errorBody, httpCode, className ->
                if (httpCode < 400) {
                    listener(errorBody.string())
                } else {
                    baseErrorFunction(errorBody, httpCode, className)
                }
            }) {}
    }

    fun getMealsMenuComplexes(date: Date, listener: (MealsMenuComplexes) -> Unit) {
        if (!context.isOnline() || tokenFlow.value == null || profile.value?.children.isNullOrEmpty()) {
            return
        }
        val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid ?: return

        mainSchoolApi.mealsMenuComplexes(
            accessToken = tokenFlow.value!!,
            personId = contingentGuid,
            onDate = date.formatToDay()
        ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
            listener(it)
        }
    }

    fun getBalanceHistory(
        fromDay: String = Date().formatToDay(),
        listener: (DaysBalanceInfo) -> Unit,
    ) {
        if (!context.isOnline() || tokenFlow.value == null || profile.value?.children.isNullOrEmpty()) {
            return
        }
        val contingentGuid = profile.value?.children?.get(currentProfile.value)?.contingentGuid ?: return

        mainSchoolApi.daysBalanceInfo(
            accessToken = tokenFlow.value!!,
            personId = contingentGuid,
            from = "${fromDay}T00:00:00.000Z",
            withPayments = true,
            limit = Int.MAX_VALUE
        ).baseEnqueue(::baseErrorFunction, ::baseInternalExceptionFunction) {
            listener(it)
        }
    }

    fun sendStatistic(onUpdated: () -> Unit) {
        if (userIdFlow.value.isNotEmpty()) {
            externalApi().sendStat(
                subsystem.ordinal,
                encodeToBase64(hash(userIdFlow.value[0].id.toString()))
            ).baseEnqueue { onUpdated() }
        }
    }

    fun <Model> pushUserSettings(
        path: String,
        content: Model,
        onError: (String) -> Unit = {},
        onUpdated: () -> Unit,
    ) {
        if (!context.isOnline() || tokenFlow.value == null) {
            onUpdated()
            return
        }
        mainSchoolApi.pushUserSettings(tokenFlow.value!!, path, Gson().toJsonTree(content).asJsonObject)
            .baseEnqueueOrNull(
                { errorBody, _, _ -> onError(errorBody.string()) },
                { throwable, _ -> onError(throwable.message ?: "null throwable message") }) {
                onUpdated()
            }
    }

    fun updateAll(silent: Boolean = false) {
        measurePerformance("DataService", "updateAll") {
            loadOfflineData { }
            val onSingleItemLoad = { name: String ->
                onSingleItemInUpdateAllLoadedHandler?.invoke(name, 0.5f)
            }
            authRepository.updateUserId {
                onSingleItemLoad(::userIdFlow.name)
                authRepository.updateSessionUser {
                    onSingleItemLoad(::sessionUserFlow.name)
                    updateProfile {
                        onSingleItemLoad(::profile.name)
                        updateEventCalendar {
                            onSingleItemLoad(::eventCalendar.name)
                            onSingleItemLoad(::eventsRange.name)
                        }
                        updateMarksDate { onSingleItemLoad(::marksDateFlow.name) }
                        updateMarksSubject { onSingleItemLoad(::marksSubjectFlow.name) }
                        updateHomeworks { onSingleItemLoad(::homeworksFlow.name) }
                        updateRanking {
                            updateCustomClassMembers {
                                onSingleItemLoad(::classMembers.name)
                            }
                            onSingleItemLoad(::ranking.name)
                        }
                        updateGovExams { onSingleItemLoad(::govExamsFlow.name) }
                        updateSubjectRanking { onSingleItemLoad(::subjectRanking.name) }
                        if (subsystem == Diary.MES) updateVisits { onSingleItemLoad(::visits.name) }
                        if (subsystem == Diary.MES) updateMealBalance { onSingleItemLoad(::mealBalance.name) }
                        updateSchoolInfo { onSingleItemLoad(::schoolInfo.name) }
                        updateAvatars { onSingleItemLoad(::avatarsFlow.name) }
                        updatePersonData { onSingleItemLoad(::personData.name) }
                        if (subsystem == Diary.MES) updateDaysBalanceInfo { onSingleItemLoad(::daysBalanceInfo.name) }
                        if (subsystem == Diary.MES) updateMealsMenuComplexes { onSingleItemLoad(::mealsMenuComplexes.name) }
                    }
                }
            }
        }
    }

    fun loadFromCache(get: (String) -> String) {
        fields.map { it.name }.forEachIndexed { index, name ->
            try {
                when (name) {
                    "userIdFlow" -> {
                        val type = object : TypeToken<ProfilesId>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<ProfilesId>(json, type)
                            _userIdMutable.value = value
                        }
                    }
                    "sessionUserFlow" -> {
                        val type = object : TypeToken<SessionUser>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<SessionUser>(json, type)
                            _sessionUserMutable.value = value
                        }
                    }
                    "profile" -> {
                        val type = object : TypeToken<ProfileResponse>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<ProfileResponse>(json, type)
                            _profileMutable.value = value
                        }
                    }
                    "classMembers" -> {
                        val type = object : TypeToken<List<ClassMember>>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<List<ClassMember>>(json, type)
                            _classMembersMutable.value = value
                        }
                    }
                    "ranking" -> {
                        val type = object : TypeToken<List<RankingMember>>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<List<RankingMember>>(json, type)
                            _rankingMutable.value = value
                        }
                    }
                    "visits" -> {
                        val type = object : TypeToken<VisitsResponse>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<VisitsResponse>(json, type)
                            _visitsMutable.value = value
                        }
                    }
                    "eventsRange" -> {
                        val type = object : TypeToken<List<Long>>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<List<Long>>(json, type)
                            _eventsRangeMutable.value = value
                        }
                    }
                    "mealBalance" -> {
                        val type = object : TypeToken<MealBalance>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<MealBalance>(json, type)
                            _mealBalanceMutable.value = value
                        }
                    }
                    "schoolInfo" -> {
                        val type = object : TypeToken<SchoolInfo>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<SchoolInfo>(json, type)
                            _schoolInfoMutable.value = value
                        }
                    }
                    "personData" -> {
                        val type = object : TypeToken<PersonData>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<PersonData>(json, type)
                            _personDataMutable.value = value
                        }
                    }
                    "daysBalanceInfo" -> {
                        val type = object : TypeToken<DaysBalanceInfo>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<DaysBalanceInfo>(json, type)
                            _daysBalanceInfoMutable.value = value
                        }
                    }
                    "mealsMenuComplexes" -> {
                        val type = object : TypeToken<MealsMenuComplexes>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<MealsMenuComplexes>(json, type)
                            _mealsMenuComplexesMutable.value = value
                        }
                    }
                    "govExamsFlow" -> {
                        val type = object : TypeToken<GovExamsResponse>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<GovExamsResponse>(json, type)
                            _govExamsMutable.value = value
                        }
                    }
                    "marksDateFlow" -> {
                        val type = object : TypeToken<MarkListDate>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<MarkListDate>(json, type)
                            _marksDateMutable.value = value
                        }
                    }
                    "marksSubjectFlow" -> {
                        val type = object : TypeToken<List<MarkListSubjectItem>>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<List<MarkListSubjectItem>>(json, type)
                            _marksSubjectMutable.value = value
                        }
                    }
                    "homeworksFlow" -> {
                        val type = object : TypeToken<List<org.bxkr.octodiary.models.homeworks2.Homework>>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<List<org.bxkr.octodiary.models.homeworks2.Homework>>(json, type)
                            _homeworksMutable.value = value
                        }
                    }
                    "avatarsFlow" -> {
                        val type = object : TypeToken<List<Avatar>>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<List<Avatar>>(json, type)
                            _avatarsMutable.value = value
                        }
                    }
                    "subjectRanking" -> {
                        val type = object : TypeToken<List<SubjectRanking>>() {}.type
                        val json = get(name)
                        if (json != null) {
                            val value = Gson().fromJson<List<SubjectRanking>>(json, type)
                            _subjectRankingMutable.value = value
                        }
                    }
                    else -> {
                        val json = get(name)
                        if (json != null) {
                            javaClass.getDeclaredField(name).let { field ->
                                val type = field.genericType
                                val value = Gson().fromJson<Any?>(json, TypeToken.get(type).type)
                                field.set(this, value)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun Context.loadDemoCache() =
        loadFromCache {
            resources.openRawResource(
                mapOfDemoResourceIds.getValue(
                    it
                )
            ).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
}
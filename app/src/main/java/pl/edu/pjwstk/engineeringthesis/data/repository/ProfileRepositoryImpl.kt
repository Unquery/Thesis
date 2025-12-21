package pl.edu.pjwstk.engineeringthesis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val diaryDB: DiaryDB
) : ProfileRepository {

    private val dao = diaryDB.userProfileDao

    override suspend fun upsert(profile: UserProfile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun insert(profile: UserProfile): Int {
        return dao.insert(profile.toEntity()).toInt()
    }
    override suspend fun getById(id: Int): UserProfile? =
        dao.getById(id)?.toDomain()

    override suspend fun getLatest(): UserProfile? =
        dao.getLatest()?.toDomain()

    override suspend fun getAll(): List<UserProfile> =
        dao.getAll().map { it.toDomain() }

    override fun observeById(id: Int): Flow<UserProfile?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeLatest(): Flow<UserProfile?> =
        dao.observeLatest().map { it?.toDomain() }

    override fun observeAll(): Flow<List<UserProfile>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun existsAny(): Boolean =
        dao.existsAny()

    override suspend fun deleteById(id: Int) {
        dao.deleteById(id)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun setGender(id: Int, gender: String) {
        dao.setGender(id, gender)
    }

    override suspend fun setBirthDateEpochDays(id: Int, birthDateEpochDays: Long) {
        dao.setBirthDateEpochDays(id, birthDateEpochDays)
    }

    override suspend fun setHeightCm(id: Int, heightCm: Int) {
        dao.setHeightCm(id, heightCm)
    }

    override suspend fun getActive(): UserProfile? =
        dao.getActive()?.toDomain()

    override fun observeActive(): Flow<UserProfile?> =
        dao.observeActive().map { it?.toDomain() }

    override suspend fun setActiveProfile(id: Int) {
        dao.setActiveProfile(id)
    }

    override suspend fun insertAndActivate(profile: UserProfile): Int {
        return dao.insertAndActivate(profile.toEntity())
    }

    override suspend fun getNextId(): Int = dao.getNextId()
}

private fun UserProfileEntity.toDomain(): UserProfile =
    UserProfile(
        id = id,
        gender = gender,
        birthDateEpochDays = birthDateEpochDays,
        heightCm = heightCm,
        isActive = isActive
    )

private fun UserProfile.toEntity(): UserProfileEntity =
    UserProfileEntity(
        id = id,
        gender = gender,
        birthDateEpochDays = birthDateEpochDays,
        heightCm = heightCm,
        isActive = isActive
    )

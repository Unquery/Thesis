package pl.edu.pjwstk.engineeringthesis.data.repository

import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity
import pl.edu.pjwstk.engineeringthesis.model.UserProfile
import pl.edu.pjwstk.engineeringthesis.model.toDomain
import pl.edu.pjwstk.engineeringthesis.model.toEntity
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val db: DiaryDB
) : ProfileRepository {

    private val dao get() = db.userProfileDao

    override suspend fun get(): UserProfile? = dao.get()?.toDomain()

    override suspend fun save(profile: UserProfile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun exists(): Boolean = dao.get() != null

    override suspend fun clear() { dao.clear() }
}
package com.vadimtoptunov.devdata.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: IdentityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<IdentityEntity>)

    @Delete
    suspend fun delete(entity: IdentityEntity)

    @Query("DELETE FROM identities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM identities WHERE suiteId = :suiteId")
    suspend fun deleteBySuite(suiteId: String)

    @Query("SELECT * FROM identities ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<IdentityEntity>>

    @Query("SELECT * FROM identities WHERE suiteId IS NULL ORDER BY createdAt DESC")
    fun observeStandalone(): Flow<List<IdentityEntity>>

    @Query("SELECT * FROM identities WHERE suiteId = :suiteId ORDER BY createdAt ASC")
    fun observeBySuite(suiteId: String): Flow<List<IdentityEntity>>

    @Query("SELECT * FROM identities WHERE countryCode = :code ORDER BY createdAt DESC")
    fun observeByCountry(code: String): Flow<List<IdentityEntity>>

    @Query("SELECT * FROM identities WHERE id = :id")
    suspend fun findById(id: String): IdentityEntity?

    /** The identity flagged as active for HCE — there can only be one at a time. */
    @Query("SELECT * FROM identities WHERE id = (SELECT activeIdentityId FROM hce_state LIMIT 1)")
    fun observeActiveForHce(): Flow<IdentityEntity?>
}

@Dao
interface TestSuiteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TestSuiteEntity)

    @Update
    suspend fun update(entity: TestSuiteEntity)

    @Delete
    suspend fun delete(entity: TestSuiteEntity)

    @Query("SELECT * FROM test_suites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TestSuiteEntity>>

    @Query("SELECT * FROM test_suites WHERE id = :id")
    suspend fun findById(id: String): TestSuiteEntity?
}

@Dao
interface RunResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RunResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RunResultEntity>)

    @Update
    suspend fun update(entity: RunResultEntity)

    @Query("SELECT * FROM run_results WHERE suiteId = :suiteId ORDER BY runAt DESC")
    fun observeBySuite(suiteId: String): Flow<List<RunResultEntity>>

    @Query("SELECT * FROM run_results WHERE identityId = :identityId ORDER BY runAt DESC")
    fun observeByIdentity(identityId: String): Flow<List<RunResultEntity>>
}

/**
 * Singleton row that tracks which identity is currently loaded in the HCE service.
 * Using a table with a fixed id = "singleton" avoids SharedPreferences coupling.
 */
@Dao
interface HceStateDao {

    @Query("SELECT activeIdentityId FROM hce_state LIMIT 1")
    fun observeActiveId(): Flow<String?>

    @Query("INSERT OR REPLACE INTO hce_state(id, activeIdentityId) VALUES('singleton', :identityId)")
    suspend fun setActive(identityId: String)

    @Query("UPDATE hce_state SET activeIdentityId = NULL WHERE id = 'singleton'")
    suspend fun clearActive()
}

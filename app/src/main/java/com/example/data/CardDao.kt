package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM user_cards ORDER BY lastUpdated DESC")
    fun getAllCards(): Flow<List<UserCard>>

    @Query("SELECT * FROM user_cards WHERE id = :id")
    suspend fun getCardById(id: Int): UserCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: UserCard): Long

    @Update
    suspend fun updateCard(card: UserCard)

    @Delete
    suspend fun deleteCard(card: UserCard)

    @Query("DELETE FROM user_cards WHERE id = :id")
    suspend fun deleteCardById(id: Int)
}

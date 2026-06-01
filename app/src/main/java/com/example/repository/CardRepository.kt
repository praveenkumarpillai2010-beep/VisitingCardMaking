package com.example.repository

import com.example.data.CardDao
import com.example.data.UserCard
import kotlinx.coroutines.flow.Flow

class CardRepository(private val cardDao: CardDao) {
    val allCards: Flow<List<UserCard>> = cardDao.getAllCards()

    suspend fun getCardById(id: Int): UserCard? {
        return cardDao.getCardById(id)
    }

    suspend fun saveCard(card: UserCard): Long {
        return cardDao.insertCard(card)
    }

    suspend fun updateCard(card: UserCard) {
        cardDao.updateCard(card)
    }

    suspend fun deleteCard(card: UserCard) {
        cardDao.deleteCard(card)
    }

    suspend fun deleteCardById(id: Int) {
        cardDao.deleteCardById(id)
    }
}

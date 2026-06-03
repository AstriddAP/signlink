package com.signlink.di

import com.signlink.data.repository.AlertRepository
import com.signlink.data.repository.AlertRepositoryImpl
import com.signlink.data.repository.AuthRepository
import com.signlink.data.repository.AuthRepositoryImpl
import com.signlink.data.repository.UserRepository
import com.signlink.data.repository.UserRepositoryImpl
import com.signlink.data.repository.SpeechRecognitionRepository
import com.signlink.data.repository.SpeechRecognitionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        alertRepositoryImpl: AlertRepositoryImpl
    ): AlertRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindSpeechRecognitionRepository(
        speechRecognitionRepositoryImpl: SpeechRecognitionRepositoryImpl
    ): SpeechRecognitionRepository
}

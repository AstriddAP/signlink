package com.signlink.di

import android.content.Context
import androidx.room.Room
import com.signlink.BuildConfig
import com.signlink.data.local.AppDatabase
import com.signlink.data.local.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val dbName = "signlink_db_secure"
        
        // SEGURIDAD TOTAL: Lee la clave desde local.properties (vía BuildConfig)
        // Si sale en rojo, recuerda darle al icono del elefante (Sync Now)
        val passphraseString = BuildConfig.DB_PASSPHRASE
        val passphrase = passphraseString.toByteArray()
        
        SQLiteDatabase.loadLibs(context)
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }
}

package com.cleanos.storage

import android.app.Application
import com.cleanos.storage.data.repositories.FileRepository
import com.cleanos.storage.domain.cleaner.DeleteFilesUseCase

/** Manual dependency container - small app, no DI framework needed. */
class CleanOSApp : Application() {
    lateinit var repository: FileRepository
        private set
    lateinit var deleteFiles: DeleteFilesUseCase
        private set
    override fun onCreate() {
        super.onCreate()
        repository = FileRepository(this)
        deleteFiles = DeleteFilesUseCase(this)
    }
}

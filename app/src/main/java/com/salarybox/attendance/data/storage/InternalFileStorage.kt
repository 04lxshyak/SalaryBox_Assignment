package com.salarybox.attendance.data.storage

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Hardened, app-private file storage service.
 * Manages biometric images and attendance selfies strictly within app-internal storage.
 */
class InternalFileStorage(private val context: Context) {

    private val selfiesDir: File
        get() = File(context.filesDir, "selfies").apply { if (!exists()) mkdirs() }

    private val enrollmentsDir: File
        get() = File(context.filesDir, "enrollments").apply { if (!exists()) mkdirs() }

    /**
     * Saves a captured attendance selfie securely into private storage.
     * @return Absolute file path on success.
     */
    suspend fun saveSelfie(bitmap: Bitmap, staffId: Long): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val safeStaffId = staffId.toString().filter { it.isDigit() }
        val fileName = "selfie_${safeStaffId}_${timestamp}.jpg"
        val targetFile = File(selfiesDir, fileName)

        try {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            targetFile.absolutePath
        } catch (e: IOException) {
            if (targetFile.exists()) targetFile.delete()
            throw IOException("Failed to save selfie to private storage: ${e.message}", e)
        }
    }

    /**
     * Saves an enrollment sample image into private storage.
     * @return Absolute file path on success.
     */
    suspend fun saveEnrollmentImage(bitmap: Bitmap, staffId: Long, sampleIndex: Int): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val safeStaffId = staffId.toString().filter { it.isDigit() }
        val fileName = "enrollment_${safeStaffId}_sample_${sampleIndex}_${timestamp}.jpg"
        val targetFile = File(enrollmentsDir, fileName)

        try {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            targetFile.absolutePath
        } catch (e: IOException) {
            if (targetFile.exists()) targetFile.delete()
            throw IOException("Failed to save enrollment image: ${e.message}", e)
        }
    }

    /**
     * Deletes previous enrollment sample images for a staff member (for re-enrollment).
     */
    suspend fun deleteEnrollmentImages(staffId: Long): Int = withContext(Dispatchers.IO) {
        val prefix = "enrollment_${staffId}_"
        var deletedCount = 0
        val files = enrollmentsDir.listFiles() ?: return@withContext 0
        for (file in files) {
            if (file.name.startsWith(prefix)) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        deletedCount
    }

    /**
     * Cleans up orphaned selfie files that are no longer referenced in the database.
     */
    suspend fun cleanupOrphanedSelfies(validSelfiePaths: Set<String>): Int = withContext(Dispatchers.IO) {
        var removedCount = 0
        val files = selfiesDir.listFiles() ?: return@withContext 0
        for (file in files) {
            if (!validSelfiePaths.contains(file.absolutePath)) {
                if (file.delete()) {
                    removedCount++
                }
            }
        }
        removedCount
    }

    /**
     * Retrieves file handle if it exists, or null.
     */
    fun getFile(path: String): File? {
        val file = File(path)
        return if (file.exists() && file.isFile) file else null
    }

    /**
     * Checks if a file exists at the given path.
     */
    fun fileExists(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.isFile
    }

    /**
     * Deletes a specific file safely.
     */
    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }
}

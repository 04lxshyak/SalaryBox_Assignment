package com.salarybox.attendance.core.util

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error<out T>(val message: String, val data: T? = null) : Resource<T>()
    class Loading<out T> : Resource<T>()
}

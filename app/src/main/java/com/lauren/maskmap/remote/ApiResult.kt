package com.lauren.MaskMap.remote

sealed class ApiResult<out T: Any> {
    class Success<out T: Any>(val data: T): ApiResult<T>()
    class Error(val exception: Throwable): ApiResult<Nothing>()
}
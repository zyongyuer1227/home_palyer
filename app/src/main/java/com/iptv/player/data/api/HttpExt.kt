package com.iptv.player.data.api

import retrofit2.Response

fun <T> Response<T>.bodyOrThrow(): T {
    if (isSuccessful) {
        body()?.let { return it }
    }
    throw RuntimeException("HTTP ${code()}")
}

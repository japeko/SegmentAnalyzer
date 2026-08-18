package com.segmentanalyzer.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().cookieJar(InMemoryCookieJar()).build()
}

/** Per-host in-memory cookie storage, enough to carry session cookies through the Garmin SSO login. */
private class InMemoryCookieJar : CookieJar {
    private val cookiesByHost = mutableMapOf<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookiesByHost[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookiesByHost[url.host].orEmpty()
}

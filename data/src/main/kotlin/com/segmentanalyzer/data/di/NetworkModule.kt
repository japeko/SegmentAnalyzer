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

/**
 * Per-host in-memory cookie storage, enough to carry session cookies through the Garmin SSO
 * login. Merges by cookie name rather than replacing the whole host bucket per response — a
 * server only re-sends Set-Cookie for what it's adding/changing, not everything already set, so
 * overwriting the bucket wholesale silently drops earlier cookies (e.g. the session cookie from
 * step 1) on the very next response that happens to set an unrelated one.
 */
private class InMemoryCookieJar : CookieJar {
    private val cookiesByHost = mutableMapOf<String, MutableMap<String, Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val hostCookies = cookiesByHost.getOrPut(url.host) { mutableMapOf() }
        for (cookie in cookies) hostCookies[cookie.name] = cookie
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val hostCookies = cookiesByHost[url.host] ?: return emptyList()
        hostCookies.values.removeAll { it.expiresAt < now }
        return hostCookies.values.toList()
    }
}

package com.iptv.player.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/health")
    suspend fun health(): Response<Health>

    @GET("api/sources")
    suspend fun sources(): Response<List<Source>>

    @GET("api/channels")
    suspend fun channels(
        @Query("grp") grp: String? = null,
        @Query("q") q: String? = null,
        @Query("favorite") favorite: Int? = null,
        @Query("enabled") enabled: Int? = null,
        @Query("sourceId") sourceId: Int? = null,
        @Query("ids") ids: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): Response<ChannelPage>

    @GET("api/channels/groups")
    suspend fun channelGroups(): Response<List<GroupItem>>

    @GET("api/channels/{id}/streaminfo")
    suspend fun streamInfo(@Path("id") id: Int): Response<StreamInfo>

    @GET("api/favorites")
    suspend fun favorites(): Response<List<Channel>>

    @POST("api/favorites")
    suspend fun addFavorite(@Body body: FavoriteBody): Response<OkBody>

    @DELETE("api/favorites/{channelId}")
    suspend fun removeFavorite(@Path("channelId") channelId: Int): Response<Unit>

    @GET("api/groups")
    suspend fun groups(): Response<List<GroupItem>>

    @POST("api/groups")
    suspend fun createGroup(@Body body: GroupNameBody): Response<GroupItem>

    @POST("api/groups/{id}/items")
    suspend fun updateGroupItems(
        @Path("id") id: Int,
        @Body body: GroupItemsBody,
    ): Response<OkBody>

    @DELETE("api/groups/{id}")
    suspend fun deleteGroup(@Path("id") id: Int): Response<Unit>

    @GET("api/epg/{channelId}")
    suspend fun epg(@Path("channelId") channelId: Int): Response<Epg>

    @GET("api/nas/{sourceId}/browse")
    suspend fun nasBrowse(
        @Path("sourceId") sourceId: Int,
        @Query("dir") dir: String? = null,
    ): Response<NasBrowse>

    @GET("api/nas/{sourceId}/streaminfo")
    suspend fun nasStreamInfo(
        @Path("sourceId") sourceId: Int,
        @Query("path") path: String,
    ): Response<NasStreamInfo>

    @GET("api/settings")
    suspend fun settings(): Response<Map<String, Any>>
}

@kotlinx.serialization.Serializable
data class FavoriteBody(val channelId: Int)

@kotlinx.serialization.Serializable
data class GroupNameBody(val name: String)

@kotlinx.serialization.Serializable
data class GroupItemsBody(val channelIds: List<Int>)

@kotlinx.serialization.Serializable
data class OkBody(val ok: Boolean = false)

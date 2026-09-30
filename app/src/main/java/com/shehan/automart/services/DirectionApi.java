package com.shehan.automart.services;



import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface DirectionApi {
    @GET("json")
    Call<JsonObject> getJson(@Query("origin") String origin,
                             @Query("destination") String destination,
                             @Query("key") String key,
                             @Header("X-Android-Package") String packageName,
                             @Header("X-Android-Cert") String certFingerprint);
}

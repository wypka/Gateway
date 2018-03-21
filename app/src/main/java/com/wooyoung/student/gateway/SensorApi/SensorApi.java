package com.wooyoung.student.gateway.SensorApi;

import com.wooyoung.student.gateway.Model.Sensor;

import java.io.File;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Created by student on 2017-12-07.
 */

public interface SensorApi {
    @POST("sensor/")
    Call<Boolean> insert(@Body Sensor sensor);

    /*@Multipart
    @POST("sensor/")
    //@POST("board/upload")
    Call<ResponseBody> upload(
            @Part MultipartBody.Part file);*/

    @Multipart
    @POST("sensor/upload")
        //@POST("board/upload")
    Call<ResponseBody> upload(
            @Part MultipartBody.Part file);



    public static final Retrofit retrofit = new Retrofit.Builder()
            //.baseUrl("http://70.12.112.146:8888/ex/api/") //내꺼
            //.baseUrl("http://70.12.112.131:8888/fire/api/") //건현이형
            .baseUrl("http://70.12.112.158:8888/fire/api/") // 상민이형
            .addConverterFactory(GsonConverterFactory.create())
            .build();


}

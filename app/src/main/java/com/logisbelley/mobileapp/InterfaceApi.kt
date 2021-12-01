package com.logisbelley.mobileapp

import android.content.Context
import android.util.Log
import com.juvis.diet.android.anewproject.RetrofitCreator
import io.reactivex.Observable
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.*

/**
 * API 정의
 */
class InterfaceApi {

    interface InterfaceApiImpl {

        /**
         * gps 전송
         */
        @GET("/saveGps.dt?")
        fun sendLocation(
            @Query(KeyInfo.KEY_PARAM) param: String,
            @Query(KeyInfo.KEY_LATT) latt: String,
            @Query(KeyInfo.KEY_LNGT) lngt: String,
            @Query(KeyInfo.KEY_GPS_DT) gpsDt: String
        ): Observable<LocationResponseModel>

        /**
         * 상하 검수 저장
         */
        @FormUrlEncoded
        @POST("/saveLoad.dt?")
        fun saveLoad(
            @Field("save_data") save_data: String
        ): Observable<BaseResponseModel>

        /**
         * 하차 검수 저장
         */
        @FormUrlEncoded
        @POST("/saveUnload.dt?")
        fun saveUnload(
            @Field("save_data") save_data: String
        ): Observable<BaseResponseModel>

        /**
         * 인수스캔 저장
         */
        @FormUrlEncoded
        @POST("/saveTake.dt?")
        fun saveTake(
            @Field("save_data") save_data: String
        ): Observable<BaseResponseModel>

        /**
         * 인계스캔 저장
         */
        @FormUrlEncoded
        @POST("/saveTrans.dt?")
        fun saveTrans(
            @Field("save_data") save_data: String
        ): Observable<BaseResponseModel>

        /**
         * 상하 검수리스트 조회
         */
        @GET("/getLoadList.dt?")
        fun getLoadList(
            @Query(KeyInfo.KEY_PARAM) param: String
        ): Observable<GetLoadList>

        /**
         * 인수 검수리스트 조회
         */
        @GET("/getTakeList.dt?")
        fun getTakeList(
            @Query(KeyInfo.KEY_PARAM) param: String
        ): Observable<GetTakeList>

        /**
         * 인계스캔 리스트 조회
         */
        @GET("/getTransList.dt?")
        fun getTransList(
            @Query(KeyInfo.KEY_PARAM) param: String
        ): Observable<GetTransList>

        /**
         * 하차 검수리스트 조회
         */
        @GET("/getUnloadList.dt?")
        fun getUnloadList(
            @Query(KeyInfo.KEY_PARAM) param: String
        ): Observable<GetLoadList>
    }

    companion object {
        /**
         * gps 전송
         */
        fun sendLocation(
            context: Context, hashMap: HashMap<String, String>
        ): Observable<LocationResponseModel> {

            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).sendLocation(
                hashMap[KeyInfo.KEY_PARAM].toString(),
                hashMap[KeyInfo.KEY_LATT].toString(),
                hashMap[KeyInfo.KEY_LNGT].toString(),
                hashMap[KeyInfo.KEY_GPS_DT].toString()
            )
        }

        /**
         * 상차 검수 저장
         */
        fun saveLoad(
            context: Context, save_data: JSONArray
        ): Observable<BaseResponseModel> {

            val json = JSONObject()
            json.put("data", save_data)

            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).saveLoad(
                json.toString()
            )
        }

        /**
         * 하차 검수 저장
         */
        fun saveUnload(
            context: Context, save_data: JSONArray
        ): Observable<BaseResponseModel> {

            val json = JSONObject()
            json.put("data", save_data)

            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).saveUnload(
                json.toString()
            )
        }

        /**
         * 인계스캔 저장
         */
        fun saveTrans(
            context: Context, save_data: JSONArray
        ): Observable<BaseResponseModel> {

            val json = JSONObject()
            json.put("data", save_data)
            Log.e("SNsaveTrans",save_data.toString())
            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).saveTrans(
                json.toString()
            )
        }

        /**
         * 인수스캔 저장
         */
        fun saveTake(
            context: Context, save_data: JSONArray
        ): Observable<BaseResponseModel> {

            val json = JSONObject()
            json.put("data", save_data)
            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).saveTake(
                json.toString()
            )
        }

        /**
         * 상하 검수리스트 조회
         */
        fun getLoadList(
            context: Context, param: String
        ): Observable<GetLoadList> {

            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).getLoadList(param)
        }

        /**
         * 인수 검수리스트 조회
         */
        fun getTakeList(
            context: Context, param: String
        ): Observable<GetTakeList> {

            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).getTakeList(param)
        }

        /**
         * 인계스캔 리스트 조회
         */
        fun getTransList(
            context: Context, param: String
        ): Observable<GetTransList> {

            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).getTransList(param)
        }

        /**
         * 하차 검수리스트 조회
         */
        fun getUnloadList(
            context: Context, param: String
        ): Observable<GetLoadList> {

            return RetrofitCreator.create(
                InterfaceApiImpl::class.java,
                context
            ).getUnloadList(param)
        }


    }
}
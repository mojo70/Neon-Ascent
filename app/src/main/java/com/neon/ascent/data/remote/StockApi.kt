package com.neon.ascent.data.remote

import com.neon.ascent.model.EarningsResponse
import com.neon.ascent.model.StockCandleResponse
import com.neon.ascent.model.StockFinancials
import com.neon.ascent.model.StockQuote
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApi {
    @GET("stock/candle")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") token: String
    ): StockCandleResponse

    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): StockQuote

    @GET("stock/metric")
    suspend fun getFinancials(
        @Query("symbol") symbol: String,
        @Query("metric") metric: String = "all",
        @Query("token") token: String
    ): StockFinancials

    @GET("calendar/earnings")
    suspend fun getEarnings(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): EarningsResponse
}

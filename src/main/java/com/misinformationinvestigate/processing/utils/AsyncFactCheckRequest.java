package com.misinformationinvestigate.processing.utils;

import com.google.gson.Gson;
import com.misinformationinvestigate.processing.models.*;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class AsyncFactCheckRequest extends RichAsyncFunction<LiveStreamPrediction, LiveStreamFactChecked> {

    private AsyncHttpClient httpClient;
    final static Gson gson = new Gson();
    private final Properties properties;

    public AsyncFactCheckRequest(Properties properties){
        this.properties = properties;
    }

    @Override
    public void asyncInvoke(LiveStreamPrediction liveStreamPrediction, ResultFuture<LiveStreamFactChecked> resultFuture) throws Exception {
        Param param = new Param("news_to_check", liveStreamPrediction.getAudioData());
        Request request = new RequestBuilder(HttpConstants.Methods.GET)
                .setRequestTimeout(600000)
                .setUrl(properties.getProperty("FACT_CHECK_API_URL") + "/api/v1/fact-check")
                .setQueryParams(Collections.singletonList(param))
                .build();
        ListenableFuture<Response> result = httpClient.executeRequest(request);
        result.toCompletableFuture()
                .thenAccept((Response response) -> {
                    if(response.getStatusCode() == 200){
                        FactCheckResponse factCheckResponse = gson.fromJson(
                                response.getResponseBody(), FactCheckResponse.class);
                        resultFuture.complete(
                                Collections
                                        .singleton(LiveStreamFactChecked.builder()
                                                .source(liveStreamPrediction.getSource())
                                                .audioData(liveStreamPrediction.getAudioData())
                                                .sessionId(liveStreamPrediction.getSessionId())
                                                .isFakeNews(factCheckResponse.isFakeNews())
                                                .factSource(factCheckResponse.getFactSource())
                                                .build())
                        );
                    }else{
                        resultFuture.completeExceptionally(
                                new Exception("Invalid request")
                        );
                    }
                }).exceptionally(ex -> {
                    resultFuture.completeExceptionally(ex);
                    return null;
                });
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        httpClient = Dsl.asyncHttpClient();
    }

    @Override
    public void close() throws  Exception {
        httpClient.close();
    }
}

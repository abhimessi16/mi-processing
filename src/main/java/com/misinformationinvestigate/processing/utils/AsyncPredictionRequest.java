package com.misinformationinvestigate.processing.utils;

import com.google.gson.Gson;
import com.misinformationinvestigate.processing.models.LiveStreamPrediction;
import com.misinformationinvestigate.processing.models.LiveStreamText;
import com.misinformationinvestigate.processing.models.PredictionInput;
import com.misinformationinvestigate.processing.models.STTInput;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;

import java.util.Collections;
import java.util.Properties;

public class AsyncPredictionRequest extends RichAsyncFunction<LiveStreamText, LiveStreamPrediction> {

    private AsyncHttpClient httpClient;
    final static Gson gson = new Gson();
    private final Properties properties;

    public AsyncPredictionRequest(Properties properties){
        this.properties = properties;
    }

    @Override
    public void asyncInvoke(LiveStreamText liveStreamText, ResultFuture<LiveStreamPrediction> resultFuture) throws Exception {
        Request request = new RequestBuilder(HttpConstants.Methods.POST)
                .setRequestTimeout(600000)
                .setUrl(properties.getProperty("PREDICTION_API_URL") + "/api/v1/fake-news/predict")
                .setBody(gson.toJson(new PredictionInput(liveStreamText.getAudioData())))
                .build();
        ListenableFuture<Response> result = httpClient.executeRequest(request);
        result.toCompletableFuture()
                .thenAccept((Response response) -> {
                    if(response.getStatusCode() == 200){
                        boolean isFakeNews = response.getResponseBody().contains("true");
                        resultFuture.complete(
                                Collections
                                        .singleton(LiveStreamPrediction.builder()
                                                .source(liveStreamText.getSource())
                                                .audioData(liveStreamText.getAudioData())
                                                .sessionId(liveStreamText.getSessionId())
                                                .isFakeNews(isFakeNews)
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

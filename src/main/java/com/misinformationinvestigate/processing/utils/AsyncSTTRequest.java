package com.misinformationinvestigate.processing.utils;

import com.google.gson.Gson;
import com.misinformationinvestigate.processing.models.LiveStreamFloat;
import com.misinformationinvestigate.processing.models.LiveStreamText;
import com.misinformationinvestigate.processing.models.STTInput;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;

import java.util.Collections;
import java.util.Properties;

public class AsyncSTTRequest extends RichAsyncFunction<LiveStreamFloat, LiveStreamText> {

    private AsyncHttpClient httpClient;
    final static Gson gson = new Gson();
    private final Properties properties;

    public AsyncSTTRequest(Properties properties){
        this.properties = properties;
    }

    @Override
    public void asyncInvoke(LiveStreamFloat liveStreamFloat, ResultFuture<LiveStreamText> resultFuture) throws Exception {
        Request request = new RequestBuilder(HttpConstants.Methods.POST)
                .setRequestTimeout(600000)
                .setUrl(properties.getProperty("STT_API_URL") + "/api/v1/stt")
                .setBody(gson.toJson(new STTInput(liveStreamFloat.getAudioData()), STTInput.class))
                .build();
        ListenableFuture<Response> result = httpClient.executeRequest(request);
        result.toCompletableFuture()
                .thenAccept((Response response) -> {
                    if(response.getStatusCode() == 200){
                        resultFuture.complete(Collections
                                .singleton(LiveStreamText.builder()
                                        .source(liveStreamFloat.getSource())
                                        .sessionId(liveStreamFloat.getSessionId())
                                        .audioData(response.getResponseBody())
                                        .build()));
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
    public void open(Configuration parameters) throws Exception {
        httpClient = Dsl.asyncHttpClient();
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }
}

package com.misinformationinvestigate.processing.utils;

import com.google.gson.Gson;
import com.misinformationinvestigate.processing.models.LiveStreamFactChecked;
import com.misinformationinvestigate.processing.models.LiveStreamPrediction;
import com.misinformationinvestigate.processing.models.SSEInput;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;

import java.util.Properties;

public class AsyncSSEFactCheckRequest extends RichAsyncFunction<LiveStreamFactChecked, LiveStreamFactChecked> {

    private AsyncHttpClient httpClient;
    final static Gson gson = new Gson();
    private final Properties properties;

    public AsyncSSEFactCheckRequest(Properties properties){
        this.properties = properties;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        httpClient = Dsl.asyncHttpClient();
    }

    @Override
    public void asyncInvoke(LiveStreamFactChecked liveStreamFactChecked, ResultFuture<LiveStreamFactChecked> resultFuture) throws Exception {
        Request request = new RequestBuilder(HttpConstants.Methods.POST)
                .setRequestTimeout(600000)
                .setUrl(properties.getProperty("LIVE_FEED_API_URL") + "/v1/api/fact-check-events/emit")
                .setBody(gson.toJson(SSEInput.builder()
                                .source(liveStreamFactChecked.getSource())
                                .sessionId(liveStreamFactChecked.getSessionId())
                                .audioData(liveStreamFactChecked.getAudioData())
                                .factSource(liveStreamFactChecked.getFactSource())
                        .build()))
                .build();
        ListenableFuture<Response> listenableFuture = httpClient.executeRequest(request);
        listenableFuture.toCompletableFuture()
                .thenAccept((Response response) -> {
                    if(response.getStatusCode() != 200){
                        resultFuture.completeExceptionally(new Exception(("Invalid request")));
                    }
                }).exceptionally(ex -> {
                    resultFuture.completeExceptionally(ex);
                    return null;
                });
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }
}

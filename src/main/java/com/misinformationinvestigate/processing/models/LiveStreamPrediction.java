package com.misinformationinvestigate.processing.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LiveStreamPrediction {
    private String source;
    @SerializedName(value = "session_id")
    private String sessionId;
    @SerializedName(value = "audio_data")
    private String audioData;
    @SerializedName(value = "is_fake_news")
    private boolean isFakeNews;

    public String toString(){
        return this.source + ":" + this.isFakeNews + ":" + this.audioData;
    }
}

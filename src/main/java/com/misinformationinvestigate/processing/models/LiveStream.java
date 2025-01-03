package com.misinformationinvestigate.processing.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LiveStream {

    private String source;
    @SerializedName(value = "audio_data")
    private short[] audioData;
    @SerializedName(value = "session_id")
    private String sessionId;

    public String toString(){
        return this.source + " : " + this.audioData.length + " : " + this.sessionId;
    }
}
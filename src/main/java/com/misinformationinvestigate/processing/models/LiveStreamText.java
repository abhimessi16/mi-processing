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
public class LiveStreamText {
    private String source;
    private String audioData;
    private String sessionId;

    public String toString(){
        return this.source + " : " + this.audioData + " : " + this.sessionId;
    }
}

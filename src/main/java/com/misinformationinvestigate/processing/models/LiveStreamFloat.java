package com.misinformationinvestigate.processing.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LiveStreamFloat {
    private String source;
    private float[] audioData;
    private String sessionId;

    public String toString(){
        return this.source + " : " + this.audioData.length + " : " + this.sessionId;
    }
}

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
public class SSEInput {
    private String source;
    @SerializedName(value = "session_id")
    private String sessionId;
    @SerializedName(value = "audio_data")
    private String audioData;
    @SerializedName(value = "fact_source")
    private String factSource;

    public String toString(){
        return this.source + ":" + this.factSource + ":" + this.audioData;
    }
}

package com.misinformationinvestigate.processing.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionInput {
    @SerializedName(value = "fake_news")
    private String fakeNews;
}

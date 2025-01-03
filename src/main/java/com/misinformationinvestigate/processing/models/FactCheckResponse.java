package com.misinformationinvestigate.processing.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FactCheckResponse {
    @SerializedName(value = "is_fake_news")
    private boolean isFakeNews;
    @SerializedName(value = "fact_source")
    private String factSource;
}

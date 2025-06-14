package com.hospital.telemedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrugInteractionResponse {
    private boolean hasInteractions;
    private String message;
    private List<InteractionDetail> interactions;
    private List<AlternativeDrug> alternatives;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InteractionDetail {
        private String drug1Name;
        private String drug1Rxcui;
        private String drug2Name;
        private String drug2Rxcui;
        private String description;
        private String severity; // high, moderate, low
        private String recommendation;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AlternativeDrug {
        private String rxcui;
        private String name;
        private String originalRxcui;
        private String reason;
        private String therapeuticClass;
    }
}
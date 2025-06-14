package com.hospital.telemedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmartPrescriptionResponse {
    private boolean success;
    private String message;
    private List<PrescriptionWarning> warnings;
    private List<DrugSuggestionResponse> suggestions;
    private List<GenericDrugResponse> genericAlternatives;
    private DrugInteractionResponse interactionCheck;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrescriptionWarning {
        private String type; // INTERACTION, ALLERGY, CONTRAINDICATION
        private String severity; // HIGH, MODERATE, LOW
        private String message;
        private String drugName;
        private String recommendation;
    }
}
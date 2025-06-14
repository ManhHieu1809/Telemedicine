package com.hospital.telemedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrugSuggestionResponse {
    private String rxcui;
    private String name;
    private String synonym;
    private String tty; // Term Type
    private String indication; // Chỉ định sử dụng
    private String dosageForm;
    private String strength;
    private String route;
    private boolean isGeneric;
    private String description;
}
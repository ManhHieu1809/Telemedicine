package com.hospital.telemedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericDrugResponse {
    private String rxcui;
    private String name;
    private String tty;
    private boolean isGeneric;
    private String brandName;
    private String estimatedSavings; // Ước tính tiết kiệm
    private String availability; // Tình trạng có sẵn
    private String manufacturer;
}

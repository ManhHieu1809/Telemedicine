package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.MedicalRecordRequest;
import com.hospital.telemedicine.dto.request.PrescriptionDetailRequest;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmartPrescriptionService {

    @Autowired
    private DrugInteractionService drugInteractionService;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    /**
     * Phân tích đơn thuốc thông minh trước khi lưu
     */
    public SmartPrescriptionResponse analyzePrescription(MedicalRecordRequest request) {
        try {
            SmartPrescriptionResponse response = new SmartPrescriptionResponse();
            List<SmartPrescriptionResponse.PrescriptionWarning> warnings = new ArrayList<>();
            List<DrugSuggestionResponse> suggestions = new ArrayList<>();
            List<GenericDrugResponse> genericAlternatives = new ArrayList<>();

            // 1. Lấy danh sách thuốc từ request
            List<String> drugNames = request.getPrescriptionDetails().stream()
                    .map(PrescriptionDetailRequest::getMedicationName)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.trim().isEmpty())
                    .collect(Collectors.toList());

            if (drugNames.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("Không có thuốc nào trong đơn để phân tích");
                return response;
            }

            log.info("Analyzing prescription with {} drugs: {}", drugNames.size(), drugNames);

            // 2. Tìm RXCUI cho các thuốc (để sử dụng cho generic alternatives)
            Map<String, String> drugRxcuiMap = new HashMap<>();
            List<String> validDrugNames = new ArrayList<>();

            for (String drugName : drugNames) {
                List<DrugSuggestionResponse> searchResults = drugInteractionService.searchDrugs(drugName);
                if (!searchResults.isEmpty()) {
                    String rxcui = searchResults.get(0).getRxcui();
                    drugRxcuiMap.put(drugName, rxcui);
                    validDrugNames.add(drugName);
                    log.debug("Found RXCUI {} for drug {}", rxcui, drugName);
                } else {
                    // Cảnh báo thuốc không tìm thấy trong database
                    warnings.add(new SmartPrescriptionResponse.PrescriptionWarning(
                            "NOT_FOUND", "MODERATE",
                            "Không tìm thấy thông tin thuốc trong cơ sở dữ liệu: " + drugName,
                            drugName,
                            "Vui lòng kiểm tra lại tên thuốc hoặc nhập tên khác"
                    ));
                    // Vẫn thêm vào danh sách để kiểm tra tương tác
                    validDrugNames.add(drugName);
                    log.warn("Drug not found in RxNav database: {}", drugName);
                }
            }

            // 3. Kiểm tra tương tác thuốc sử dụng DDInter API
            DrugInteractionResponse interactionCheck = null;
            if (validDrugNames.size() >= 2) {
                log.info("Checking drug interactions for {} drugs using DDInter", validDrugNames.size());
                interactionCheck = drugInteractionService.checkDrugInteractions(validDrugNames);

                if (interactionCheck.isHasInteractions()) {
                    log.info("Found {} drug interactions", interactionCheck.getInteractions().size());

                    for (DrugInteractionResponse.InteractionDetail interaction : interactionCheck.getInteractions()) {
                        String severity = mapSeverityLevel(interaction.getSeverity());
                        warnings.add(new SmartPrescriptionResponse.PrescriptionWarning(
                                "INTERACTION", severity,
                                String.format("Tương tác giữa %s và %s: %s",
                                        interaction.getDrug1Name(), interaction.getDrug2Name(), interaction.getDescription()),
                                interaction.getDrug1Name() + " + " + interaction.getDrug2Name(),
                                interaction.getRecommendation() != null ?
                                        interaction.getRecommendation() :
                                        generateInteractionRecommendation(interaction)
                        ));
                    }
                } else {
                    log.info("No drug interactions found");
                }
            } else {
                log.info("Less than 2 drugs, skipping interaction check");
            }

            // 4. Tìm thuốc generic thay thế (sử dụng RxNav)
            for (Map.Entry<String, String> entry : drugRxcuiMap.entrySet()) {
                try {
                    List<GenericDrugResponse> generics = drugInteractionService.findGenericAlternatives(entry.getValue());
                    if (!generics.isEmpty()) {
                        log.debug("Found {} generic alternatives for {}", generics.size(), entry.getKey());
                        genericAlternatives.addAll(generics);
                    }
                } catch (Exception e) {
                    log.warn("Error finding generic alternatives for {}: {}", entry.getKey(), e.getMessage());
                }
            }

            // 5. Gợi ý thuốc theo chẩn đoán
            if (request.getDiagnosis() != null && !request.getDiagnosis().trim().isEmpty()) {
                try {
                    List<DrugSuggestionResponse> diagnosisSuggestions =
                            drugInteractionService.suggestDrugsByDiagnosis(request.getDiagnosis());
                    suggestions.addAll(diagnosisSuggestions);
                    log.debug("Found {} drug suggestions for diagnosis: {}",
                            diagnosisSuggestions.size(), request.getDiagnosis());
                } catch (Exception e) {
                    log.warn("Error getting drug suggestions for diagnosis {}: {}",
                            request.getDiagnosis(), e.getMessage());
                }
            }

            // 6. Kiểm tra lịch sử dị ứng thuốc của bệnh nhân
            try {
                warnings.addAll(checkPatientAllergies(request.getPatientId(), validDrugNames));
            } catch (Exception e) {
                log.warn("Error checking patient allergies: {}", e.getMessage());
            }

            // 7. Kiểm tra chống chỉ định dựa trên tiền sử bệnh
            try {
                warnings.addAll(checkContraindications(request.getPatientId(), validDrugNames, request.getDiagnosis()));
            } catch (Exception e) {
                log.warn("Error checking contraindications: {}", e.getMessage());
            }

            // 8. Tạo response
            response.setSuccess(true);
            response.setMessage(generateAnalysisMessage(warnings, genericAlternatives.size()));
            response.setWarnings(warnings);
            response.setSuggestions(suggestions);
            response.setGenericAlternatives(genericAlternatives);
            response.setInteractionCheck(interactionCheck);

            log.info("Prescription analysis completed. Warnings: {}, Suggestions: {}, Generics: {}",
                    warnings.size(), suggestions.size(), genericAlternatives.size());

            return response;

        } catch (Exception e) {
            log.error("Error analyzing prescription: ", e);
            SmartPrescriptionResponse errorResponse = new SmartPrescriptionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Lỗi khi phân tích đơn thuốc: " + e.getMessage());
            return errorResponse;
        }
    }


    /**
     * Gợi ý thuốc theo chẩn đoán
     */
    public List<DrugSuggestionResponse> suggestDrugsForDiagnosis(String diagnosis) {
        return drugInteractionService.suggestDrugsByDiagnosis(diagnosis);
    }

    /**
     * Tìm thuốc generic
     */
    public List<GenericDrugResponse> findGenericAlternatives(String drugName) {
        List<DrugSuggestionResponse> searchResults = drugInteractionService.searchDrugs(drugName);
        if (!searchResults.isEmpty()) {
            return drugInteractionService.findGenericAlternatives(searchResults.get(0).getRxcui());
        }
        return Collections.emptyList();
    }

    /**
     * Kiểm tra tương tác thuốc
     */
    public DrugInteractionResponse checkInteractions(List<String> drugNames) {
        if (drugNames == null || drugNames.size() < 2) {
            return new DrugInteractionResponse(false, "Cần ít nhất 2 loại thuốc để kiểm tra tương tác",
                    Collections.emptyList(), Collections.emptyList());
        }

        // Lọc và làm sạch tên thuốc
        List<String> cleanDrugNames = drugNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toList());

        if (cleanDrugNames.size() < 2) {
            return new DrugInteractionResponse(false, "Cần ít nhất 2 loại thuốc hợp lệ để kiểm tra tương tác",
                    Collections.emptyList(), Collections.emptyList());
        }

        log.info("Checking interactions for drugs: {}", cleanDrugNames);
        return drugInteractionService.checkDrugInteractions(cleanDrugNames);
    }

    /**
     * Tìm kiếm thuốc
     */
    public List<DrugSuggestionResponse> searchDrugs(String query) {
        return drugInteractionService.searchDrugs(query);
    }

    /**
     * Kiểm tra dị ứng thuốc của bệnh nhân
     */
    private List<SmartPrescriptionResponse.PrescriptionWarning> checkPatientAllergies(Long patientId, List<String> drugNames) {
        List<SmartPrescriptionResponse.PrescriptionWarning> warnings = new ArrayList<>();

        try {
            // Lấy lịch sử dị ứng từ các đơn thuốc trước
            List<MedicalRecord> previousRecords = medicalRecordRepository.findByPatientId(patientId);
            Set<String> allergyKeywords = new HashSet<>();

            // Thu thập từ khóa dị ứng từ ghi chú trong các đơn thuốc cũ
            for (MedicalRecord record : previousRecords) {
                if (record.getPrescription() != null && record.getPrescription().getNotes() != null) {
                    String notes = record.getPrescription().getNotes().toLowerCase();
                    if (notes.contains("dị ứng") || notes.contains("allergy") || notes.contains("phản ứng")) {
                        // Trích xuất tên thuốc gây dị ứng (đơn giản)
                        for (String drugName : drugNames) {
                            if (notes.contains(drugName.toLowerCase())) {
                                allergyKeywords.add(drugName.toLowerCase());
                            }
                        }
                    }
                }
            }

            // Tạo cảnh báo cho thuốc có thể gây dị ứng
            for (String drugName : drugNames) {
                if (allergyKeywords.contains(drugName.toLowerCase())) {
                    warnings.add(new SmartPrescriptionResponse.PrescriptionWarning(
                            "ALLERGY", "HIGH",
                            "Bệnh nhân có thể dị ứng với thuốc: " + drugName,
                            drugName,
                            "Kiểm tra kỹ lịch sử dị ứng và cân nhắc thay thuốc khác"
                    ));
                }
            }

        } catch (Exception e) {
            log.error("Error checking patient allergies: ", e);
        }

        return warnings;
    }

    /**
     * Kiểm tra chống chỉ định
     */
    private List<SmartPrescriptionResponse.PrescriptionWarning> checkContraindications(Long patientId, List<String> drugNames, String diagnosis) {
        List<SmartPrescriptionResponse.PrescriptionWarning> warnings = new ArrayList<>();

        try {
            // Danh sách chống chỉ định phổ biến
            Map<String, List<String>> contraindications = getCommonContraindications();

            // Lấy tiền sử bệnh từ chẩn đoán trước
            List<MedicalRecord> previousRecords = medicalRecordRepository.findByPatientId(patientId);
            Set<String> patientConditions = new HashSet<>();

            // Thu thập các bệnh lý từ chẩn đoán trước
            for (MedicalRecord record : previousRecords) {
                if (record.getDiagnosis() != null) {
                    patientConditions.add(record.getDiagnosis().toLowerCase());
                }
            }

            // Thêm chẩn đoán hiện tại
            if (diagnosis != null) {
                patientConditions.add(diagnosis.toLowerCase());
            }

            // Kiểm tra chống chỉ định
            for (String drugName : drugNames) {
                String drugLower = drugName.toLowerCase();

                for (Map.Entry<String, List<String>> entry : contraindications.entrySet()) {
                    String condition = entry.getKey();
                    List<String> contraindicatedDrugs = entry.getValue();

                    if (patientConditions.stream().anyMatch(pc -> pc.contains(condition)) &&
                            contraindicatedDrugs.stream().anyMatch(cd -> drugLower.contains(cd))) {

                        warnings.add(new SmartPrescriptionResponse.PrescriptionWarning(
                                "CONTRAINDICATION", "HIGH",
                                String.format("Thuốc %s có thể chống chỉ định với bệnh lý %s", drugName, condition),
                                drugName,
                                "Cân nhắc thay thuốc khác hoặc điều chỉnh liều lượng"
                        ));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error checking contraindications: ", e);
        }

        return warnings;
    }

    /**
     * Lấy danh sách chống chỉ định phổ biến
     */
    private Map<String, List<String>> getCommonContraindications() {
        Map<String, List<String>> contraindications = new HashMap<>();

        contraindications.put("thận", Arrays.asList("nsaid", "ibuprofen", "aspirin", "diclofenac"));
        contraindications.put("gan", Arrays.asList("paracetamol", "acetaminophen"));
        contraindications.put("tim", Arrays.asList("nsaid", "ibuprofen", "celecoxib"));
        contraindications.put("dạ dày", Arrays.asList("aspirin", "nsaid", "corticosteroid"));
        contraindications.put("hen", Arrays.asList("aspirin", "beta-blocker"));
        contraindications.put("đái tháo đường", Arrays.asList("corticosteroid", "thiazide"));
        contraindications.put("cao huyết áp", Arrays.asList("nsaid", "pseudoephedrine"));

        return contraindications;
    }

    /**
     * Ánh xạ mức độ nghiêm trọng
     */
    private String mapSeverityLevel(String originalSeverity) {
        if (originalSeverity == null) return "MODERATE";

        switch (originalSeverity.toLowerCase()) {
            case "high":
            case "major":
            case "contraindicated":
                return "HIGH";
            case "moderate":
                return "MODERATE";
            case "low":
            case "minor":
                return "LOW";
            default:
                return "MODERATE";
        }
    }

    /**
     * Tạo khuyến nghị cho tương tác thuốc
     */
    private String generateInteractionRecommendation(DrugInteractionResponse.InteractionDetail interaction) {
        String severity = interaction.getSeverity();

        if ("high".equalsIgnoreCase(severity) || "contraindicated".equalsIgnoreCase(severity)) {
            return "Tránh sử dụng đồng thời. Cân nhắc thay thuốc khác.";
        } else if ("moderate".equalsIgnoreCase(severity)) {
            return "Theo dõi chặt chẽ bệnh nhân. Có thể cần điều chỉnh liều.";
        } else {
            return "Theo dõi tác dụng phụ. Thông báo cho bệnh nhân.";
        }
    }

    /**
     * Tạo thông điệp tổng kết phân tích
     */
    private String generateAnalysisMessage(List<SmartPrescriptionResponse.PrescriptionWarning> warnings, int genericCount) {
        StringBuilder message = new StringBuilder("Phân tích đơn thuốc hoàn tất. ");

        long highRiskWarnings = warnings.stream().filter(w -> "HIGH".equals(w.getSeverity())).count();
        long moderateRiskWarnings = warnings.stream().filter(w -> "MODERATE".equals(w.getSeverity())).count();

        if (highRiskWarnings > 0) {
            message.append(String.format("Phát hiện %d cảnh báo nghiêm trọng. ", highRiskWarnings));
        }

        if (moderateRiskWarnings > 0) {
            message.append(String.format("Có %d cảnh báo cần lưu ý. ", moderateRiskWarnings));
        }

        if (genericCount > 0) {
            message.append(String.format("Tìm thấy %d thuốc generic thay thế. ", genericCount));
        }

        if (warnings.isEmpty()) {
            message.append("Không phát hiện vấn đề đáng lo ngại.");
        }

        return message.toString();
    }

    /**
     * Lấy thông tin chi tiết thuốc
     */
    public DrugSuggestionResponse getDrugDetails(String drugName) {
        List<DrugSuggestionResponse> searchResults = drugInteractionService.searchDrugs(drugName);
        if (!searchResults.isEmpty()) {
            String rxcui = searchResults.get(0).getRxcui();
            return drugInteractionService.getDrugDetails(rxcui);
        }
        return null;
    }

    /**
     * Phân tích đơn thuốc của bệnh nhân (dựa trên lịch sử)
     */
    public SmartPrescriptionResponse analyzePatientMedicationHistory(Long patientId) {
        try {
            List<MedicalRecord> records = medicalRecordRepository.findByPatientId(patientId);
            List<String> allDrugs = new ArrayList<>();

            // Thu thập tất cả thuốc từ lịch sử
            for (MedicalRecord record : records) {
                if (record.getPrescription() != null && record.getPrescription().getDetails() != null) {
                    for (PrescriptionDetail detail : record.getPrescription().getDetails()) {
                        allDrugs.add(detail.getMedicationName());
                    }
                }
            }

            if (allDrugs.isEmpty()) {
                SmartPrescriptionResponse response = new SmartPrescriptionResponse();
                response.setSuccess(true);
                response.setMessage("Bệnh nhân chưa có lịch sử dùng thuốc");
                response.setWarnings(Collections.emptyList());
                return response;
            }

            // Tạo request giả để phân tích
            MedicalRecordRequest mockRequest = new MedicalRecordRequest();
            mockRequest.setPatientId(patientId);

            List<PrescriptionDetailRequest> details = allDrugs.stream()
                    .distinct()
                    .map(drugName -> {
                        PrescriptionDetailRequest detail = new PrescriptionDetailRequest();
                        detail.setMedicationName(drugName);
                        return detail;
                    })
                    .collect(Collectors.toList());

            mockRequest.setPrescriptionDetails(details);

            return analyzePrescription(mockRequest);

        } catch (Exception e) {
            log.error("Error analyzing patient medication history: ", e);
            SmartPrescriptionResponse errorResponse = new SmartPrescriptionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Lỗi khi phân tích lịch sử dùng thuốc: " + e.getMessage());
            return errorResponse;
        }
    }
}
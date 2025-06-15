package com.hospital.telemedicine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.telemedicine.dto.response.DrugInteractionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DDInterService {

    private static final String DDINTER_BASE_URL = "https://www.ddinter.org/ddinter/api";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Kiểm tra tương tác thuốc sử dụng DDInter API
     */
    public DrugInteractionResponse checkDrugInteractions(List<String> drugNames) {
        try {
            if (drugNames == null || drugNames.size() < 2) {
                return new DrugInteractionResponse(false, "Cần ít nhất 2 loại thuốc để kiểm tra tương tác",
                        Collections.emptyList(), Collections.emptyList());
            }

            log.info("Checking drug interactions for: {}", drugNames);

            // Chuẩn bị danh sách thuốc cho DDInter API
            List<String> cleanedDrugNames = drugNames.stream()
                    .map(this::cleanDrugName)
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());

            if (cleanedDrugNames.size() < 2) {
                return new DrugInteractionResponse(false, "Không thể xử lý tên thuốc",
                        Collections.emptyList(), Collections.emptyList());
            }

            // Kiểm tra từng cặp thuốc
            List<DrugInteractionResponse.InteractionDetail> allInteractions = new ArrayList<>();

            for (int i = 0; i < cleanedDrugNames.size(); i++) {
                for (int j = i + 1; j < cleanedDrugNames.size(); j++) {
                    String drug1 = cleanedDrugNames.get(i);
                    String drug2 = cleanedDrugNames.get(j);

                    List<DrugInteractionResponse.InteractionDetail> pairInteractions =
                            checkDrugPairInteraction(drug1, drug2);
                    allInteractions.addAll(pairInteractions);
                }
            }

            // Tìm thuốc thay thế nếu có tương tác nghiêm trọng
            List<DrugInteractionResponse.AlternativeDrug> alternatives = new ArrayList<>();
            boolean hasHighRiskInteraction = allInteractions.stream()
                    .anyMatch(interaction -> "high".equalsIgnoreCase(interaction.getSeverity()) ||
                            "major".equalsIgnoreCase(interaction.getSeverity()));

            if (hasHighRiskInteraction) {
                alternatives = findAlternativeDrugs(cleanedDrugNames);
            }

            String message = allInteractions.isEmpty() ?
                    "Không phát hiện tương tác thuốc có ý nghĩa lâm sàng" :
                    String.format("Phát hiện %d tương tác thuốc", allInteractions.size());

            return new DrugInteractionResponse(
                    !allInteractions.isEmpty(),
                    message,
                    allInteractions,
                    alternatives
            );

        } catch (Exception e) {
            log.error("Error checking drug interactions with DDInter: ", e);
            return new DrugInteractionResponse(false,
                    "Lỗi khi kiểm tra tương tác thuốc: " + e.getMessage(),
                    Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * Kiểm tra tương tác giữa một cặp thuốc
     */
    private List<DrugInteractionResponse.InteractionDetail> checkDrugPairInteraction(String drug1, String drug2) {
        List<DrugInteractionResponse.InteractionDetail> interactions = new ArrayList<>();

        try {
            // Tạo request body cho DDInter API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("drug1", drug1);
            requestBody.put("drug2", drug2);
            requestBody.put("format", "json");

            // Thiết lập headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Telemedicine-App/1.0");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Gọi DDInter API
            String url = DDINTER_BASE_URL + "/ddi";
            log.debug("Calling DDInter API: {} with drugs: {} and {}", url, drug1, drug2);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                interactions.addAll(parseDDInterResponse(root, drug1, drug2));
            }

        } catch (Exception e) {
            log.warn("Error checking interaction between {} and {}: {}", drug1, drug2, e.getMessage());

            // Fallback: Kiểm tra bằng phương pháp đơn giản
            DrugInteractionResponse.InteractionDetail fallbackCheck =
                    checkSimpleInteraction(drug1, drug2);
            if (fallbackCheck != null) {
                interactions.add(fallbackCheck);
            }
        }

        return interactions;
    }

    /**
     * Parse response từ DDInter API
     */
    private List<DrugInteractionResponse.InteractionDetail> parseDDInterResponse(JsonNode root, String drug1, String drug2) {
        List<DrugInteractionResponse.InteractionDetail> interactions = new ArrayList<>();

        try {
            // DDInter có thể trả về nhiều format khác nhau, cần xử lý linh hoạt
            if (root.has("interactions")) {
                JsonNode interactionsNode = root.get("interactions");

                if (interactionsNode.isArray()) {
                    for (JsonNode interactionNode : interactionsNode) {
                        DrugInteractionResponse.InteractionDetail detail = parseInteractionDetail(interactionNode, drug1, drug2);
                        if (detail != null) {
                            interactions.add(detail);
                        }
                    }
                }
            } else if (root.has("result")) {
                // Format khác của DDInter
                JsonNode resultNode = root.get("result");
                if (resultNode.has("interaction") && resultNode.get("interaction").asBoolean()) {
                    DrugInteractionResponse.InteractionDetail detail = new DrugInteractionResponse.InteractionDetail();
                    detail.setDrug1Name(drug1);
                    detail.setDrug2Name(drug2);
                    detail.setDescription(resultNode.has("description") ?
                            resultNode.get("description").asText() : "Phát hiện tương tác thuốc");
                    detail.setSeverity(resultNode.has("severity") ?
                            resultNode.get("severity").asText() : "moderate");
                    detail.setRecommendation(generateRecommendation(detail.getSeverity()));
                    interactions.add(detail);
                }
            } else if (root.has("ddi_score")) {
                // Nếu DDInter trả về điểm số tương tác
                double score = root.get("ddi_score").asDouble();
                if (score > 0.5) { // Ngưỡng tương tác có ý nghĩa
                    DrugInteractionResponse.InteractionDetail detail = new DrugInteractionResponse.InteractionDetail();
                    detail.setDrug1Name(drug1);
                    detail.setDrug2Name(drug2);
                    detail.setDescription(String.format("Điểm tương tác: %.2f", score));
                    detail.setSeverity(score > 0.8 ? "high" : "moderate");
                    detail.setRecommendation(generateRecommendation(detail.getSeverity()));
                    interactions.add(detail);
                }
            }

        } catch (Exception e) {
            log.error("Error parsing DDInter response: ", e);
        }

        return interactions;
    }

    /**
     * Parse chi tiết một tương tác từ response
     */
    private DrugInteractionResponse.InteractionDetail parseInteractionDetail(JsonNode node, String drug1, String drug2) {
        try {
            DrugInteractionResponse.InteractionDetail detail = new DrugInteractionResponse.InteractionDetail();

            detail.setDrug1Name(drug1);
            detail.setDrug2Name(drug2);

            // Mô tả tương tác
            if (node.has("description")) {
                detail.setDescription(node.get("description").asText());
            } else if (node.has("interaction_type")) {
                detail.setDescription("Loại tương tác: " + node.get("interaction_type").asText());
            } else {
                detail.setDescription("Phát hiện tương tác thuốc");
            }

            // Mức độ nghiêm trọng
            if (node.has("severity")) {
                detail.setSeverity(node.get("severity").asText().toLowerCase());
            } else if (node.has("risk_level")) {
                detail.setSeverity(mapRiskLevel(node.get("risk_level").asText()));
            } else {
                detail.setSeverity("moderate");
            }

            // Khuyến nghị
            if (node.has("recommendation")) {
                detail.setRecommendation(node.get("recommendation").asText());
            } else {
                detail.setRecommendation(generateRecommendation(detail.getSeverity()));
            }

            return detail;

        } catch (Exception e) {
            log.error("Error parsing interaction detail: ", e);
            return null;
        }
    }

    /**
     * Kiểm tra tương tác đơn giản (fallback method)
     */
    private DrugInteractionResponse.InteractionDetail checkSimpleInteraction(String drug1, String drug2) {
        // Database đơn giản các tương tác thuốc phổ biến
        Map<String, Map<String, String>> knownInteractions = getKnownInteractions();

        String key1 = drug1.toLowerCase().trim();
        String key2 = drug2.toLowerCase().trim();

        // Kiểm tra theo cả hai hướng
        if (knownInteractions.containsKey(key1) && knownInteractions.get(key1).containsKey(key2)) {
            return createInteractionDetail(drug1, drug2, knownInteractions.get(key1).get(key2));
        } else if (knownInteractions.containsKey(key2) && knownInteractions.get(key2).containsKey(key1)) {
            return createInteractionDetail(drug1, drug2, knownInteractions.get(key2).get(key1));
        }

        return null;
    }

    /**
     * Tạo chi tiết tương tác từ thông tin đã biết
     */
    private DrugInteractionResponse.InteractionDetail createInteractionDetail(String drug1, String drug2, String interactionInfo) {
        DrugInteractionResponse.InteractionDetail detail = new DrugInteractionResponse.InteractionDetail();
        detail.setDrug1Name(drug1);
        detail.setDrug2Name(drug2);

        String[] parts = interactionInfo.split("\\|");
        detail.setDescription(parts[0]);
        detail.setSeverity(parts.length > 1 ? parts[1] : "moderate");
        detail.setRecommendation(generateRecommendation(detail.getSeverity()));

        return detail;
    }

    /**
     * Database tương tác thuốc đã biết
     */
    private Map<String, Map<String, String>> getKnownInteractions() {
        Map<String, Map<String, String>> interactions = new HashMap<>();

        // Warfarin interactions
        Map<String, String> warfarinInteractions = new HashMap<>();
        warfarinInteractions.put("aspirin", "Tăng nguy cơ chảy máu|high");
        warfarinInteractions.put("ibuprofen", "Tăng nguy cơ chảy máu|high");
        warfarinInteractions.put("paracetamol", "Có thể tăng tác dụng chống đông máu|moderate");
        interactions.put("warfarin", warfarinInteractions);

        // NSAIDs interactions
        Map<String, String> ibuprofenInteractions = new HashMap<>();
        ibuprofenInteractions.put("aspirin", "Tăng nguy cơ tổn thương dạ dày|moderate");
        ibuprofenInteractions.put("prednisone", "Tăng nguy cơ loét dạ dày|high");
        interactions.put("ibuprofen", ibuprofenInteractions);

        // ACE inhibitors
        Map<String, String> aceInhibitorInteractions = new HashMap<>();
        aceInhibitorInteractions.put("spironolactone", "Tăng nguy cơ tăng kali máu|high");
        aceInhibitorInteractions.put("nsaid", "Giảm tác dụng hạ huyết áp|moderate");
        interactions.put("lisinopril", aceInhibitorInteractions);
        interactions.put("enalapril", aceInhibitorInteractions);

        return interactions;
    }

    /**
     * Tìm thuốc thay thế
     */
    private List<DrugInteractionResponse.AlternativeDrug> findAlternativeDrugs(List<String> problematicDrugs) {
        List<DrugInteractionResponse.AlternativeDrug> alternatives = new ArrayList<>();

        // Database thuốc thay thế đơn giản
        Map<String, List<String>> alternativeMap = getAlternativeDrugsMap();

        for (String drug : problematicDrugs) {
            String drugLower = drug.toLowerCase().trim();
            for (Map.Entry<String, List<String>> entry : alternativeMap.entrySet()) {
                if (drugLower.contains(entry.getKey())) {
                    for (String alternative : entry.getValue()) {
                        if (!problematicDrugs.stream().anyMatch(d ->
                                d.toLowerCase().contains(alternative.toLowerCase()))) {
                            DrugInteractionResponse.AlternativeDrug altDrug =
                                    new DrugInteractionResponse.AlternativeDrug();
                            altDrug.setName(alternative);
                            altDrug.setOriginalRxcui(drug);
                            altDrug.setReason("Ít tương tác hơn với các thuốc khác");
                            alternatives.add(altDrug);
                        }
                    }
                }
            }
        }

        return alternatives.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * Database thuốc thay thế
     */
    private Map<String, List<String>> getAlternativeDrugsMap() {
        Map<String, List<String>> alternatives = new HashMap<>();

        alternatives.put("aspirin", Arrays.asList("clopidogrel", "dipyridamole"));
        alternatives.put("ibuprofen", Arrays.asList("celecoxib", "paracetamol", "diclofenac gel"));
        alternatives.put("warfarin", Arrays.asList("rivaroxaban", "apixaban", "dabigatran"));
        alternatives.put("omeprazole", Arrays.asList("lansoprazole", "pantoprazole", "esomeprazole"));

        return alternatives;
    }

    /**
     * Làm sạch tên thuốc
     */
    private String cleanDrugName(String drugName) {
        if (drugName == null) return "";

        return drugName.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    /**
     * Ánh xạ mức độ rủi ro
     */
    private String mapRiskLevel(String riskLevel) {
        if (riskLevel == null) return "moderate";

        switch (riskLevel.toLowerCase()) {
            case "high":
            case "major":
            case "severe":
                return "high";
            case "low":
            case "minor":
            case "mild":
                return "low";
            default:
                return "moderate";
        }
    }

    /**
     * Tạo khuyến nghị dựa trên mức độ nghiêm trọng
     */
    private String generateRecommendation(String severity) {
        if (severity == null) severity = "moderate";

        switch (severity.toLowerCase()) {
            case "high":
            case "major":
                return "Tránh sử dụng đồng thời. Cân nhắc thay thuốc khác hoặc điều chỉnh liều lượng.";
            case "moderate":
                return "Theo dõi chặt chẽ bệnh nhân. Có thể cần điều chỉnh liều hoặc thời gian dùng thuốc.";
            case "low":
            case "minor":
                return "Theo dõi tác dụng phụ. Thông báo cho bệnh nhân về các triệu chứng cần chú ý.";
            default:
                return "Cần theo dõi khi sử dụng đồng thời.";
        }
    }

    /**
     * Kiểm tra trạng thái hoạt động của DDInter API
     */
    public boolean isDDInterAvailable() {
        try {
            String url = DDINTER_BASE_URL + "/status";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("DDInter API not available: {}", e.getMessage());
            return false;
        }
    }
}
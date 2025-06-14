package com.hospital.telemedicine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.telemedicine.dto.response.DrugInteractionResponse;
import com.hospital.telemedicine.dto.response.DrugSuggestionResponse;
import com.hospital.telemedicine.dto.response.GenericDrugResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DrugInteractionService {

    private static final String RXNAV_BASE_URL = "https://rxnav.nlm.nih.gov/REST";
    private static final String RXCLASS_BASE_URL = "https://rxnav.nlm.nih.gov/REST/rxclass";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Tìm kiếm thuốc theo tên
     */
    public List<DrugSuggestionResponse> searchDrugs(String drugName) {
        try {
            String encodedName = URLEncoder.encode(drugName, StandardCharsets.UTF_8);
            String url = RXNAV_BASE_URL + "/drugs.json?name=" + encodedName;

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            List<DrugSuggestionResponse> suggestions = new ArrayList<>();

            if (root.has("drugGroup") && root.get("drugGroup").has("conceptGroup")) {
                JsonNode conceptGroups = root.get("drugGroup").get("conceptGroup");

                if (conceptGroups.isArray()) {
                    for (JsonNode group : conceptGroups) {
                        if (group.has("conceptProperties")) {
                            JsonNode properties = group.get("conceptProperties");
                            if (properties.isArray()) {
                                for (JsonNode prop : properties) {
                                    DrugSuggestionResponse suggestion = new DrugSuggestionResponse();
                                    suggestion.setRxcui(prop.get("rxcui").asText());
                                    suggestion.setName(prop.get("name").asText());
                                    suggestion.setSynonym(prop.has("synonym") ? prop.get("synonym").asText() : "");
                                    suggestion.setTty(prop.has("tty") ? prop.get("tty").asText() : "");
                                    suggestions.add(suggestion);
                                }
                            }
                        }
                    }
                }
            }

            return suggestions;
        } catch (Exception e) {
            log.error("Error searching drugs: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Kiểm tra tương tác giữa các thuốc
     */
    public DrugInteractionResponse checkDrugInteractions(List<String> rxcuis) {
        try {
            if (rxcuis.size() < 2) {
                return new DrugInteractionResponse(false, "Cần ít nhất 2 loại thuốc để kiểm tra tương tác",
                        Collections.emptyList(), Collections.emptyList());
            }

            String rxcuiList = String.join("+", rxcuis);
            String url = RXNAV_BASE_URL + "/interaction/list.json?rxcuis=" + rxcuiList;

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            List<DrugInteractionResponse.InteractionDetail> interactions = new ArrayList<>();
            List<DrugInteractionResponse.AlternativeDrug> alternatives = new ArrayList<>();

            if (root.has("fullInteractionTypeGroup")) {
                JsonNode interactionGroups = root.get("fullInteractionTypeGroup");

                if (interactionGroups.isArray()) {
                    for (JsonNode group : interactionGroups) {
                        if (group.has("fullInteractionType")) {
                            JsonNode interactionTypes = group.get("fullInteractionType");

                            if (interactionTypes.isArray()) {
                                for (JsonNode interactionType : interactionTypes) {
                                    if (interactionType.has("interactionPair")) {
                                        JsonNode pairs = interactionType.get("interactionPair");

                                        if (pairs.isArray()) {
                                            for (JsonNode pair : pairs) {
                                                DrugInteractionResponse.InteractionDetail detail =
                                                        new DrugInteractionResponse.InteractionDetail();

                                                // Lấy thông tin thuốc 1
                                                if (pair.has("interactionConcept")) {
                                                    JsonNode concepts = pair.get("interactionConcept");
                                                    if (concepts.isArray() && concepts.size() >= 2) {
                                                        detail.setDrug1Name(concepts.get(0).get("minConceptItem").get("name").asText());
                                                        detail.setDrug1Rxcui(concepts.get(0).get("minConceptItem").get("rxcui").asText());
                                                        detail.setDrug2Name(concepts.get(1).get("minConceptItem").get("name").asText());
                                                        detail.setDrug2Rxcui(concepts.get(1).get("minConceptItem").get("rxcui").asText());
                                                    }
                                                }

                                                // Lấy mô tả tương tác
                                                if (pair.has("description")) {
                                                    detail.setDescription(pair.get("description").asText());
                                                }

                                                // Lấy mức độ nghiêm trọng
                                                if (pair.has("severity")) {
                                                    detail.setSeverity(pair.get("severity").asText());
                                                }

                                                interactions.add(detail);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tìm thuốc thay thế nếu có tương tác nghiêm trọng
            boolean hasHighRiskInteraction = interactions.stream()
                    .anyMatch(interaction -> "high".equalsIgnoreCase(interaction.getSeverity()) ||
                            "contraindicated".equalsIgnoreCase(interaction.getSeverity()));

            if (hasHighRiskInteraction) {
                alternatives = findAlternativeDrugs(rxcuis);
            }

            return new DrugInteractionResponse(
                    !interactions.isEmpty(),
                    interactions.isEmpty() ? "Không phát hiện tương tác thuốc" :
                            "Phát hiện " + interactions.size() + " tương tác thuốc",
                    interactions,
                    alternatives
            );

        } catch (Exception e) {
            log.error("Error checking drug interactions: ", e);
            return new DrugInteractionResponse(false, "Lỗi khi kiểm tra tương tác thuốc: " + e.getMessage(),
                    Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * Tìm thuốc generic (thuốc thay thế giá rẻ)
     */
    public List<GenericDrugResponse> findGenericAlternatives(String rxcui) {
        try {
            // Tìm generic equivalent
            String url = RXNAV_BASE_URL + "/rxcui/" + rxcui + "/related.json?tty=SCD+SBD+GPCK+BPCK";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            List<GenericDrugResponse> generics = new ArrayList<>();

            if (root.has("relatedGroup") && root.get("relatedGroup").has("conceptGroup")) {
                JsonNode conceptGroups = root.get("relatedGroup").get("conceptGroup");

                if (conceptGroups.isArray()) {
                    for (JsonNode group : conceptGroups) {
                        if (group.has("conceptProperties")) {
                            JsonNode properties = group.get("conceptProperties");
                            if (properties.isArray()) {
                                for (JsonNode prop : properties) {
                                    GenericDrugResponse generic = new GenericDrugResponse();
                                    generic.setRxcui(prop.get("rxcui").asText());
                                    generic.setName(prop.get("name").asText());
                                    generic.setTty(prop.has("tty") ? prop.get("tty").asText() : "");

                                    // Kiểm tra xem có phải là generic không
                                    String tty = generic.getTty();
                                    generic.setGeneric(tty.equals("SCD") || tty.equals("GPCK"));

                                    // Ước tính tiết kiệm chi phí (giả định)
                                    if (generic.isGeneric()) {
                                        generic.setEstimatedSavings("20-80%");
                                    }

                                    generics.add(generic);
                                }
                            }
                        }
                    }
                }
            }

            return generics;
        } catch (Exception e) {
            log.error("Error finding generic alternatives: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Gợi ý thuốc theo chẩn đoán/bệnh
     */
    public List<DrugSuggestionResponse> suggestDrugsByDiagnosis(String diagnosis) {
        try {
            // Tìm kiếm theo indication/diagnosis
            String encodedDiagnosis = URLEncoder.encode(diagnosis, StandardCharsets.UTF_8);
            String url = RXCLASS_BASE_URL + "/class/byName.json?className=" + encodedDiagnosis + "&relaSource=MEDRT";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            List<DrugSuggestionResponse> suggestions = new ArrayList<>();

            // Nếu không tìm thấy theo tên chính xác, thử tìm theo keyword
            if (!root.has("rxclassMinConceptList") ||
                    !root.get("rxclassMinConceptList").has("rxclassMinConcept")) {

                // Fallback: tìm kiếm một số thuốc phổ biến theo từ khóa
                suggestions = searchCommonDrugsByKeyword(diagnosis);
            } else {
                // Xử lý kết quả từ RxClass nếu có
                JsonNode concepts = root.get("rxclassMinConceptList").get("rxclassMinConcept");
                if (concepts.isArray()) {
                    for (JsonNode concept : concepts) {
                        DrugSuggestionResponse suggestion = new DrugSuggestionResponse();
                        suggestion.setRxcui(concept.get("classId").asText());
                        suggestion.setName(concept.get("className").asText());
                        suggestion.setIndication(diagnosis);
                        suggestions.add(suggestion);
                    }
                }
            }

            return suggestions;
        } catch (Exception e) {
            log.error("Error suggesting drugs by diagnosis: ", e);
            return searchCommonDrugsByKeyword(diagnosis);
        }
    }

    /**
     * Tìm thuốc thay thế khi có tương tác
     */
    private List<DrugInteractionResponse.AlternativeDrug> findAlternativeDrugs(List<String> problematicRxcuis) {
        List<DrugInteractionResponse.AlternativeDrug> alternatives = new ArrayList<>();

        for (String rxcui : problematicRxcuis) {
            try {
                // Tìm thuốc cùng nhóm trị liệu
                String url = RXCLASS_BASE_URL + "/class/byRxcui.json?rxcui=" + rxcui + "&relaSource=ATC";
                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);

                if (root.has("rxclassDrugInfoList") && root.get("rxclassDrugInfoList").has("rxclassDrugInfo")) {
                    JsonNode drugInfos = root.get("rxclassDrugInfoList").get("rxclassDrugInfo");

                    if (drugInfos.isArray() && drugInfos.size() > 0) {
                        JsonNode firstDrugInfo = drugInfos.get(0);
                        String classId = firstDrugInfo.get("rxclassMinConceptItem").get("classId").asText();

                        // Tìm thuốc khác trong cùng nhóm
                        String altUrl = RXCLASS_BASE_URL + "/classMembers.json?classId=" + classId + "&relaSource=ATC";
                        String altResponse = restTemplate.getForObject(altUrl, String.class);
                        JsonNode altRoot = objectMapper.readTree(altResponse);

                        if (altRoot.has("drugMemberGroup") && altRoot.get("drugMemberGroup").has("drugMember")) {
                            JsonNode members = altRoot.get("drugMemberGroup").get("drugMember");

                            if (members.isArray()) {
                                for (JsonNode member : members) {
                                    String altRxcui = member.get("minConcept").get("rxcui").asText();
                                    if (!problematicRxcuis.contains(altRxcui)) {
                                        DrugInteractionResponse.AlternativeDrug alternative =
                                                new DrugInteractionResponse.AlternativeDrug();
                                        alternative.setRxcui(altRxcui);
                                        alternative.setName(member.get("minConcept").get("name").asText());
                                        alternative.setOriginalRxcui(rxcui);
                                        alternative.setReason("Thuốc cùng nhóm trị liệu, ít tương tác hơn");
                                        alternatives.add(alternative);

                                        if (alternatives.size() >= 10) break; // Giới hạn số lượng
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error finding alternatives for rxcui: " + rxcui, e);
            }
        }

        return alternatives;
    }

    /**
     * Tìm thuốc phổ biến theo từ khóa (fallback method)
     */
    private List<DrugSuggestionResponse> searchCommonDrugsByKeyword(String keyword) {
        List<DrugSuggestionResponse> suggestions = new ArrayList<>();

        // Danh sách thuốc phổ biến theo các bệnh thường gặp
        Map<String, List<String>> commonDrugs = new HashMap<>();
        commonDrugs.put("đau đầu", Arrays.asList("paracetamol", "ibuprofen", "aspirin"));
        commonDrugs.put("cảm cúm", Arrays.asList("paracetamol", "ibuprofen", "cetirizine"));
        commonDrugs.put("ho", Arrays.asList("dextromethorphan", "guaifenesin", "codeine"));
        commonDrugs.put("tiêu hóa", Arrays.asList("omeprazole", "ranitidine", "simethicone"));
        commonDrugs.put("đau bụng", Arrays.asList("omeprazole", "buscopan", "paracetamol"));
        commonDrugs.put("sốt", Arrays.asList("paracetamol", "ibuprofen", "aspirin"));
        commonDrugs.put("dị ứng", Arrays.asList("cetirizine", "loratadine", "diphenhydramine"));

        String lowerKeyword = keyword.toLowerCase();

        for (Map.Entry<String, List<String>> entry : commonDrugs.entrySet()) {
            if (lowerKeyword.contains(entry.getKey())) {
                for (String drugName : entry.getValue()) {
                    List<DrugSuggestionResponse> searchResults = searchDrugs(drugName);
                    if (!searchResults.isEmpty()) {
                        DrugSuggestionResponse suggestion = searchResults.get(0);
                        suggestion.setIndication(keyword);
                        suggestions.add(suggestion);
                    }
                }
                break;
            }
        }

        return suggestions;
    }

    /**
     * Lấy thông tin chi tiết thuốc
     */
    public DrugSuggestionResponse getDrugDetails(String rxcui) {
        try {
            String url = RXNAV_BASE_URL + "/rxcui/" + rxcui + "/properties.json";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            DrugSuggestionResponse details = new DrugSuggestionResponse();

            if (root.has("properties")) {
                JsonNode properties = root.get("properties");
                details.setRxcui(properties.get("rxcui").asText());
                details.setName(properties.get("name").asText());
                details.setTty(properties.has("tty") ? properties.get("tty").asText() : "");
                details.setSynonym(properties.has("synonym") ? properties.get("synonym").asText() : "");
            }

            return details;
        } catch (Exception e) {
            log.error("Error getting drug details: ", e);
            return null;
        }
    }
}
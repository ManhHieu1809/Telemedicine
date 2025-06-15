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

    @Autowired
    private DDInterService ddInterService;

    /**
     * Tìm kiếm thuốc theo tên (sử dụng RxNav - vẫn hoạt động)
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
     * Kiểm tra tương tác giữa các thuốc - SỬ DỤNG DDINTER API
     */
    public DrugInteractionResponse checkDrugInteractions(List<String> rxcuis) {
        try {
            log.info("Checking drug interactions using DDInter API");

            // Chuyển đổi RXCUI thành tên thuốc nếu cần
            List<String> drugNames = new ArrayList<>();

            for (String rxcui : rxcuis) {
                // Nếu rxcui là số, cố gắng chuyển thành tên thuốc
                if (rxcui.matches("\\d+")) {
                    String drugName = getDrugNameFromRxcui(rxcui);
                    if (drugName != null && !drugName.isEmpty()) {
                        drugNames.add(drugName);
                    } else {
                        drugNames.add(rxcui); // Fallback nếu không tìm được tên
                    }
                } else {
                    drugNames.add(rxcui); // Đã là tên thuốc
                }
            }

            // Sử dụng DDInter service để kiểm tra tương tác
            DrugInteractionResponse ddinterResult = ddInterService.checkDrugInteractions(drugNames);

            if (ddinterResult.isHasInteractions()) {
                log.info("Found {} interactions using DDInter", ddinterResult.getInteractions().size());
            } else {
                log.info("No interactions found using DDInter");
            }

            return ddinterResult;

        } catch (Exception e) {
            log.error("Error checking drug interactions: ", e);
            return new DrugInteractionResponse(false,
                    "Lỗi khi kiểm tra tương tác thuốc: " + e.getMessage(),
                    Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * Lấy tên thuốc từ RXCUI
     */
    private String getDrugNameFromRxcui(String rxcui) {
        try {
            String url = RXNAV_BASE_URL + "/rxcui/" + rxcui + "/properties.json";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("properties")) {
                JsonNode properties = root.get("properties");
                return properties.get("name").asText();
            }
        } catch (Exception e) {
            log.warn("Could not get drug name for RXCUI: {}", rxcui);
        }
        return null;
    }

    /**
     * Tìm thuốc generic (sử dụng RxNav - vẫn hoạt động)
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
     * Gợi ý thuốc theo chẩn đoán/bệnh (sử dụng RxNav + fallback)
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
     * Lấy thông tin chi tiết thuốc (sử dụng RxNav)
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

    /**
     * Kiểm tra trạng thái của các API
     */
    public Map<String, Boolean> checkApiStatus() {
        Map<String, Boolean> status = new HashMap<>();

        // Kiểm tra RxNav
        try {
            String url = RXNAV_BASE_URL + "/allstatus.json";
            String response = restTemplate.getForObject(url, String.class);
            status.put("rxnav", response != null);
        } catch (Exception e) {
            status.put("rxnav", false);
        }

        // Kiểm tra DDInter
        status.put("ddinter", ddInterService.isDDInterAvailable());

        return status;
    }

    /**
     * Phương thức tương thích ngược cho các service khác
     * @deprecated Sử dụng checkDrugInteractions với tên thuốc thay vì RXCUI
     */
    @Deprecated
    public DrugInteractionResponse checkDrugInteractionsLegacy(List<String> rxcuis) {
        log.warn("Using legacy drug interaction check method. Consider updating to use drug names directly.");
        return checkDrugInteractions(rxcuis);
    }
}
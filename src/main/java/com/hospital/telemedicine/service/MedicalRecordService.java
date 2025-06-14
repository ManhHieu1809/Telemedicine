package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.MedicalRecordRequest;
import com.hospital.telemedicine.dto.request.PrescriptionDetailRequest;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {
    @Autowired
    private SmartPrescriptionService smartPrescriptionService;

    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PrescriptionRepository prescriptionRepository,
                                PrescriptionDetailRepository prescriptionDetailRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                UserFcmTokenRepository userFcmTokenRepository,
                                JavaMailSender mailSender,
                                NotificationRepository notificationRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionDetailRepository = prescriptionDetailRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.userFcmTokenRepository = userFcmTokenRepository;
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }

    @PreAuthorize("hasRole('DOCTOR') and authentication.principal.id == @doctorRepository.findById(#request.doctorId).orElseThrow().user.id")
    @Transactional
    public MedicalRecordResponse createMedicalRecordWithDrugCheck(MedicalRecordRequest request) {
        try {
            System.out.println("Creating medical record with drug safety check...");

            // 1. Phân tích đơn thuốc trước khi tạo
            SmartPrescriptionResponse drugAnalysis = smartPrescriptionService.analyzePrescription(request);

            // 2. Kiểm tra cảnh báo nghiêm trọng
            boolean hasHighRiskWarnings = drugAnalysis.getWarnings() != null &&
                    drugAnalysis.getWarnings().stream()
                            .anyMatch(warning -> "HIGH".equals(warning.getSeverity()));

            if (hasHighRiskWarnings) {
                // Trả về cảnh báo thay vì tạo đơn thuốc ngay
                StringBuilder warningMessage = new StringBuilder("CẢNH BÁO NGHIÊM TRỌNG:\n");
                drugAnalysis.getWarnings().stream()
                        .filter(w -> "HIGH".equals(w.getSeverity()))
                        .forEach(w -> warningMessage.append("- ").append(w.getMessage()).append("\n"));

                warningMessage.append("\nVui lòng xem xét lại đơn thuốc trước khi lưu.");

                MedicalRecordResponse warningResponse = new MedicalRecordResponse(warningMessage.toString());
                // Thêm thông tin phân tích thuốc vào response
                warningResponse.setDrugAnalysis(drugAnalysis);
                return warningResponse;
            }

            // 3. Nếu không có cảnh báo nghiêm trọng, tiếp tục tạo medical record
            MedicalRecordResponse result = createMedicalRecord(request);

            // 4. Thêm thông tin phân tích thuốc vào response
            if (result.isSuccess()) {
                result.setDrugAnalysis(drugAnalysis);

                // Thêm thông tin generic alternatives vào message nếu có
                if (drugAnalysis.getGenericAlternatives() != null && !drugAnalysis.getGenericAlternatives().isEmpty()) {
                    String originalMessage = result.getMessage();
                    result.setMessage(originalMessage + " Tìm thấy " + drugAnalysis.getGenericAlternatives().size() + " thuốc generic thay thế.");
                }
            }

            return result;

        } catch (Exception e) {
            System.err.println("Error creating medical record with drug check: " + e.getMessage());
            return new MedicalRecordResponse("Lỗi khi tạo hồ sơ y tế với kiểm tra thuốc: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('DOCTOR') and authentication.principal.id == @doctorRepository.findById(#request.doctorId).orElseThrow().user.id")
    @Transactional
    public MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request) {
        try {
            System.out.println("Request: " + request);
            System.out.println("Authentication principal ID: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            Patient patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + request.getPatientId()));
            System.out.println("Patient found: " + patient);
            Doctor doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with ID: " + request.getDoctorId()));
            System.out.println("Doctor found: " + doctor);

            // Kiểm tra prescriptionDetails
            if (request.getPrescriptionDetails() == null || request.getPrescriptionDetails().isEmpty()) {
                throw new IllegalArgumentException("Prescription details cannot be empty");
            }

            // Tạo đơn thuốc và lưu trước
            Prescription prescription = new Prescription();
            prescription.setDoctor(doctor);
            prescription.setPatient(patient);
            prescription.setNotes(request.getNotes());
            prescription.setPrescribedDate(LocalDateTime.now());
            Prescription savedPrescription = prescriptionRepository.save(prescription);

            // Tạo chi tiết đơn thuốc
            List<PrescriptionDetail> details = request.getPrescriptionDetails().stream().map(detailRequest -> {
                if (detailRequest.getMedicationName() == null || detailRequest.getMedicationName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Medication name is required for each prescription detail");
                }
                PrescriptionDetail detail = new PrescriptionDetail();
                detail.setPrescription(savedPrescription);
                detail.setMedicationName(detailRequest.getMedicationName());
                detail.setDosage(detailRequest.getDosage());
                detail.setFrequency(detailRequest.getFrequency());
                detail.setDuration(detailRequest.getDuration());
                detail.setInstructions(detailRequest.getInstructions());
                return detail;
            }).collect(Collectors.toList());
            savedPrescription.setDetails(details);

            // Lưu lại prescription với details
            prescriptionRepository.save(savedPrescription);

            // Tạo hồ sơ y tế
            MedicalRecord medicalRecord = new MedicalRecord();
            medicalRecord.setPatient(patient);
            medicalRecord.setDoctor(doctor);
            medicalRecord.setDiagnosis(request.getDiagnosis());
            medicalRecord.setPrescription(savedPrescription);
            medicalRecord.setRecordDate(LocalDateTime.now());
            medicalRecord = medicalRecordRepository.save(medicalRecord);

            sendPushNotification(patient.getUser().getId(), "Hồ sơ y tế mới",
                    "Hồ sơ y tế mới được tạo bởi Dr. " + doctor.getFullName());
            createNotification(patient.getUser(), "Hồ sơ y tế mới được tạo bởi Dr. " + doctor.getFullName());

            return mapToResponse(medicalRecord, true, "Medical record created successfully");
      } catch (IllegalArgumentException e) {
            return new MedicalRecordResponse(e.getMessage());
        } catch (Exception e) {
            return new MedicalRecordResponse("Failed to create medical record: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('PATIENT') and #patientId == @patientRepository.findById(#patientId).orElseThrow().id")
    public List<MedicalRecordResponse> getMedicalRecordsByPatient(Long patientId) {
        try {
            List<MedicalRecordResponse> responses = medicalRecordRepository.findByPatientId(patientId).stream()
                    .map(medicalRecord -> mapToResponse(medicalRecord, true, "Retrieved medical record successfully"))
                    .collect(Collectors.toList());
            return responses.isEmpty() ? List.of(new MedicalRecordResponse("No medical records found")) : responses;
        } catch (IllegalArgumentException e) {
            return List.of(new MedicalRecordResponse(e.getMessage()));
        } catch (Exception e) {
            return List.of(new MedicalRecordResponse("Failed to retrieve medical records: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('DOCTOR') and authentication.principal.id == @medicalRecordRepository.findById(#id).orElseThrow().doctor.user.id")
    @Transactional
    public MedicalRecordResponse updateMedicalRecord(Long id, MedicalRecordRequest request) {
        try {
            MedicalRecord medicalRecord = medicalRecordRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record not found"));
            Patient patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
            Doctor doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            // Cập nhật chẩn đoán
            medicalRecord.setDiagnosis(request.getDiagnosis());

            // Kiểm tra prescriptionDetails
            if (request.getPrescriptionDetails() == null || request.getPrescriptionDetails().isEmpty()) {
                throw new IllegalArgumentException("Prescription details cannot be empty");
            }

            // Cập nhật đơn thuốc
            Prescription prescription = medicalRecord.getPrescription();
            if (prescription == null) {
                prescription = new Prescription();
                prescription.setDoctor(doctor);
                prescription.setPatient(patient);
                prescription.setPrescribedDate(LocalDateTime.now());
            }
            prescription.setNotes(request.getNotes());
            prescription.setPrescribedDate(LocalDateTime.now());

            // Lưu prescription trước
            Prescription savedPrescription = prescriptionRepository.save(prescription);

            // Xóa chi tiết đơn thuốc cũ
            prescriptionDetailRepository.deleteByPrescriptionId(savedPrescription.getId());

            // Tạo chi tiết đơn thuốc mới
            List<PrescriptionDetail> details = request.getPrescriptionDetails().stream().map(detailRequest -> {
                if (detailRequest.getMedicationName() == null || detailRequest.getMedicationName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Medication name is required for each prescription detail");
                }
                PrescriptionDetail detail = new PrescriptionDetail();
                detail.setPrescription(savedPrescription);
                detail.setMedicationName(detailRequest.getMedicationName());
                detail.setDosage(detailRequest.getDosage());
                detail.setFrequency(detailRequest.getFrequency());
                detail.setDuration(detailRequest.getDuration());
                detail.setInstructions(detailRequest.getInstructions());
                return detail;
            }).collect(Collectors.toList());
            savedPrescription.setDetails(details);

            // Lưu lại prescription với details
            prescriptionRepository.save(savedPrescription);

            medicalRecord.setPrescription(savedPrescription);
            medicalRecord = medicalRecordRepository.save(medicalRecord);

            sendPushNotification(patient.getUser().getId(), "Hồ sơ y tế cập nhật",
                    "Hồ sơ y tế của bạn đã được cập nhật bởi Dr. " + doctor.getFullName());
            createNotification(patient.getUser(), "Hồ sơ y tế của bạn đã được cập nhật bởi Dr. " + doctor.getFullName());

            return mapToResponse(medicalRecord, true, "Medical record updated successfully");
        } catch (IllegalArgumentException e) {
            return new MedicalRecordResponse(e.getMessage());
        } catch (Exception e) {
            return new MedicalRecordResponse("Failed to create medical record: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('PATIENT') and authentication.principal.id == @medicalRecordRepository.findById(#id).orElseThrow().patient.user.id")
    public String generateMedicalRecordPdf(Long id) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Medical record not found"));

        StringBuilder latexContent = new StringBuilder();
        latexContent.append("\\documentclass{article}\n")
                .append("\\usepackage[utf8]{vietnam}\n")
                .append("\\usepackage{geometry}\n")
                .append("\\geometry{a4paper, margin=1in}\n")
                .append("\\usepackage{fancyhdr}\n")
                .append("\\pagestyle{fancy}\n")
                .append("\\fancyhf{}\n")
                .append("\\fancyhead[C]{Hồ Sơ Y Tế}\n")
                .append("\\fancyfoot[C]{\\thepage}\n")
                .append("\\begin{document}\n")
                .append("\\section*{Hồ Sơ Y Tế}\n")
                .append("\\textbf{Bệnh nhân:} ").append(sanitizeLatex(medicalRecord.getPatient().getFullName())).append("\\\\\n")
                .append("\\textbf{Bác sĩ:} ").append(sanitizeLatex(medicalRecord.getDoctor().getFullName())).append("\\\\\n")
                .append("\\textbf{Ngày khám:} ").append(medicalRecord.getRecordDate()).append("\\\\\n")
                .append("\\textbf{Chẩn đoán:} ").append(sanitizeLatex(medicalRecord.getDiagnosis())).append("\\\\\n");

        Prescription prescription = medicalRecord.getPrescription();
        if (prescription != null) {
            latexContent.append("\\subsection*{Đơn thuốc}\n")
                    .append("\\textbf{Ngày kê đơn:} ").append(prescription.getPrescribedDate()).append("\\\\\n")
                    .append("\\textbf{Ghi chú:} ").append(sanitizeLatex(prescription.getNotes() != null ? prescription.getNotes() : "")).append("\\\\\n")
                    .append("\\begin{itemize}\n");
            for (PrescriptionDetail detail : prescription.getDetails()) {
                latexContent.append("\\item \\textbf{Thuốc:} ").append(sanitizeLatex(detail.getMedicationName())).append(", ")
                        .append("\\textbf{Liều lượng:} ").append(sanitizeLatex(detail.getDosage())).append(", ")
                        .append("\\textbf{Tần suất:} ").append(sanitizeLatex(detail.getFrequency())).append(", ")
                        .append("\\textbf{Thời gian:} ").append(sanitizeLatex(detail.getDuration())).append(", ")
                        .append("\\textbf{Hướng dẫn:} ").append(sanitizeLatex(detail.getInstructions() != null ? detail.getInstructions() : "")).append("\n");
            }
            latexContent.append("\\end{itemize}\n");
        }

        latexContent.append("\\end{document}\n");

        return latexContent.toString();
    }

    @PreAuthorize("hasRole('PATIENT') and authentication.principal.id == @medicalRecordRepository.findById(#id).orElseThrow().patient.user.id")
    @Transactional
    public MedicalRecordResponse sendMedicalRecordByEmail(Long id) {
        try {
            MedicalRecord medicalRecord = medicalRecordRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record not found"));
            String email = medicalRecord.getPatient().getUser().getEmail();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Hồ Sơ Y Tế");
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("Kính gửi ").append(medicalRecord.getPatient().getFullName()).append(",\n\n")
                    .append("Hồ sơ y tế của bạn đã được gửi kèm theo email này.\n")
                    .append("Thông tin chi tiết:\n")
                    .append("- Bác sĩ: ").append(medicalRecord.getDoctor().getFullName()).append("\n")
                    .append("- Ngày khám: ").append(medicalRecord.getRecordDate()).append("\n")
                    .append("- Chẩn đoán: ").append(medicalRecord.getDiagnosis()).append("\n");

            Prescription prescription = medicalRecord.getPrescription();
            if (prescription != null) {
                emailContent.append("- Đơn thuốc:\n")
                        .append("  + Ngày kê đơn: ").append(prescription.getPrescribedDate()).append("\n");
                for (PrescriptionDetail detail : prescription.getDetails()) {
                    emailContent.append("  + ").append(detail.getMedicationName()).append(": ")
                            .append(detail.getDosage()).append(", ")
                            .append(detail.getFrequency()).append(", ")
                            .append(detail.getDuration());
                    if (detail.getInstructions() != null) {
                        emailContent.append(", ").append(detail.getInstructions());
                    }
                    emailContent.append("\n");
                }
                if (prescription.getNotes() != null) {
                    emailContent.append("- Ghi chú: ").append(prescription.getNotes()).append("\n");
                }
            }

            helper.setText(emailContent.toString());
            mailSender.send(message);

            createNotification(medicalRecord.getPatient().getUser(), "Hồ sơ y tế đã được gửi qua email.");

            return mapToResponse(medicalRecord, true, "Medical record sent successfully via email");
        } catch (IllegalArgumentException e) {
            return new MedicalRecordResponse(e.getMessage());
        } catch (MessagingException e) {
            return new MedicalRecordResponse("Failed to send medical record via email: " + e.getMessage());
        } catch (Exception e) {
            return new MedicalRecordResponse("Unexpected error while sending medical record: " + e.getMessage());
        }
    }

    private void sendPushNotification(Long userId, String title, String body) {
        List<UserFcmToken> tokens = userFcmTokenRepository.findByUserId(userId);
        for (UserFcmToken token : tokens) {
            com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(token.getFcmToken())
                    .build();

            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().send(message);
            } catch (Exception e) {
                System.err.println("Failed to send push notification to token " + token.getFcmToken() + ": " + e.getMessage());
            }
        }
    }

    private void createNotification(User user, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setStatus(Notification.Status.UNREAD);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private MedicalRecordResponse mapToResponse(MedicalRecord medicalRecord, boolean success, String message) {
        Prescription prescription = medicalRecord.getPrescription();
        List<PrescriptionDetailResponse> prescriptionDetails = prescription != null ?
                prescription.getDetails().stream().map(detail -> {
                    PrescriptionDetailResponse detailResponse = new PrescriptionDetailResponse();
                    detailResponse.setId(detail.getId());
                    detailResponse.setMedicationName(detail.getMedicationName());
                    detailResponse.setDosage(detail.getDosage());
                    detailResponse.setFrequency(detail.getFrequency());
                    detailResponse.setDuration(detail.getDuration());
                    detailResponse.setInstructions(detail.getInstructions());
                    return detailResponse;
                }).collect(Collectors.toList()) : null;

        return new MedicalRecordResponse(
                medicalRecord.getId(),
                medicalRecord.getPatient().getId(),
                medicalRecord.getPatient().getFullName(),
                medicalRecord.getDoctor().getId(),
                medicalRecord.getDoctor().getFullName(),
                medicalRecord.getDiagnosis(),
                prescription != null ? prescription.getNotes() : null,
                medicalRecord.getRecordDate(),
                prescription != null ? prescription.getPrescribedDate() : null,
                prescriptionDetails,
                success,
                message
        );
    }

    private String sanitizeLatex(String input) {
        if (input == null) return "";
        return input.replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde")
                .replace("^", "\\textasciicircum")
                .replace("\\", "\\textbackslash");
    }

    // Thêm các methods sau vào MedicalRecordService

    @PreAuthorize("hasRole('DOCTOR') and authentication.principal.id == @medicalRecordRepository.findById(#id).orElseThrow().doctor.user.id")
    @Transactional
    public MedicalRecordResponse updateMedicalRecordWithDrugCheck(Long id, MedicalRecordRequest request) {
        try {
            System.out.println("Updating medical record with drug safety check...");

            // 1. Phân tích đơn thuốc trước khi cập nhật
            if (request.isCheckDrugInteractions()) {
                SmartPrescriptionResponse drugAnalysis = smartPrescriptionService.analyzePrescription(request);

                // 2. Kiểm tra cảnh báo nghiêm trọng
                boolean hasHighRiskWarnings = drugAnalysis.getWarnings() != null &&
                        drugAnalysis.getWarnings().stream()
                                .anyMatch(warning -> "HIGH".equals(warning.getSeverity()));

                if (hasHighRiskWarnings && !request.isIgnoreWarnings()) {
                    // Trả về cảnh báo thay vì cập nhật ngay
                    StringBuilder warningMessage = new StringBuilder("CẢNH BÁO NGHIÊM TRỌNG KHI CẬP NHẬT:\n");
                    drugAnalysis.getWarnings().stream()
                            .filter(w -> "HIGH".equals(w.getSeverity()))
                            .forEach(w -> warningMessage.append("- ").append(w.getMessage()).append("\n"));

                    warningMessage.append("\nVui lòng xem xét lại đơn thuốc trước khi cập nhật.");

                    MedicalRecordResponse warningResponse = new MedicalRecordResponse(warningMessage.toString());
                    warningResponse.setDrugAnalysis(drugAnalysis);
                    return warningResponse;
                }
            }

            // 3. Tiếp tục cập nhật medical record
            MedicalRecordResponse result = updateMedicalRecord(id, request);

            // 4. Thêm thông tin phân tích thuốc nếu có
            if (result.isSuccess() && request.isCheckDrugInteractions()) {
                SmartPrescriptionResponse drugAnalysis = smartPrescriptionService.analyzePrescription(request);
                result.setDrugAnalysis(drugAnalysis);
            }

            return result;

        } catch (Exception e) {
            System.err.println("Error updating medical record with drug check: " + e.getMessage());
            return new MedicalRecordResponse("Lỗi khi cập nhật hồ sơ y tế với kiểm tra thuốc: " + e.getMessage());
        }
    }

    /**
     * Lấy báo cáo tương tác thuốc cho bệnh nhân
     */
    public SmartPrescriptionResponse getPatientDrugInteractionReport(Long patientId) {
        try {
            return smartPrescriptionService.analyzePatientMedicationHistory(patientId);
        } catch (Exception e) {
            SmartPrescriptionResponse errorResponse = new SmartPrescriptionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Lỗi khi tạo báo cáo tương tác thuốc: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Kiểm tra an toàn thuốc cho đơn thuốc cụ thể
     */
    public SmartPrescriptionResponse checkPrescriptionSafety(Long prescriptionId) {
        try {
            // Lấy thông tin prescription
            Prescription prescription = prescriptionRepository.findById(prescriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

            // Tạo request để kiểm tra
            MedicalRecordRequest request = new MedicalRecordRequest();
            request.setPatientId(prescription.getPatient().getId());
            request.setDoctorId(prescription.getDoctor().getId());

            List<PrescriptionDetailRequest> details = prescription.getDetails().stream()
                    .map(detail -> {
                        PrescriptionDetailRequest detailRequest = new PrescriptionDetailRequest();
                        detailRequest.setMedicationName(detail.getMedicationName());
                        detailRequest.setDosage(detail.getDosage());
                        detailRequest.setFrequency(detail.getFrequency());
                        detailRequest.setDuration(detail.getDuration());
                        detailRequest.setInstructions(detail.getInstructions());
                        return detailRequest;
                    })
                    .collect(Collectors.toList());

            request.setPrescriptionDetails(details);

            return smartPrescriptionService.analyzePrescription(request);

        } catch (Exception e) {
            SmartPrescriptionResponse errorResponse = new SmartPrescriptionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Lỗi khi kiểm tra an toàn đơn thuốc: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Lấy gợi ý thuốc cho chẩn đoán
     */
    public List<DrugSuggestionResponse> getDrugSuggestionsForDiagnosis(String diagnosis) {
        return smartPrescriptionService.suggestDrugsForDiagnosis(diagnosis);
    }

    /**
     * Tạo báo cáo tổng hợp về sử dụng thuốc của bệnh nhân
     */
    public MedicationSummaryResponse getPatientMedicationSummary(Long patientId) {
        try {
            List<MedicalRecord> records = medicalRecordRepository.findByPatientId(patientId);

            MedicationSummaryResponse summary = new MedicationSummaryResponse();
            summary.setPatientId(patientId);
            summary.setTotalPrescriptions(records.size());

            // Thu thập tất cả thuốc đã sử dụng
            Set<String> allMedications = new HashSet<>();
            Map<String, Integer> medicationFrequency = new HashMap<>();

            for (MedicalRecord record : records) {
                if (record.getPrescription() != null && record.getPrescription().getDetails() != null) {
                    for (PrescriptionDetail detail : record.getPrescription().getDetails()) {
                        String medication = detail.getMedicationName();
                        allMedications.add(medication);
                        medicationFrequency.put(medication, medicationFrequency.getOrDefault(medication, 0) + 1);
                    }
                }
            }

            summary.setUniqueMedications(allMedications.size());
            summary.setMostFrequentMedications(
                    medicationFrequency.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .limit(10)
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (e1, e2) -> e1,
                                    LinkedHashMap::new
                            ))
            );

            // Phân tích an toàn
            List<String> currentMedications = new ArrayList<>(allMedications);
            if (currentMedications.size() > 1) {
                DrugInteractionResponse interactionCheck = smartPrescriptionService.checkInteractions(currentMedications);
                summary.setHasInteractionRisk(interactionCheck.isHasInteractions());
                summary.setInteractionCount(interactionCheck.getInteractions().size());
            }

            return summary;

        } catch (Exception e) {
            MedicationSummaryResponse errorSummary = new MedicationSummaryResponse();
            errorSummary.setPatientId(patientId);
            errorSummary.setErrorMessage("Lỗi khi tạo báo cáo tổng hợp thuốc: " + e.getMessage());
            return errorSummary;
        }
    }

    // Inner class cho báo cáo tổng hợp
    public static class MedicationSummaryResponse {
        private Long patientId;
        private int totalPrescriptions;
        private int uniqueMedications;
        private Map<String, Integer> mostFrequentMedications;
        private boolean hasInteractionRisk;
        private int interactionCount;
        private String errorMessage;

        // Getters and setters
        public Long getPatientId() { return patientId; }
        public void setPatientId(Long patientId) { this.patientId = patientId; }

        public int getTotalPrescriptions() { return totalPrescriptions; }
        public void setTotalPrescriptions(int totalPrescriptions) { this.totalPrescriptions = totalPrescriptions; }

        public int getUniqueMedications() { return uniqueMedications; }
        public void setUniqueMedications(int uniqueMedications) { this.uniqueMedications = uniqueMedications; }

        public Map<String, Integer> getMostFrequentMedications() { return mostFrequentMedications; }
        public void setMostFrequentMedications(Map<String, Integer> mostFrequentMedications) { this.mostFrequentMedications = mostFrequentMedications; }

        public boolean isHasInteractionRisk() { return hasInteractionRisk; }
        public void setHasInteractionRisk(boolean hasInteractionRisk) { this.hasInteractionRisk = hasInteractionRisk; }

        public int getInteractionCount() { return interactionCount; }
        public void setInteractionCount(int interactionCount) { this.interactionCount = interactionCount; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
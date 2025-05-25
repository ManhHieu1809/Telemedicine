package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.MedicalRecordRequest;
import com.hospital.telemedicine.dto.response.MedicalRecordResponse;
import com.hospital.telemedicine.dto.response.PrescriptionDetailResponse;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {

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
}
package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.AppointmentRequest;
import com.hospital.telemedicine.dto.response.AppointmentResponse;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationRepository notificationRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorScheduleRepository doctorScheduleRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              NotificationRepository notificationRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationRepository = notificationRepository;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        try {
            // Debug log để kiểm tra userId nhận được
            System.out.println("User ID từ JWT: " + request.getPatientId());

            // Tìm patient bằng userId từ JWT token
            Patient patient = patientRepository.findByUserId(request.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân với user_id: " + request.getPatientId()));

            System.out.println("Tìm thấy bệnh nhân: " + patient.getId() + " - " + patient.getFullName());

            Doctor doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));

            // Kiểm tra lịch trống của bác sĩ
            LocalDate date = request.getDate();
            LocalTime time = request.getTime();
            DoctorSchedule.DayOfWeek dayOfWeek = DoctorSchedule.DayOfWeek.valueOf(
                    date.getDayOfWeek().name());
            List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndDayOfWeek(
                    request.getDoctorId(), dayOfWeek);

            boolean isAvailable = schedules.stream().anyMatch(schedule ->
                    !time.isBefore(schedule.getStartTime()) &&
                            !time.isAfter(schedule.getEndTime()));

            if (!isAvailable) {
                throw new IllegalArgumentException("Bác sĩ không có lịch trống vào thời gian này");
            }

            // Kiểm tra xung đột lịch hẹn
            LocalDateTime appointmentDateTime = LocalDateTime.of(date, time);
            LocalDateTime startWindow = appointmentDateTime.minusMinutes(30);
            LocalDateTime endWindow = appointmentDateTime.plusMinutes(30);
            List<Appointment> conflictingAppointments = appointmentRepository.findActiveAppointmentsByDoctorIdAndDateTimeBetween(
                    request.getDoctorId(), date, startWindow.toLocalTime(), endWindow.toLocalTime());
            if (!conflictingAppointments.isEmpty()) {
                throw new IllegalArgumentException("Bác sĩ đã có lịch hẹn vào thời gian này");
            }

            // Tạo lịch hẹn
            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctor(doctor);
            appointment.setDate(date);
            appointment.setTime(time);
            appointment.setStatus(Appointment.Status.PENDING);
            appointment.setAppointmentType(request.getAppointmentType());
            appointment.setCreatedAt(LocalDateTime.now());
            appointment = appointmentRepository.save(appointment);

            // Tạo thông báo
            createNotification(patient.getUser(), "Lịch hẹn mới được tạo vào " + date + " " + time);
            createNotification(doctor.getUser(), "Bạn có lịch hẹn mới vào " + date + " " + time);

            // Lên lịch thông báo nhắc nhở
            scheduleReminderNotifications(appointment);

            return mapToResponse(appointment, "Lịch hẹn đã được tạo thành công");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new AppointmentResponse(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new AppointmentResponse("Lỗi khi tạo lịch hẹn: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public List<AppointmentResponse> getAppointments(Long userId, String role) {
        try {
            List<AppointmentResponse> responses;
            if ("ADMIN".equals(role)) {
                responses = appointmentRepository.findAll().stream()
                        .map(appointment -> mapToResponse(appointment, "Lấy cuộc hẹn thành công"))
                        .collect(Collectors.toList());
            } else if ("DOCTOR".equals(role)) {
                responses = appointmentRepository.findByDoctorUserId(userId).stream()
                        .map(appointment -> mapToResponse(appointment, "Lấy cuộc hẹn thành công"))
                        .collect(Collectors.toList());
            } else {
                Patient patient = patientRepository.findByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân cho user_id:" + userId));
                Long patientId = patient.getId();
                System.out.println("Đang tìm kiếm cuộc hẹn với bệnh nhân: " + patientId);
                responses = appointmentRepository.findByPatientId(patientId).stream()
                        .map(appointment -> mapToResponse(appointment, "Lấy lại cuộc hẹn thành công"))
                        .collect(Collectors.toList());
            }
            return responses.isEmpty() ? List.of(new AppointmentResponse("Không tìm thấy cuộc hẹn nào")) : responses;
        } catch (IllegalArgumentException e) {
            return List.of(new AppointmentResponse(e.getMessage()));
        } catch (Exception e) {
            return List.of(new AppointmentResponse("Không thể lấy lại cuộc hẹn: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('PATIENT')")
    public List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date) {
        // Lấy danh sách lịch làm việc của bác sĩ trong ngày
        DoctorSchedule.DayOfWeek dayOfWeek = DoctorSchedule.DayOfWeek.valueOf(date.getDayOfWeek().name());
        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        if (schedules.isEmpty()) {
            return new ArrayList<>(); // Không có lịch làm việc, trả về danh sách rỗng
        }

        // Lấy danh sách cuộc hẹn đã có của bác sĩ trong ngày
        List<Appointment> appointments = appointmentRepository.findActiveAppointmentsByDoctorIdAndDate(doctorId, date);
        List<LocalTime> bookedSlots = appointments.stream()
                .map(Appointment::getTime)
                .collect(Collectors.toList());

        // Tạo danh sách tất cả các slot thời gian có thể trong khung giờ làm việc
        List<LocalTime> allSlots = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            LocalTime startTime = schedule.getStartTime();
            LocalTime endTime = schedule.getEndTime();
            while (startTime.isBefore(endTime)) {
                allSlots.add(startTime);
                startTime = startTime.plusMinutes(30); // Mỗi slot 30 phút
            }
        }

        // Loại bỏ các slot đã được đặt
        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('PATIENT')")
    @Transactional
    public AppointmentResponse updateAppointment(Long appointmentId, AppointmentRequest request) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hẹn"));
            if (!appointment.getStatus().equals(Appointment.Status.PENDING)) {
                throw new IllegalStateException("Cuộc hẹn không thể được cập nhật");
            }

            // Kiểm tra lịch trống và xung đột
            LocalDate date = request.getDate();
            LocalTime time = request.getTime();
            DoctorSchedule.DayOfWeek dayOfWeek = DoctorSchedule.DayOfWeek.valueOf(
                    date.getDayOfWeek().name());
            List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndDayOfWeek(
                    appointment.getDoctor().getId(), dayOfWeek);

            boolean isAvailable = schedules.stream().anyMatch(schedule ->
                    !time.isBefore(schedule.getStartTime()) &&
                            !time.isAfter(schedule.getEndTime()));

            if (!isAvailable) {
                throw new IllegalArgumentException("Bác sĩ không có lịch trống vào thời gian này");
            }

            LocalDateTime newDateTime = LocalDateTime.of(date, time);
            LocalDateTime startWindow = newDateTime.minusMinutes(30);
            LocalDateTime endWindow = newDateTime.plusMinutes(30);
            List<Appointment> conflictingAppointments = appointmentRepository.findActiveAppointmentsByDoctorIdAndDateTimeBetween(
                    appointment.getDoctor().getId(), date, startWindow.toLocalTime(), endWindow.toLocalTime());
            conflictingAppointments.removeIf(a -> a.getId().equals(appointmentId));
            if (!conflictingAppointments.isEmpty()) {
                throw new IllegalArgumentException("Bác sĩ đã có lịch hẹn vào thời gian này");
            }

            appointment.setDate(date);
            appointment.setTime(time);
            appointment.setAppointmentType(request.getAppointmentType());
            appointment = appointmentRepository.save(appointment);

            createNotification(appointment.getPatient().getUser(), "Lịch hẹn đã được cập nhật vào " + date + " " + time);
            createNotification(appointment.getDoctor().getUser(), "Lịch hẹn của bạn đã được cập nhật vào " + date + " " + time);

            return mapToResponse(appointment, "Lịch hẹn đã được cập nhật thành công");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new AppointmentResponse(e.getMessage());
        } catch (Exception e) {
            return new AppointmentResponse("Lịch hẹn cập nhật thất bại: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hẹn"));
            if (appointment.getStatus().equals(Appointment.Status.CANCELLED) ||
                    appointment.getStatus().equals(Appointment.Status.COMPLETED)) {
                throw new IllegalStateException("Cuộc hẹn không thể bị hủy");
            }

            appointment.setStatus(Appointment.Status.CANCELLED);
            appointment = appointmentRepository.save(appointment);

            createNotification(appointment.getPatient().getUser(), "Lịch hẹn vào " + appointment.getDate() + " " + appointment.getTime() + " đã bị hủy");
            createNotification(appointment.getDoctor().getUser(), "Lịch hẹn vào " + appointment.getDate() + " " + appointment.getTime() + " đã bị hủy");

            return mapToResponse(appointment, "Lịch hẹn đã được hủy thành công");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new AppointmentResponse(e.getMessage());
        } catch (Exception e) {
            return new AppointmentResponse("Không thể hủy cuộc hẹn: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public AppointmentResponse confirmAppointment(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hẹn"));
            if (!appointment.getStatus().equals(Appointment.Status.PENDING)) {
                throw new IllegalStateException("Chỉ có cuộc hẹn đang chờ xác nhận mới có thể được xác nhận");
            }

            appointment.setStatus(Appointment.Status.CONFIRMED);
            appointment = appointmentRepository.save(appointment);

            createNotification(appointment.getPatient().getUser(), "Lịch hẹn vào " + appointment.getDate() + " " + appointment.getTime() + " đã được xác nhận");

            return mapToResponse(appointment, "Lịch hẹn đã được xác nhận thành công");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new AppointmentResponse(e.getMessage());
        } catch (Exception e) {
            return new AppointmentResponse("Không xác nhận được cuộc hẹn: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hẹn"));
            if (!appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
                throw new IllegalStateException("Chỉ có cuộc hẹn đã xác nhận mới có thể được hoàn thành");
            }

            appointment.setStatus(Appointment.Status.COMPLETED);
            appointment = appointmentRepository.save(appointment);

            createNotification(appointment.getPatient().getUser(), "Lịch hẹn vào " + appointment.getDate() + " " + appointment.getTime() + " đã hoàn thành");

            return mapToResponse(appointment, "Lịch hẹn đã hoàn thành thành công");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new AppointmentResponse(e.getMessage());
        } catch (Exception e) {
            return new AppointmentResponse("Không hoàn tất được cuộc hẹn: " + e.getMessage());
        }
    }

    public List<AppointmentResponse> getAppointmentsByDoctorId(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorUserId(doctorId);
        return appointments.stream()
                .map(appointment -> mapToResponse(appointment, "Lấy cuộc hẹn thành công"))
                .collect(Collectors.toList());
    }

    private void createNotification(User user, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setStatus(Notification.Status.UNREAD);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void scheduleReminderNotifications(Appointment appointment) {
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointment.getDate(), appointment.getTime());
        LocalDateTime twentyFourHoursBefore = appointmentDateTime.minusHours(24);
        LocalDateTime oneHourBefore = appointmentDateTime.minusHours(1);

        if (twentyFourHoursBefore.isAfter(LocalDateTime.now())) {
            createNotification(appointment.getPatient().getUser(),
                    "Nhắc nhở: Lịch hẹn với bác sĩ vào " + appointment.getDate() + " " + appointment.getTime());
            createNotification(appointment.getDoctor().getUser(),
                    "Nhắc nhở: Lịch hẹn với bệnh nhân vào " + appointment.getDate() + " " + appointment.getTime());
        }
        if (oneHourBefore.isAfter(LocalDateTime.now())) {
            createNotification(appointment.getPatient().getUser(),
                    "Nhắc nhở: Lịch hẹn sắp đến vào " + appointment.getDate() + " " + appointment.getTime());
            createNotification(appointment.getDoctor().getUser(),
                    "Nhắc nhở: Lịch hẹn sắp đến vào " + appointment.getDate() + " " + appointment.getTime());
        }
    }

    private AppointmentResponse mapToResponse(Appointment appointment, String message) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                appointment.getDoctor().getUser().getAvatarUrl(),
                appointment.getDoctor().getSpecialty(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getStatus(),
                appointment.getAppointmentType(),
                appointment.getCreatedAt(),
                message
        );
    }
}
package com.hospital.telemedicine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.telemedicine.dto.request.*;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class UserService {
    private final DoctorRepository doctorRepository;
    private final FavoriteDoctorRepository favoriteDoctorRepository;
    private final ReviewRepository reviewRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final MessageRepository messageRepository;
    private final UserActivityRepository userActivityRepository;
    public UserService(DoctorRepository doctorRepository,
                       FavoriteDoctorRepository favoriteDoctorRepository,AppointmentRepository appointmentRepository,PaymentRepository paymentRepository,
                       UserActivityRepository userActivityRepository,MessageRepository messageRepository,ReviewRepository reviewRepository, PatientRepository patientRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.favoriteDoctorRepository = favoriteDoctorRepository;
        this.reviewRepository = reviewRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.messageRepository = messageRepository;
        this.userActivityRepository = userActivityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${imgbb.api.key}")
    private String apiKey;

    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + userId));

        // Gửi file đến ImgBB
        String imgBBUrl = uploadToImgBB(file, apiKey);
        user.setAvatarUrl(imgBBUrl);
        userRepository.save(user);
        return imgBBUrl;
    }

    private String uploadToImgBB(MultipartFile file, String apiKey) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.imgbb.com/1/upload?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        return jsonNode.get("data").get("url").asText();
    }

    public DoctorResponse getDoctorById(Long doctorId,Long patientId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));
        return mapToDoctorResponse(doctor, patientId);
    }

    // Lấy tất cả bác sĩ
    public List<DoctorResponse> getAllDoctors(Long patientId) {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors.stream()
                .map(doctor -> mapToDoctorResponse(doctor, patientId))
                .collect(Collectors.toList());
    }

    // Lấy bác sĩ theo chuyên khoa
    public List<DoctorResponse> getDoctorsBySpecialty(String specialty, Long patientId) {
        List<Doctor> doctors = doctorRepository.findBySpecialty(specialty);
        return doctors.stream()
                .map(doctor -> mapToDoctorResponse(doctor, patientId))
                .collect(Collectors.toList());
    }



    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if(request.getUsername() != null) user.setUsername(request.getUsername());
        userRepository.save(user);

        if (user.getRoles() == User.UserRole.PATIENT) {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân"));

            if (request.getFullName() != null) patient.setFullName(request.getFullName());
            if (request.getPhone() != null) patient.setPhone(request.getPhone());
            if (request.getAddress() != null) patient.setAddress(request.getAddress());
            if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
            if (request.getGender() != null) patient.setGender(Patient.Gender.valueOf(request.getGender()));
            patientRepository.save(patient);
        } else if (user.getRoles() == User.UserRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));

            if (request.getFullName() != null) doctor.setFullName(request.getFullName());
            if (request.getPhone() != null) doctor.setPhone(request.getPhone());
            if (request.getAddress() != null) doctor.setAddress(request.getAddress());
            if (request.getSpecialty() != null) doctor.setSpecialty(request.getSpecialty());
            if (request.getExperience() != null) doctor.setExperience(request.getExperience());
            doctorRepository.save(doctor);
        }
    }

    public List<TopDoctorResponse> getTopDoctors(Long patientId) {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors.stream()
                .map(doctor -> {
                    TopDoctorResponse response = new TopDoctorResponse();
                    response.setId(doctor.getId());
                    response.setDoctorUserId(doctor.getUser().getId());
                    response.setFullName(doctor.getFullName());
                    response.setAvatarUrl(doctor.getUser().getAvatarUrl());
                    response.setSpecialty(doctor.getSpecialty());
                    List<Reviews> reviews = reviewRepository.findByDoctorId(doctor.getId());
                    response.setTotalReviews(reviews.size());
                    double averageRating = reviews.stream()
                            .mapToInt(Reviews::getRating)
                            .average()
                            .orElse(0.0);
                    response.setAverageRating(Math.round(averageRating * 10.0) / 10.0);

                    // Kiểm tra isFavorite
                    if (patientId != null) {
                        Patient patient = patientRepository.findByUserId(patientId).orElse(null);
                        if (patient != null) {
                            response.setFavorite(favoriteDoctorRepository.findByPatientIdAndDoctorId(patient.getId(), doctor.getId()).isPresent());
                        } else {
                            response.setFavorite(false);
                        }
                    } else {
                        response.setFavorite(false);
                    }

                    return response;
                })
                .filter(response -> response.getTotalReviews() > 0)
                .sorted(Comparator.comparing(TopDoctorResponse::getAverageRating, Comparator.reverseOrder())
                        .thenComparing(TopDoctorResponse::getTotalReviews, Comparator.reverseOrder()))
                .limit(3)
                .collect(Collectors.toList());
    }


    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        UserResponse response = new UserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRoles().name());

        if (user.getRoles() == User.UserRole.PATIENT) {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân"));
            response.setFullName(patient.getFullName());
            response.setPhone(patient.getPhone());
            response.setAddress(patient.getAddress());
            response.setDateOfBirth(patient.getDateOfBirth());
            response.setGender(patient.getGender().name());
        } else if (user.getRoles() == User.UserRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));
            response.setFullName(doctor.getFullName());
            response.setPhone(doctor.getPhone());
            response.setAddress(doctor.getAddress());
            response.setSpecialty(doctor.getSpecialty());
            response.setExperience(doctor.getExperience());
        }

        return response;
    }

    public UserResponse getUserById(Long requesterId, Long targetUserId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người yêu cầu"));

        if (requester.getRoles() != User.UserRole.DOCTOR && requester.getRoles() != User.UserRole.ADMIN) {
            throw new AccessDeniedException("Chỉ bác sĩ hoặc quản trị viên có thể truy cập thông tin người dùng này");
        }

        return getUserProfile(targetUserId);
    }


    public List<FavoriteDoctorResponse> getFavoriteDoctors(Long patientId) {
        List<FavoriteDoctor> favorites = favoriteDoctorRepository.findByPatientId(patientId);
        return favorites.stream()
                .map(favorite -> mapToFavoriteDoctorResponse(favorite.getDoctor()))
                .collect(Collectors.toList());
    }

    public DoctorDetailsResponse getDoctorDetails(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));

        DoctorDetailsResponse response = new DoctorDetailsResponse();
        response.setDoctorId(doctor.getId());
        response.setFullName(doctor.getFullName());
        response.setAvatarUrl(doctor.getUser().getAvatarUrl());
        response.setSpecialty(doctor.getSpecialty());
        response.setExperience(doctor.getExperience());
        response.setPhone(doctor.getPhone());
        response.setAddress(doctor.getAddress());

        // Lấy các review của bác sĩ
        List<Reviews> reviews = reviewRepository.findByDoctorId(doctorId);
        response.setTotalReviews(reviews.size()); // Tổng số đánh giá

        // Tính toán đánh giá trung bình
        double averageRating = reviews.stream()
                .mapToInt(Reviews::getRating)
                .average()
                .orElse(0.0);
        response.setAverageRating(Math.round(averageRating * 10.0) / 10.0);

        List<DoctorDetailsResponse.ReviewInfo> reviewInfos = reviews.stream()
                .map(review -> {
                    DoctorDetailsResponse.ReviewInfo reviewInfo = new DoctorDetailsResponse.ReviewInfo();
                    reviewInfo.setReviewId(review.getId());
                    reviewInfo.setPatientId(review.getPatient().getId());
                    reviewInfo.setPatientName(review.getPatient().getFullName());
                    reviewInfo.setAvatarUrl(review.getPatient().getUser().getAvatarUrl());
                    reviewInfo.setRating(review.getRating());
                    reviewInfo.setComment(review.getComment());
                    reviewInfo.setCreatedAt(review.getCreatedAt());
                    return reviewInfo;
                })
                .collect(Collectors.toList());

        response.setReviews(reviewInfos);
        return response;
    }

    // Quản lý người dùng: Thêm bệnh nhân
    public void createPatient(CreatePatientRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent() ||
                userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Tài khoản hoặc email đã tồn tại");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(User.UserRole.PATIENT);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(Patient.Gender.valueOf(request.getGender()));
        patient.setPhone(request.getPhone());
        patient.setAddress(request.getAddress());
        patientRepository.save(patient);

        logActivity(user, "REGISTER", "Tài khoản bệnh nhân được tạo bởi admin");
    }

    // Quản lý người dùng: Chỉnh sửa bệnh nhân
    public void updatePatient(Long patientId, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân"));
        User user = patient.getUser();

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Tên tài khoản đã tồn tại");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) patient.setFullName(request.getFullName());
        if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) patient.setGender(Patient.Gender.valueOf(request.getGender()));
        if (request.getPhone() != null) patient.setPhone(request.getPhone());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());

        userRepository.save(user);
        patientRepository.save(patient);

        logActivity(user, "UPDATE_PROFILE", "Thông tin bệnh nhân được cập nhật bởi admin");
    }

    // Quản lý người dùng: Xóa bệnh nhân
    public void deletePatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân"));
        User user = patient.getUser();

        patientRepository.delete(patient);
        userRepository.delete(user);

        logActivity(user, "DELETE_ACCOUNT", "Tài khoản bệnh nhân bị xóa bởi admin");
    }

    // Quản lý người dùng: Chỉnh sửa bác sĩ
    public void updateDoctor(Long doctorId, UpdateDoctorRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));
        User user = doctor.getUser();

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Tên tài khoản đã tồn tại");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) doctor.setFullName(request.getFullName());
        if (request.getSpecialty() != null) doctor.setSpecialty(request.getSpecialty());
        if (request.getExperience() != null) doctor.setExperience(request.getExperience());
        if (request.getPhone() != null) doctor.setPhone(request.getPhone());
        if (request.getAddress() != null) doctor.setAddress(request.getAddress());

        userRepository.save(user);
        doctorRepository.save(doctor);

        logActivity(user, "UPDATE_PROFILE", "Thông tin bác sĩ được cập nhật bởi admin");
    }

    // Quản lý người dùng: Xóa bác sĩ
    public void deleteDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));
        User user = doctor.getUser();

        doctorRepository.delete(doctor);
        userRepository.delete(user);

        logActivity(user, "DELETE_ACCOUNT", "Tài khoản bác sĩ bị xóa bởi admin");
    }

    // Giám sát hệ thống: Báo cáo
    public SystemReportResponse getSystemReport() {
        SystemReportResponse report = new SystemReportResponse();
        report.setTotalAppointments(appointmentRepository.count());
        report.setTotalRevenue(paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == Payment.PaymentStatus.COMPLETED)
                .mapToDouble(payment -> payment.getAmount().doubleValue())
                .sum());

        List<SystemReportResponse.DoctorRating> doctorRatings = doctorRepository.findAll()
                .stream()
                .map(doctor -> {
                    SystemReportResponse.DoctorRating rating = new SystemReportResponse.DoctorRating();
                    rating.setDoctorId(doctor.getId());
                    rating.setFullName(doctor.getFullName());
                    List<Reviews> reviews = reviewRepository.findByDoctorId(doctor.getId());
                    rating.setTotalReviews(reviews.size());
                    rating.setAverageRating(reviews.stream()
                            .mapToInt(Reviews::getRating)
                            .average()
                            .orElse(0.0));
                    return rating;
                })
                .collect(Collectors.toList());

        report.setDoctorRatings(doctorRatings);
        return report;
    }



    // Giám sát hệ thống: Theo dõi hoạt động
    public List<UserActivityResponse> getUserActivities() {
        return userActivityRepository.findAll()
                .stream()
                .map(activity -> {
                    UserActivityResponse response = new UserActivityResponse();
                    response.setUserId(activity.getUser().getId());
                    response.setUsername(activity.getUser().getUsername());
                    response.setActivityType(activity.getActivityType());
                    response.setDescription(activity.getDescription());
                    response.setTimestamp(activity.getTimestamp());
                    return response;
                })
                .collect(Collectors.toList());
    }

    // Kiểm duyệt nội dung: Xóa đánh giá
    public void deleteReview(Long reviewId) {
        Reviews review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        reviewRepository.delete(review);
    }

    // Kiểm duyệt nội dung: Chỉnh sửa đánh giá
    public void updateReview(Long reviewId, UpdateReviewRequest request) {
        Reviews review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewRepository.save(review);
    }

    // Kiểm duyệt nội dung: Xóa tin nhắn
    public void deleteMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn"));
        messageRepository.delete(message);
    }

    // Kiểm duyệt nội dung: Chỉnh sửa tin nhắn
    public void updateMessage(Long messageId, UpdateMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn"));
        message.setContent(request.getContent());
        messageRepository.save(message);
    }


    @Transactional
    public void logActivity(User user, String activityType, String description) {
        UserActivity activity = new UserActivity();
        activity.setUser(user);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setTimestamp(LocalDateTime.now());
        userActivityRepository.save(activity);
    }


    private DoctorResponse mapToDoctorResponse(Doctor doctor, Long patientId) {
        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setUserId(doctor.getUser().getId());
        response.setFullName(doctor.getFullName());
        response.setEmail(doctor.getUser().getEmail());
        response.setAvatarUrl(doctor.getUser().getAvatarUrl());
        response.setSpecialty(doctor.getSpecialty());
        response.setExperience(doctor.getExperience());
        response.setPhone(doctor.getPhone());
        response.setAddress(doctor.getAddress());

        // Lấy và tính toán số sao trung bình từ các review
        List<Reviews> reviews = reviewRepository.findByDoctorId(doctor.getId());
        response.setTotalReviews(reviews.size());
        double averageRating = reviews.stream()
                .mapToInt(Reviews::getRating)
                .average()
                .orElse(0.0);
        response.setAverageRating(Math.round(averageRating * 10.0) / 10.0);


        // Kiểm tra xem bác sĩ có trong danh sách yêu thích của bệnh nhân không
        if (patientId != null) {
            Patient patient = patientRepository.findByUserId(patientId)
                    .orElse(null);
            if (patient != null) {
                response.setFavorite(favoriteDoctorRepository.findByPatientIdAndDoctorId(patient.getId(), doctor.getId()).isPresent());
            } else {
                response.setFavorite(false);
            }
        } else {
            response.setFavorite(false);
        }

        return response;
    }

    private FavoriteDoctorResponse mapToFavoriteDoctorResponse(Doctor doctor) {
        FavoriteDoctorResponse response = new FavoriteDoctorResponse();
        response.setDoctorUserId(doctor.getId());
        response.setDoctorId(doctor.getId());
        response.setDoctorName(doctor.getFullName());
        response.setAvatarUrl(doctor.getUser().getAvatarUrl());
        response.setSpecialty(doctor.getSpecialty());

        List<Reviews> reviews = reviewRepository.findByDoctorId(doctor.getId());
        response.setTotalReviews(reviews.size());
        double averageRating = reviews.stream()
                .mapToInt(Reviews::getRating)
                .average()
                .orElse(0.0);
        response.setAverageRating(Math.round(averageRating * 10.0) / 10.0);

        return response;
    }

    // Thêm vào UserService.java

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public List<PatientResponse> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
                .map(this::convertToPatientResponse)
                .collect(Collectors.toList());
    }

    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalDoctors = doctorRepository.count();
        long totalPatients = patientRepository.count();
        long totalAppointments = appointmentRepository.count();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalDoctors(totalDoctors)
                .totalPatients(totalPatients)
                .totalAppointments(totalAppointments)
                .build();
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRoles().name());

        if (user.getRoles() == User.UserRole.PATIENT) {
            Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
            if (patient != null) {
                response.setFullName(patient.getFullName());
                response.setPhone(patient.getPhone());
                response.setAddress(patient.getAddress());
                response.setDateOfBirth(patient.getDateOfBirth());
                response.setGender(patient.getGender() != null ? patient.getGender().name() : null);
            }
        } else if (user.getRoles() == User.UserRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(user.getId()).orElse(null);
            if (doctor != null) {
                response.setFullName(doctor.getFullName());
                response.setPhone(doctor.getPhone());
                response.setAddress(doctor.getAddress());
                response.setSpecialty(doctor.getSpecialty());
                response.setExperience(doctor.getExperience());
            }
        }

        return response;
    }


    private PatientResponse convertToPatientResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .fullName(patient.getFullName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender() != null ? patient.getGender().name() : null)
                .phone(patient.getPhone())
                .address(patient.getAddress())
                .build();
    }


}

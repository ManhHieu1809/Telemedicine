package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.*;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private static final String UPLOAD_DIR = "uploads/avatars/";
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

    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + userId));

        // Tạo thư mục uploads nếu chưa tồn tại
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Lưu file với tên duy nhất
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Cập nhật avatar_url
        String avatarUrl = "/uploads/avatars/" + fileName;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    public DoctorResponse getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));
        return mapToDoctorResponse(doctor);
    }

    public List<DoctorResponse> getDoctorsBySpecialty(String specialty) {
        List<Doctor> doctors = doctorRepository.findBySpecialty(specialty);
        return doctors.stream()
                .map(this::mapToDoctorResponse)
                .collect(Collectors.toList());
    }

    public List<DoctorResponse> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors.stream()
                .map(this::mapToDoctorResponse)
                .collect(Collectors.toList());
    }

    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

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

    public List<TopDoctorResponse> getTopDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors.stream()
                .map(doctor -> {
                    TopDoctorResponse response = new TopDoctorResponse();
                    response.setId(doctor.getId());
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
        response.setSpecialty(doctor.getSpecialty());
        response.setExperience(doctor.getExperience());
        response.setPhone(doctor.getPhone());
        response.setAddress(doctor.getAddress());

        // Lấy các review của bác sĩ
        List<Reviews> reviews = reviewRepository.findByDoctorId(doctorId);
        response.setTotalReviews(reviews.size()); // Tổng số đánh giá

        List<DoctorDetailsResponse.ReviewInfo> reviewInfos = reviews.stream()
                .map(review -> {
                    DoctorDetailsResponse.ReviewInfo reviewInfo = new DoctorDetailsResponse.ReviewInfo();
                    reviewInfo.setReviewId(review.getId());
                    reviewInfo.setPatientId(review.getPatient().getId());
                    reviewInfo.setPatientName(review.getPatient().getFullName());
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


    private DoctorResponse mapToDoctorResponse(Doctor doctor) {
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
}

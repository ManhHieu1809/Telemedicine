package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.UpdateUserRequest;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
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

    public UserService(DoctorRepository doctorRepository,
                       FavoriteDoctorRepository favoriteDoctorRepository,
                       ReviewRepository reviewRepository,PatientRepository patientRepository,UserRepository userRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.favoriteDoctorRepository = favoriteDoctorRepository;
        this.reviewRepository = reviewRepository;
        this.patientRepository = patientRepository;
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

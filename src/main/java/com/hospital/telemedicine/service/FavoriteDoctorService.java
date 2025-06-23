package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.response.FavoriteDoctorResponse;
import com.hospital.telemedicine.entity.Doctor;
import com.hospital.telemedicine.entity.FavoriteDoctor;
import com.hospital.telemedicine.entity.Patient;
import com.hospital.telemedicine.entity.Reviews;
import com.hospital.telemedicine.repository.DoctorRepository;
import com.hospital.telemedicine.repository.FavoriteDoctorRepository;
import com.hospital.telemedicine.repository.PatientRepository;
import com.hospital.telemedicine.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteDoctorService {

    private final FavoriteDoctorRepository favoriteDoctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    private ReviewRepository reviewRepository;
    public FavoriteDoctorService(FavoriteDoctorRepository favoriteDoctorRepository,
                                 PatientRepository patientRepository,
                                 DoctorRepository doctorRepository) {
        this.favoriteDoctorRepository = favoriteDoctorRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public FavoriteDoctorResponse addFavoriteDoctor(Long userId, Long doctorUserId) {
        try {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found for user_id: " + userId));
            Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found for user_id: " + doctorUserId));

            if (favoriteDoctorRepository.findByPatientIdAndDoctorId(patient.getId(), doctor.getId()).isPresent()) {
                throw new IllegalStateException("Doctor is already in favorites");
            }

            FavoriteDoctor favorite = new FavoriteDoctor();
            favorite.setPatient(patient);
            favorite.setDoctor(doctor);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteDoctorRepository.save(favorite);


            // Tính toán averageRating và totalReviews
            List<Reviews> reviews = reviewRepository.findByDoctorId(doctor.getId());
            double averageRating = reviews.stream()
                    .mapToInt(Reviews::getRating)
                    .average()
                    .orElse(0.0);

            // Lấy avatarUrl
            String avatarUrl = (doctor.getUser() != null && doctor.getUser().getAvatarUrl() != null)
                    ? doctor.getUser().getAvatarUrl()
                    : "";

            // Trả về phản hồi thành công
            return new FavoriteDoctorResponse(
                    doctor.getId(),
                    doctor.getUser().getId(),
                    doctor.getFullName(),
                    doctor.getSpecialty(),
                    avatarUrl,
                    Math.round(averageRating * 10.0) / 10.0,
                    reviews.size(),
                    favorite.getCreatedAt()
            ); // success = true trong constructor
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Trả về phản hồi thất bại
            return new FavoriteDoctorResponse(); // success = false
        }
    }

    @Transactional
    public FavoriteDoctorResponse removeFavoriteDoctor(Long userId, Long doctorUserId) {
        try {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found for user_id: " + userId));
            Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found for user_id: " + doctorUserId));

            FavoriteDoctor favorite = favoriteDoctorRepository.findByPatientIdAndDoctorId(patient.getId(), doctor.getId())
                    .orElseThrow(() -> new IllegalStateException("Doctor is not in favorites"));
            favoriteDoctorRepository.delete(favorite);

            // Tính toán averageRating và totalReviews
            List<Reviews> reviews = reviewRepository.findByDoctorId(doctor.getId());
            double averageRating = reviews.stream()
                    .mapToInt(Reviews::getRating)
                    .average()
                    .orElse(0.0);

            // Lấy avatarUrl
            String avatarUrl = (doctor.getUser() != null && doctor.getUser().getAvatarUrl() != null)
                    ? doctor.getUser().getAvatarUrl()
                    : "";

            // Trả về phản hồi thành công
            return new FavoriteDoctorResponse(
                    doctor.getId(),
                    doctor.getUser().getId(),
                    doctor.getFullName(),
                    doctor.getSpecialty(),
                    avatarUrl,
                    Math.round(averageRating * 10.0) / 10.0,
                    reviews.size(),
                    favorite.getCreatedAt()
            ); // success = true trong constructor
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Trả về phản hồi thất bại
            return new FavoriteDoctorResponse(); // success = false
        }
    }

    public List<FavoriteDoctorResponse> getFavoriteDoctors(Long userId) {
        try {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found for user_id: " + userId));
            return favoriteDoctorRepository.findByPatientId(patient.getId()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            // success = true được đặt trong mapToResponse
        } catch (IllegalArgumentException e) {
            return List.of(new FavoriteDoctorResponse());
        }
    }

    private FavoriteDoctorResponse mapToResponse(FavoriteDoctor favorite) {
        List<Reviews> reviews = reviewRepository.findByDoctorId(favorite.getDoctor().getId());
        double averageRating = reviews.stream()
                .mapToInt(Reviews::getRating)
                .average()
                .orElse(0.0);

        return new FavoriteDoctorResponse(
                favorite.getDoctor().getId(),
                favorite.getDoctor().getUser().getId(),
                favorite.getDoctor().getFullName(),
                favorite.getDoctor().getSpecialty(),
                favorite.getDoctor().getUser().getAvatarUrl(), // Thêm avatarUrl
                Math.round(averageRating * 10.0) / 10.0, // Số sao trung bình
                reviews.size(), // Tổng số đánh giá
                favorite.getCreatedAt()
        ); // success = true trong constructor
    }
}
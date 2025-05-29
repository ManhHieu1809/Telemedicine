package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.ReviewRequest;
import com.hospital.telemedicine.dto.response.ReviewResponse;
import com.hospital.telemedicine.entity.Appointment;
import com.hospital.telemedicine.entity.Reviews;
import com.hospital.telemedicine.repository.AppointmentRepository;
import com.hospital.telemedicine.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         AppointmentRepository appointmentRepository) {
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public ReviewResponse addReview(Long patientId, ReviewRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn"));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("Bạn không có quyền đánh giá lịch hẹn này");
        }

        if (!appointment.getStatus().equals(Appointment.Status.COMPLETED)) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá lịch hẹn đã hoàn thành");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }

        Reviews review = new Reviews();
        review.setPatient(appointment.getPatient());
        review.setDoctor(appointment.getDoctor());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());

        Reviews savedReview = reviewRepository.save(review);
        return mapToReviewResponse(savedReview);
    }


    public List<ReviewResponse> getReviewsByDoctorId(Long doctorId) {
        List<Reviews> reviews = reviewRepository.findByDoctorId(doctorId);
        return reviews.stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse mapToReviewResponse(Reviews review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setDoctorId(review.getDoctor().getId());
        response.setPatientId(review.getPatient().getId());
        response.setPatientName(review.getPatient().getFullName());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
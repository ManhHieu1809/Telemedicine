package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.ChangePasswordRequest;
import com.hospital.telemedicine.dto.request.LoginRequest;
import com.hospital.telemedicine.dto.request.RegisterRequest;
import com.hospital.telemedicine.dto.response.AuthResponse;
import com.hospital.telemedicine.dto.response.MessageResponse;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import com.hospital.telemedicine.security.JwtTokenProvider;
import com.hospital.telemedicine.security.UserDetailsImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.AuthenticationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserStatusRepository userStatusRepository;

    public AuthResponse authenticateUser(LoginRequest loginRequest) {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Lấy User entity từ repository
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Ghi log hoạt động đăng nhập
            userService.logActivity(user, "LOGIN", "Người dùng đăng nhập thành công");

            return new AuthResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getAuthorities().toString().replace("[ROLE_", "").replace("]", ""),
                    userDetails.getAvatarUrl()
            );

    }

    @Transactional
    public MessageResponse registerUser(RegisterRequest registerRequest) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return new MessageResponse("Lỗi: Username đã được sử dụng!",false);
        }

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new MessageResponse("Lỗi: Email đã được sử dụng!",false);
        }

        // Tạo tài khoản mới - Mặc định role là PATIENT
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(encoder.encode(registerRequest.getPassword()));
        user.setRoles(User.UserRole.PATIENT); // Mặc định là PATIENT
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Tạo hồ sơ bệnh nhân
        Patient patient = new Patient();
        patient.setUser(savedUser);


        patientRepository.save(patient);

        return new MessageResponse("Đăng ký tài khoản thành công!", true);
    }

    // Tạo tài khoản bác sĩ mới - chỉ admin mới được phép thực hiện
    @Transactional
    public MessageResponse createDoctorAccount(String username, String email, String password,
                                               String fullName, String specialty, Integer experience,
                                               String phone, String address) {
        // Kiểm tra xem người dùng hiện tại có phải là ADMIN hay không
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new MessageResponse("Lỗi: Bạn không có quyền tạo tài khoản bác sĩ!",false);
        }

        // Kiểm tra username và email đã tồn tại chưa
        if (userRepository.existsByUsername(username)) {
            return new MessageResponse("Lỗi: Username đã được sử dụng!",false);
        }

        if (userRepository.existsByEmail(email)) {
            return new MessageResponse("Lỗi: Email đã được sử dụng!",false);
        }

        // Tạo tài khoản bác sĩ
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRoles(User.UserRole.DOCTOR);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Tạo hồ sơ bác sĩ
        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setFullName(fullName);
        doctor.setSpecialty(specialty);
        doctor.setExperience(experience);
        doctor.setPhone(phone);
        doctor.setAddress(address);

        doctorRepository.save(doctor);
        return new MessageResponse("Tạo tài khoản bác sĩ thành công!",true);
    }


    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository tokenRepository,
                       JavaMailSender mailSender,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MessageResponse requestPasswordReset(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));

            // Xóa token cũ (nếu có)
            tokenRepository.deleteByUserId(user.getId());

            // Tạo OTP
            String otp = generateOTP();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setToken(otp);
            token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
            token.setCreatedAt(LocalDateTime.now());
            tokenRepository.save(token);

            // Gửi email
            sendOTPEmail(user.getEmail(), otp);

            return new MessageResponse("Yêu cầu đặt lại mật khẩu thành công! Vui lòng kiểm tra email.", true);
        } catch (IllegalArgumentException e) {
            return new MessageResponse("Lỗi: " + e.getMessage(), false);
        } catch (MessagingException e) {
            return new MessageResponse("Lỗi: Không thể gửi email. Vui lòng thử lại sau.", false);
        } catch (Exception e) {
            return new MessageResponse("Lỗi hệ thống: " + e.getMessage(), false);
        }
    }

    public MessageResponse verifyOTP(String otp) {
        try {
            // Tìm token bằng OTP
            PasswordResetToken token = tokenRepository.findByToken(otp)
                    .orElseThrow(() -> new IllegalArgumentException("OTP không hợp lệ"));

            // Kiểm tra thời hạn OTP
            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("OTP đã hết hạn");
            }

            return new MessageResponse("Xác thực OTP thành công!", true);
        } catch (IllegalArgumentException e) {
            return new MessageResponse("Lỗi: " + e.getMessage(), false);
        } catch (IllegalStateException e) {
            return new MessageResponse("Lỗi: " + e.getMessage(), false);
        } catch (Exception e) {
            return new MessageResponse("Lỗi hệ thống: " + e.getMessage(), false);
        }
    }

    @Transactional
    public void resetPassword(String otp, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(otp)
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP has expired");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa token sau khi đổi mật khẩu
        tokenRepository.delete(token);
    }

    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // OTP 6 chữ số
        return String.valueOf(otp);
    }

    private void sendOTPEmail(String to, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject("Password Reset OTP");
        helper.setText("Your OTP for password reset is: <b>" + otp + "</b>. It is valid for 10 minutes.", true);
        mailSender.send(message);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }

        if (request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}

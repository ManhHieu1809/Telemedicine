package com.hospital.telemedicine.security;

import com.hospital.telemedicine.entity.User;
import com.hospital.telemedicine.repository.PatientRepository;
import com.hospital.telemedicine.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email không tồn tại: " + email));
        Long patientId = null;
        if (user.getRoles().name().equals("PATIENT")) {
            patientId = patientRepository.findByUserId(user.getId())
                    .map(patient -> patient.getId())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy bệnh nhân cho người dùng: " + email));
        }
        return UserDetailsImpl.build(user,patientId);
    }
}


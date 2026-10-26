package com.alumni.management.security.CustomUserDetailsService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String lookupEmail = email != null ? email.trim() : "";
        User user = userRepository.findByEmailIgnoreCase(lookupEmail)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isVerified(),
                true,
                true,
                true,
                Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName())
                ));
    }
}

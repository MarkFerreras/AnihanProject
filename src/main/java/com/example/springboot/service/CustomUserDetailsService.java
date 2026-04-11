package com.example.springboot.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by username OR email.
     * Spring Security calls this method with whatever the user typed in
     * the "username" field. We query both columns so the user can log in
     * with either their username or their email address.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + username));

        // Enforce exact case-sensitivity for username. 
        // If it's an email login, allow it to remain case-insensitive.
        boolean isExactUsernameMatch = user.getUsername().equals(username);
        boolean isEmailMatch = user.getEmail().equalsIgnoreCase(username);

        if (!isExactUsernameMatch && !isEmailMatch) {
            throw new UsernameNotFoundException("Invalid username or password");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),  // enabled — soft-deleted users are blocked
                true,               // accountNonExpired
                true,               // credentialsNonExpired
                true,               // accountNonLocked
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}

package com.obs.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obs.entity.Role;
import com.obs.entity.User;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.response.MessageResponse;
import com.obs.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @PostMapping("/create-banker")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBanker(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setFullName(signUpRequest.getFullName());

        Set<Role> roles = new HashSet<>();
        roles.add(Role.BANKER);
        user.setRoles(roles);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Banker registered successfully!"));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<User>> getAllUsers() {
        java.util.List<User> users = userRepository.findAll().stream()
                .filter(user -> !user.getRoles().contains(Role.ADMIN))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent deactivating own account if admin (optional, but good practice)
        // For now, allowing it as per requirement.
        
        user.setActive(!user.isActive());
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("User " + (user.isActive() ? "activated" : "deactivated") + " successfully!"));
    }


}

package com.obs.controller;

import com.obs.entity.User;
import com.obs.payload.request.UpdateUserRequest;
import com.obs.payload.response.MessageResponse;
import com.obs.repository.UserRepository;
import com.obs.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateUserRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.obs.exception.ResourceNotFoundException("Error: User not found."));

        if (!user.getEmail().equals(updateRequest.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new com.obs.exception.ConflictException("Error: Email is already in use!");
            }
        }

        user.setFullName(updateRequest.getFullName());
        user.setEmail(updateRequest.getEmail());
        user.setPhoneNumber(updateRequest.getPhoneNumber());

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));
    }
}

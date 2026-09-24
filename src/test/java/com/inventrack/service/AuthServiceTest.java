package com.inventrack.service;

import com.inventrack.dto.request.LoginRequest;
import com.inventrack.dto.request.RegisterRequest;
import com.inventrack.dto.response.AuthResponse;
import com.inventrack.entity.Role;
import com.inventrack.entity.User;
import com.inventrack.exception.DuplicateResourceException;
import com.inventrack.repository.UserRepository;
import com.inventrack.security.JwtTokenProvider;
import com.inventrack.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User staffUser;

    @BeforeEach
    void setUp() {
        staffUser = User.builder()
                .id(1L)
                .username("john_staff")
                .password("encoded_pass")
                .role(Role.STAFF)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user with STAFF role")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("john_staff", "password123");

        when(userRepository.existsByUsername("john_staff")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(staffUser);
        when(tokenProvider.generateToken(staffUser)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("john_staff");
        assertThat(response.getRole()).isEqualTo(Role.STAFF);
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when username is already taken")
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest("john_staff", "password123");

        when(userRepository.existsByUsername("john_staff")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully authenticate user and return JWT token")
    void login_Success() {
        LoginRequest request = new LoginRequest("john_staff", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(staffUser.getUsername(), null));
        when(userRepository.findByUsername("john_staff")).thenReturn(Optional.of(staffUser));
        when(tokenProvider.generateToken(staffUser)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("john_staff");
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password is invalid")
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("john_staff", "wrong_pass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}

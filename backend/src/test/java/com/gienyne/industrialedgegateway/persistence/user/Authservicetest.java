package com.gienyne.industrialedgegateway.persistence.user;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


import com.gienyne.industrialedgegateway.persistence.user.domain.DashboardUser;
import com.gienyne.industrialedgegateway.persistence.user.repository.DashboardUserRepository;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;



/**
 * Tests of the password change. Login and logout need a real HTTP session,
 * so they are covered by the integration tests, not here.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest{

    private static final String USER = "USR-TEST";
    private static final String OLD_HASH = "old-hash";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CookieCsrfTokenRepository csrfTokenRepository;

    @Mock
    private DashboardUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;


    @BeforeEach
    void setUp(){

        authService = new AuthService(authenticationManager, csrfTokenRepository, userRepository, passwordEncoder);

    }


    private DashboardUser stubExistingUser(){

        DashboardUser user = new DashboardUser(USER, OLD_HASH);
        when(userRepository.findById(USER)).thenReturn(Optional.of(user));

        return user;
    
    }



    @Test
    void changePassword_withTheRightCurrentPassword_storesTheNewHash(){

        DashboardUser user = stubExistingUser();

        when(passwordEncoder.matches("current-pass", OLD_HASH)).thenReturn(true);

        when(passwordEncoder.matches("new-password-1", OLD_HASH)).thenReturn(false);

        when(passwordEncoder.encode("new-password-1")).thenReturn("new-hash");

        authService.changePassword(USER, "current-pass", "new-password-1");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");

        verify(userRepository).save(user);

    }


    @Test
    void changePassword_withAWrongCurrentPassword_isRejected(){

        DashboardUser user = stubExistingUser();

        when(passwordEncoder.matches("wrong-pass", OLD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(USER, "wrong-pass", "new-password-1"))
            .isInstanceOf(PasswordChangeException.class)
            .hasMessageContaining("current password is incorrect");

        assertThat(user.getPasswordHash()).isEqualTo(OLD_HASH);

        verify(userRepository, never()).save(user);

    }


    @Test
    void changePassword_withATooShortNewPassword_isRejectedBeforeAnythingElse(){

        assertThatThrownBy(() -> authService.changePassword(USER, "current-pass", "short"))
            .isInstanceOf(PasswordChangeException.class)
            .hasMessageContaining("at least 8 characters");

        verifyNoInteractions(userRepository);

    }


    @Test
    void changePassword_withATooLongNewPassword_isRejected(){

        String tooLong = "a".repeat(73);

        assertThatThrownBy(() -> authService.changePassword(USER, "current-pass", tooLong))
            .isInstanceOf(PasswordChangeException.class)
            .hasMessageContaining("too long");

        verifyNoInteractions(userRepository);

    }


    @Test
    void changePassword_withMissingPasswords_isRejected(){

        assertThatThrownBy(() -> authService.changePassword(USER, null, "new-password-1"))
            .isInstanceOf(PasswordChangeException.class)
            .hasMessageContaining("required");
        
        assertThatThrownBy(() -> authService.changePassword(USER, "current-pass", " "))
            .isInstanceOf(PasswordChangeException.class)
            .hasMessageContaining("required");

        verifyNoInteractions(userRepository);

    }


    @Test
    void changePassword_toTheSamePassword_isRejected(){

        DashboardUser user = stubExistingUser();

        when(passwordEncoder.matches("same-password-1", OLD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(USER, "same-password-1", "same-password-1"))
            .isInstanceOf(PasswordChangeException.class)
            .hasMessageContaining("must be different");

        verify(userRepository, never()).save(user);

    }


    @Test
    void changePassword_forAnUnknownUser_isRejected(){

        when(userRepository.findById(USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(USER, "current-pass", "new-password-1"))
            .isInstanceOf(PasswordChangeException.class)
            .hasMessageContaining("user was not found");

    }

}

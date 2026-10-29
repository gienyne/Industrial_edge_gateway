package com.gienyne.industrialedgegateway.persistence.user;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gienyne.industrialedgegateway.persistence.shared.error.GlobalExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;



/**
 * Tests of the HTTP layer of /api/auth: URLs, JSON in and out, status codes.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest{

    private static final UsernamePasswordAuthenticationToken SIGNED_IN_USER = new UsernamePasswordAuthenticationToken("USR-DIMZZ237", null, List.of());

    @Mock
    private AuthService authService;


    private MockMvc mockMvc;

    @BeforeEach
    void setUp(){

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    }


    // --- login --------------------------------------------

    @Test
    void login_withValidCredentials_returnsTheUserId() throws Exception{

        when(authService.login(eq("USR-DIMZZ237"), eq("believeInyrslf"), any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(SIGNED_IN_USER);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"USR-DIMZZ237\",\"password\":\"believeInyrslf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("USR-DIMZZ237"));
    }


    @Test
    void login_withWrongCredentials_returns401(){

        when(authService.login(eq("USR-DIMZZ237"), eq("wrong"), any(HttpServletRequest.class), any(HttpServletResponse.class)))
        .thenThrow(new BadCredentialsException("bad credentials"));

        try{

            mockMvc.perform(post("/api/auth/login")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("{\"userId\":\"USR-DIMZZ237\",\"password\":\"wrong\"}"))
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$.message").value("Invalid user ID or password"));

        }
        catch(Exception exception){
            throw new AssertionError(exception);
        }
    }


    @Test
    void login_withMissingFields_returns401_withoutCallingTheService() throws Exception{

        mockMvc.perform(post("/api/auth/login")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }


    // --- current user --------------------------------------------

    @Test
    void me_returnsTheSignedInUser() throws Exception{

        mockMvc.perform(get("/api/auth/me").principal(SIGNED_IN_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("USR-DIMZZ237"));

    }


    // --- change password --------------------------------------------

    @Test
    void changePassword_withValidData_returns204() throws Exception{

        mockMvc.perform(post("/api/auth/password")
               .principal(SIGNED_IN_USER)
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"currentPassword\":\"current-pass\",\"newPassword\":\"new-password-1\"}"))
            .andExpect(status().isNoContent());

        verify(authService).changePassword("USR-DIMZZ237", "current-pass", "new-password-1");

    }


    @Test
    void changePassword_rejectedByTheService_returns400_withTheMessage() throws Exception{

        doThrow(new PasswordChangeException("The current password is incorrect"))
            .when(authService).changePassword(any(), any(), any());
        

        mockMvc.perform(post("/api/auth/password")
               .principal(SIGNED_IN_USER)
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-password-1\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("The current password is incorrect"));

    }


    @Test
    void changePassword_withoutBody_returns400() throws Exception{

        mockMvc.perform(post("/api/auth/password")
               .principal(SIGNED_IN_USER)
               .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

}
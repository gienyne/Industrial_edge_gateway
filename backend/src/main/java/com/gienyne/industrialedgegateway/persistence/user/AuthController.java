package com.gienyne.industrialedgegateway.persistence.user;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.gienyne.industrialedgegateway.persistence.user.models.api.ApiErrorResponse;
import com.gienyne.industrialedgegateway.persistence.user.models.api.ApiLoginRequest;
import com.gienyne.industrialedgegateway.persistence.user.models.api.ApiLoginResponse;


@RestController
@RequestMapping("/api/auth")
public class AuthController{

    private final AuthService authService;


    public AuthController(AuthService authService){
        this.authService = authService;
    }

    // Prepares the CSRF token before login.
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken csrfToken){
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }


    @Operation(parameters = { @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true, description = "CSRF token from the XSRF-TOKEN cookie")})
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @RequestBody(required = false) ApiLoginRequest body,
        HttpServletRequest request,
        HttpServletResponse response
        ){

            if(body == null || body.userId() == null || body.userId().isBlank() || body.password() == null || body.password().isBlank()){
                return invalidCredentials();
            }
            try{
                // The service checks the credentials and creates the session.
                Authentication authentication = authService.login(body.userId(), body.password(), request, response);

                // Returns the ID of the logged-in user.
                return ResponseEntity.ok(new ApiLoginResponse(authentication.getName()));
            }
            catch(AuthenticationException exception){
                return invalidCredentials();
            }
        }


    // Returns the ID of the current user.
    @GetMapping("/me")
    public ApiLoginResponse me(Authentication authentication){
        return new ApiLoginResponse(authentication.getName());
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ApiErrorResponse> invalidCredentials(){

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            new ApiErrorResponse(
                401,
                "Unauthorized",
                "Invalid user ID or password",
                Instant.now()
            )
        );
    }

}
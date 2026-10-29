package com.gienyne.industrialedgegateway.persistence.user;


import com.gienyne.industrialedgegateway.persistence.user.domain.DashboardUser;
import com.gienyne.industrialedgegateway.persistence.user.repository.DashboardUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Service;


@Service
public class AuthService{

    static final int MIN_PASSWORD_LENGTH = 8;

    // BCrypt only uses the first 72 bytes of a password.
    static final int MAX_PASSWORD_BYTES = 72;

    private final AuthenticationManager authenticationManager;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final DashboardUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Manages the security information of the current user.
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    // Saves the authenticated user in the HTTP session.
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    // Changes the session ID after a successful login.
    private final SessionAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();


    public AuthService(AuthenticationManager authenticationManager, CookieCsrfTokenRepository csrfTokenRepository, DashboardUserRepository userRepository, PasswordEncoder passwordEncoder){

        this.authenticationManager = authenticationManager;
        this.csrfTokenRepository = csrfTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

    }


    // Checks the credentials and creates the authenticated session.
    public Authentication login(String userId, String password, HttpServletRequest request, HttpServletResponse response){

        Authentication authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(userId, password));

        // Creates a session before changing its ID.
        request.getSession(true);

        sessionStrategy.onAuthentication(authentication, request, response);

        // Stores the authenticated user in the security context.
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);

        //Saves the security context so later requests keep the login.
        contextRepository.saveContext(context, request, response);

        // Clears the old CSRF token; the client must fetch a new one.
        csrfTokenRepository.saveToken(null, request, response);

        return authentication;
    }


    // Ends the session and clears the CSRF cookie.
    public void logout(HttpServletRequest request, HttpServletResponse response){

        new SecurityContextLogoutHandler().logout(request, response, contextHolder.getContext().getAuthentication());

        csrfTokenRepository.saveToken(null, request, response);

    }


    // Replaces the password of the signed-in user after checking the current one.
    public void changePassword(String userId, String currentPassword, String newPassword){

        if(currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()){
            throw new PasswordChangeException("The current and the new password are required");
        }

        if(newPassword.length() < MIN_PASSWORD_LENGTH){
            throw new PasswordChangeException("The new password must have at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        if(newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES){
            throw new PasswordChangeException("The new password is too long. Please choose a shorter password.");
        }

        DashboardUser user = userRepository.findById(userId).orElseThrow(() -> new PasswordChangeException("The user was not found"));

        if(!passwordEncoder.matches(currentPassword, user.getPasswordHash())){
            throw new PasswordChangeException("The current password is incorrect");
        }

        if(passwordEncoder.matches(newPassword, user.getPasswordHash())){
            throw new PasswordChangeException("The new password must be different from the current one");
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));

        userRepository.save(user);

    }
}
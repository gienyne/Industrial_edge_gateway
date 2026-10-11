package com.gienyne.industrialedgegateway.persistence.user;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Service;


@Service
public class AuthService{

    private final AuthenticationManager authenticationManager;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    // Manages the security information of the current user.
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    // Saves the authenticated user in the HTTP session.
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    // Changes the session ID after a successful login.
    private final SessionAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();


    public AuthService(AuthenticationManager authenticationManager, CookieCsrfTokenRepository csrfTokenRepository){

        this.authenticationManager = authenticationManager;
        this.csrfTokenRepository = csrfTokenRepository;

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
}
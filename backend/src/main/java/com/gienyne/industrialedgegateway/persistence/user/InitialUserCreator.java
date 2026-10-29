package com.gienyne.industrialedgegateway.persistence.user;


import com.gienyne.industrialedgegateway.persistence.user.domain.DashboardUser;
import com.gienyne.industrialedgegateway.persistence.user.repository.DashboardUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;



@Component
public class InitialUserCreator implements ApplicationRunner{

    private static final Logger log = LoggerFactory.getLogger(InitialUserCreator.class);

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final DashboardUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUser;
    private final String adminPassword;

    public InitialUserCreator(

        DashboardUserRepository userRepository,
        PasswordEncoder passwordEncoder,

        @Value("${gateway.dashboard.admin-user:}")
        String adminUser,

        @Value("${gateway.dashboard.admin-password:}")
        String adminPassword
    ){

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;

    }


    @Override
    public void run(ApplicationArguments arguments){

        log.info("Dashboard admin configured: '{}'", adminUser);
        log.info("Dashboard password configured: {}", !adminPassword.isBlank());
        log.info("Users count before initialization: {}", userRepository.count());

        if(userRepository.count() > 0){
            return;
        }

        if(adminUser.isBlank() || adminPassword.isBlank()){
            log.warn("No dashboard user exists. Set DASHBOARD_ADMIN_USER and DASHBOARD_ADMIN_PASSWORD to create the first one.");
            return;
        }

        if(adminPassword.length() < MIN_PASSWORD_LENGTH){
            log.error("DASHBOARD_ADMIN_PASSWORD needs at least {} characters; no user was created", MIN_PASSWORD_LENGTH);
            return;
        }

        String userId = adminUser.trim();

        userRepository.save(new DashboardUser(userId, passwordEncoder.encode(adminPassword)));

        log.info("Initial dashboard user '{}' created", userId);

    }
}
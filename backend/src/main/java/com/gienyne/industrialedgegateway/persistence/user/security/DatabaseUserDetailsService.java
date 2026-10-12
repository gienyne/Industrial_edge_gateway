package com.gienyne.industrialedgegateway.persistence.user.security;


import com.gienyne.industrialedgegateway.persistence.user.domain.DashboardUser;
import com.gienyne.industrialedgegateway.persistence.user.repository.DashboardUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class DatabaseUserDetailsService implements UserDetailsService{

    private final DashboardUserRepository users;

    public DatabaseUserDetailsService(DashboardUserRepository users){
         this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException{

        DashboardUser user = users.findById(userId).orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUserId())
            .password(user.getPasswordHash())
            .disabled(!user.isActive())
            .roles("USER").build();
    }
}
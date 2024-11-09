package com.ecom.config;

import com.ecom.jwt.JWTAuthenticationFilter;
import com.ecom.jwt.JwtAuthenticationEntryPoint;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@AllArgsConstructor
public class SecurityFilterConfig {

    @Autowired
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    @Lazy
    private AuthFailureHandlerImpl authenticationFailureHandler;

    @Autowired
    private JwtAuthenticationEntryPoint point;
    @Autowired
    private JWTAuthenticationFilter filter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/**").hasRole("USER")               // Restrict /user/** to USER role
                        .requestMatchers("/admin/**").hasRole("ADMIN")             // Restrict /admin/** to ADMIN role
                        .requestMatchers("/auth/login", "/auth/create-employee").permitAll()  // Allow public access
                        .anyRequest().authenticated()                              // All other requests require authentication
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(point)                           // Custom entry point
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)    // Use stateless session management
                )
                .formLogin(form -> form
                        .loginPage("/signin")                                      // Custom login page
                        .loginProcessingUrl("/login")                              // Login processing URL
                        .failureHandler(authenticationFailureHandler)              // Failure handler
                        .successHandler(authenticationSuccessHandler)              // Success handler
                )
                .logout(logout -> logout
                        .permitAll()                                               // Allow logout for all users
                )
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class); // Custom filter

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}

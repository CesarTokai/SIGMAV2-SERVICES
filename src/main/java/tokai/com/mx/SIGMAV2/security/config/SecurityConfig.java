package tokai.com.mx.SIGMAV2.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.security.infrastructure.filter.JwtAuthenticationFilter;
import tokai.com.mx.SIGMAV2.security.infrastructure.filter.JwtRevocationFilter;
import tokai.com.mx.SIGMAV2.security.infrastructure.jwt.JwtUtils;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.JwtBlacklistService;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.TokenRevocationService;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final JwtBlacklistService jwtBlacklistService;
    private final TokenRevocationService tokenRevocationService;
    private final JpaUserRepository jpaUserRepository;

    public SecurityConfig(JwtUtils jwtUtils, JwtBlacklistService jwtBlacklistService,
                         TokenRevocationService tokenRevocationService, JpaUserRepository jpaUserRepository) {
        this.jwtUtils = jwtUtils;
        this.jwtBlacklistService = jwtBlacklistService;
        this.tokenRevocationService = tokenRevocationService;
        this.jpaUserRepository = jpaUserRepository;
    }

    // configuration of security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/sigmav2/users/register",
                                "/api/sigmav2/users/verify",
                                "/api/sigmav2/users/exists",
                                // Permitir solo endpoints públicos concretos
                                "/api/sigmav2/auth/createRequest",
                                "/api/sigmav2/auth/verifyUser",
                                "/api/sigmav2/auth/login",
                                "/api/auth/logout")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtRevocationFilter(tokenRevocationService, jwtUtils), BasicAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtils, jwtBlacklistService, jpaUserRepository), BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // definition of authenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // configuration authentication provider
    @Bean
    @SuppressWarnings("deprecation")
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
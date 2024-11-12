package org.ftf.koifishveterinaryservicecenter.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class AppConfig {

    private final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/users/token", "/api/v1/users/introspect", "api/v1/users/customers", "api/v1/users/signup", "api/v1/users/**",
            "/api/v1/users/outbound/authentication"
    };

    @Value("${frontend.domain}")
    private String frontendDomain;

    private CustomJwtDecoder customJwtDecoder;

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        return modelMapper;
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean  // Remove authority prefix
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrfConfig -> csrfConfig
                        .ignoringRequestMatchers("/api/v1/users/token",
                                "/api/v1/users/introspect", "/api/v1/users/customers",
                                "/api/v1/users/signup", "/api/v1/fishes", "api/v1/users/logout",
                                "api/v1/users/refresh", "/api/v1/users/outbound/authentication"))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/fish/update/**").permitAll()
                        // Chỉ cho phép role CUS truy cập PUT /api/v1/fishes/deletefish

                        // Các yêu cầu còn lại phải được xác thực
                        .requestMatchers("/api/v1/users/signup").hasAnyAuthority("MAN")
                        .requestMatchers("/api/v1/users/staff").hasAnyAuthority("MAN")
                        .requestMatchers("/api/v1/users/staffs").hasAnyAuthority("MAN")
                        .requestMatchers("/api/v1/users/customers").hasAnyAuthority("MAN")
                        .requestMatchers("/api/v1/users/deleteuser").hasAnyAuthority("MAN")
                        .requestMatchers("/api/v1/email/sendNotification").hasAnyAuthority("STA")
                        .requestMatchers("api/v1/email/sendBill").hasAnyAuthority("CUS")  // for test
                        .requestMatchers("/api/v1/email/sendNotification").hasAnyAuthority("STA")
                        .requestMatchers("api/v1/email/sendBill").hasAnyAuthority("CUS")  // for test

                        .requestMatchers("/files/**", "/favicon.ico", "/api/v1/users/service/{serviceId}", "/api/v1/users/{veterinarianId}/average-rating", "/api/v1/users/average-rating/**", "/api/v1/users/veterinarians").permitAll()
                        .requestMatchers("/api/v1/email/sendNotification").hasAnyAuthority("STA")
                        .requestMatchers("api/v1/email/sendBill").hasAnyAuthority("CUS")  // for test

                        .requestMatchers("/files/images/**", "/files/certificates/**", "/api/v1/payments/vnpay-notify", "/error", "/api/v1/feedbacks/limited").permitAll()
                        .requestMatchers("/api/v1/certificates/**").hasAnyAuthority("MAN")
                        .requestMatchers("/api/v1/email/sendNotification").hasAnyAuthority("STA")
                        .requestMatchers("api/v1/email/sendBill").hasAnyAuthority("CUS")  // for test

                        .requestMatchers("api/v1/users/payment-statistics").hasAnyAuthority("MAN")
                        .requestMatchers("api/v1/users/appointment-statistics").hasAnyAuthority("MAN")
                        .requestMatchers("api/v1/users/user-fish-statistics").hasAnyAuthority("MAN")
                        .requestMatchers("api/v1/users/{vetId}/slots-this-week").hasAnyAuthority("MAN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer
                                .decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        ))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS) // or SessionCreationPolicy.ALWAYS
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource())); // Enable CORS

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin(frontendDomain); // Allow requests from React app
        configuration.addAllowedMethod("*"); // Allow all HTTP methods (GET, POST, PUT, DELETE, etc.)
        configuration.addAllowedHeader("*"); // Allow all headers
        configuration.setAllowCredentials(true); // Allow credentials (e.g., cookies, authorization headers)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply this CORS configuration to all endpoints
        return source;
    }
}



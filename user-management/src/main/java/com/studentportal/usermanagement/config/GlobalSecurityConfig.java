package com.studentportal.usermanagement.config;

import com.studentportal.usermanagement.security.JwtAuthenticationFilter;
import com.studentportal.usermanagement.security.RestAuthenticationEntryPoint;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless JWT security: public paths from config, everything else needs a valid access token. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
@EnableConfigurationProperties({
  SecurityProperties.class,
  CorsProperties.class,
  JwtProperties.class,
  OtpProperties.class,
  BootstrapAdminProperties.class
})
public class GlobalSecurityConfig {

  private final SecurityProperties securityProperties;
  private final CorsProperties corsProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(securityProperties.getPublicAccess().toArray(String[]::new))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAuthenticationEntryPoint))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    for (CorsProperties.CorsMapping mapping : corsProperties.getMappings()) {
      CorsConfiguration config = new CorsConfiguration();
      if (Objects.nonNull(mapping.getAllowedOrigins())) {
        // Origins may arrive as one comma-separated env var; patterns allow https://*.vercel.app
        config.setAllowedOriginPatterns(
            mapping.getAllowedOrigins().stream()
                .flatMap(origin -> Arrays.stream(origin.split(",")))
                .map(String::trim)
                .toList());
      }
      config.setAllowedMethods(mapping.getAllowedMethods());
      config.setAllowCredentials(mapping.getAllowCredentials());
      config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
      config.setExposedHeaders(List.of("Authorization"));
      source.registerCorsConfiguration(mapping.getPath(), config);
    }
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Auth is done by our own services, not Spring's login flow. This bean only stops Boot from
   * creating its default in-memory user and printing a generated password at startup.
   */
  @Bean
  public UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException(username);
    };
  }
}

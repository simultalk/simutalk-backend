package pe.upc.simutalk.securities;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private static final String[] PUBLIC_ROUTES = {
            "/api/v1/authentication/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/error"
    };

    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final UnauthorizedRequestHandlerEntryPoint unauthorizedRequestHandler;
    private final ForbiddenRequestHandler forbiddenRequestHandler;

    public SecurityConfiguration(UserDetailsService userDetailsService, TokenService tokenService,
                                    UnauthorizedRequestHandlerEntryPoint unauthorizedRequestHandler,
                                    ForbiddenRequestHandler forbiddenRequestHandler) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.unauthorizedRequestHandler = unauthorizedRequestHandler;
        this.forbiddenRequestHandler = forbiddenRequestHandler;
    }

    @Bean
    public BearerAuthorizationRequestFilter authorizationRequestFilter() {
        return new BearerAuthorizationRequestFilter(tokenService, userDetailsService);
    }

    /**
     * Keeps the servlet container from registering the filter a second time outside the
     * security chain; there it would run before the SecurityContext is set up and the
     * chain's instance would then be skipped as "already filtered".
     */
    @Bean
    public FilterRegistrationBean<BearerAuthorizationRequestFilter> authorizationRequestFilterRegistration(
            BearerAuthorizationRequestFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** Single bean serving as Spring's PasswordEncoder and as the application HashingService. */
    @Bean
    public HashingService passwordEncoder() {
        return new BCryptHashingServiceImpl();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // TODO: in production restrict allowed origins to the Angular frontend domain.
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedRequestHandler)
                        .accessDeniedHandler(forbiddenRequestHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

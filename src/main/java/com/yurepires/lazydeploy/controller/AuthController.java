package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.dto.request.LoginRequest;
import com.yurepires.lazydeploy.dto.request.RegisterUserRequest;
import com.yurepires.lazydeploy.dto.response.AuthenticatedUserResponse;
import com.yurepires.lazydeploy.exception.InvalidCredentialsException;
import com.yurepires.lazydeploy.security.AuthenticatedUser;
import com.yurepires.lazydeploy.security.CurrentUserProvider;
import com.yurepires.lazydeploy.security.EmailNormalizer;
import com.yurepires.lazydeploy.service.auth.CurrentUserProfileService;
import com.yurepires.lazydeploy.service.auth.RegisterUserService;
import com.yurepires.lazydeploy.service.observability.SecurityMetrics;
import com.yurepires.lazydeploy.service.observability.SecurityEventLogger;
import com.yurepires.lazydeploy.service.observability.SecurityEventOutcome;
import com.yurepires.lazydeploy.service.observability.SecurityEventType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserService registerUserService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final EmailNormalizer emailNormalizer;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentUserProfileService currentUserProfileService;
    private final SecurityMetrics securityMetrics;
    private final SecurityEventLogger securityEventLogger;

    public AuthController(
            RegisterUserService registerUserService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            EmailNormalizer emailNormalizer,
            CurrentUserProvider currentUserProvider,
            CurrentUserProfileService currentUserProfileService,
            SecurityMetrics securityMetrics,
            SecurityEventLogger securityEventLogger
    ) {
        this.registerUserService = registerUserService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.emailNormalizer = emailNormalizer;
        this.currentUserProvider = currentUserProvider;
        this.currentUserProfileService = currentUserProfileService;
        this.securityMetrics = securityMetrics;
        this.securityEventLogger = securityEventLogger;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthenticatedUserResponse register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        return registerUserService.register(request);
    }

    @PostMapping("/login")
    public AuthenticatedUserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            securityMetrics.recordLoginAttempt("invalid_credentials");
            securityEventLogger.log(
                    SecurityEventType.AUTH_LOGIN_FAILURE,
                    SecurityEventOutcome.REJECTED,
                    "INVALID_CREDENTIALS",
                    "/api/auth/login"
            );
            throw new InvalidCredentialsException();
        }

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        securityMetrics.recordLoginAttempt("success");
        securityEventLogger.log(
                SecurityEventType.AUTH_LOGIN_SUCCESS,
                SecurityEventOutcome.SUCCESS,
                "AUTHENTICATED",
                "/api/auth/login"
        );
        return AuthenticatedUserResponse.from(authenticatedUser);
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me() {
        return currentUserProfileService.findById(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/csrf")
    public String csrfToken(CsrfToken csrfToken) {
        return csrfToken.getToken();
    }
}

package com.nova.bank.auth.controllers;
import com.nova.bank.auth.dto.*;
import com.nova.bank.auth.dto.request.ChangePasswordRequest;
import com.nova.bank.auth.entites.Role;
import com.nova.bank.auth.entites.User;
import com.nova.bank.auth.exception.UserNotFound;
import com.nova.bank.auth.repositories.RoleRepository;
import com.nova.bank.auth.repositories.UserRepository;
import com.nova.bank.auth.security.JwtHelper;
import com.nova.bank.auth.services.UserService;
import io.jsonwebtoken.JwtException;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthenticationController {
    private static final Pattern ROLE_NAME_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{1,29}");

    private final ModelMapper modelMapper;

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final JwtHelper jwtHelper;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    public AdminAuthenticationController(ModelMapper modelMapper, UserService userService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtHelper jwtHelper, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.modelMapper = modelMapper;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtHelper = jwtHelper;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/role/add")
    public ResponseEntity<String> roleAdd(@RequestBody Role role){
        String roleName = role.getName() == null ? "" : role.getName().trim().toUpperCase(Locale.ROOT);
        if (!ROLE_NAME_PATTERN.matcher(roleName).matches()) {
            return ResponseEntity.badRequest().body("Role name must be 2-30 characters and use uppercase letters, digits, or underscores.");
        }
        if (!roleRepository.existsByName(roleName)){
            Role role1= new Role();
            role1.setId(UUID.randomUUID().toString());
            role1.setName(roleName);
            roleRepository.save(role1);
            return ResponseEntity.status(HttpStatus.CREATED).body("Role registered successfully: " + roleName);
        }
        return ResponseEntity.ok("Role already exists: " + roleName);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employees")
    public List<AdminEmployeeResponse> getEmployees() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> !"CUSTOMER".equalsIgnoreCase(role.getName())))
                .sorted(Comparator.comparing(User::getId))
                .map(user -> new AdminEmployeeResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getCreateAt(),
                        user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList())))
                .collect(Collectors.toList());
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'HR')")
    @GetMapping("/roles")
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }
    // assign the role to the employee

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER','HR')")
    @PatchMapping("/assign/role/{email}")
    public ResponseEntity<String> assignRole(@PathVariable String email, @RequestBody Role role){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFound("user not found :" + email));
        String roleName = role.getName().trim().toUpperCase(Locale.ROOT);
        Role role1 = roleRepository.findByNameContainingIgnoreCase(roleName).orElseThrow();
        user.setRoles(List.of(role1));
        userRepository.save(user);
        return ResponseEntity.ok("Role assigned successfully: " + roleName);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/employee/register")
    public ResponseEntity<Long> register(@RequestBody AdminRegisterUser adminRegisterUser) {
        Long l = userService.adminRegisterEmployee(adminRegisterUser);
        return new ResponseEntity<>(l,HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        System.out.println(loginRequest.username());
        System.out.println(loginRequest.password());

        try {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());
            this.authenticationManager.authenticate(authentication);

            // generate the token
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.username());
            String token = jwtHelper.generateAccessToken(userDetails);
            String refreshToken = jwtHelper.generateRefreshToken(userDetails);
            User user = userRepository.findByEmail(loginRequest.username()).orElseThrow(() -> new UsernameNotFoundException("User not exist in database"));

            JwtResponse jwtResponse = new JwtResponse(
                    token,
                    refreshToken,
                    modelMapper.map(user, UserDto.class)

            );

            return new ResponseEntity<>(jwtResponse, HttpStatus.CREATED);


        } catch (BadCredentialsException e) {
            System.out.println("Invalid Credential ");
            ErrorResponse errorResponse = new ErrorResponse("username and password is invalid ", 401, false);
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody(required = false) String refreshToken) {

        if (refreshToken == null) {
            return new ResponseEntity<>(new ErrorResponse("Refresh token is required.", 400, false), HttpStatus.BAD_REQUEST);
        }

        try {
            if (!jwtHelper.isRefreshToken(refreshToken)) {
                return new ResponseEntity<>(new ErrorResponse("Refresh token is invalid or expired.", 401, false), HttpStatus.UNAUTHORIZED);
            }

            String usernameFromToken = jwtHelper.getUsernameFromToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(usernameFromToken);

            if (jwtHelper.isTokenValid(refreshToken, userDetails)) {

                String accessToken = jwtHelper.generateAccessToken(userDetails);
                String newRefreshToken1 = jwtHelper.generateRefreshToken(userDetails);
                User user = userRepository.findByEmail(usernameFromToken).orElseThrow(() -> new UsernameNotFoundException("username not found "));

                UserDto userDto = modelMapper.map(user, UserDto.class);

                return new ResponseEntity<>(new JwtResponse(accessToken, newRefreshToken1, userDto), HttpStatus.OK);


            } else {
                return new ResponseEntity<>(new ErrorResponse("Refresh token is invalid or expired.", 401, false), HttpStatus.UNAUTHORIZED);
            }
        } catch (JwtException | IllegalArgumentException exception) {
            return new ResponseEntity<>(new ErrorResponse("Refresh token is invalid or expired.", 401, false), HttpStatus.UNAUTHORIZED);
        }

    }

    @PostMapping("/employee/register-service")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'BRANCH_MANAGER')")
    public ResponseEntity<String> registerEmployeeFromService(@RequestBody AuthRegistrationRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreateAt(LocalDateTime.now());
        user.setBranchName(request.getBranchName());
        user.setBranchCode(request.getBranchCode());
        user.setIfsc(request.getIfsc());

        String roleName = request.getRoleName().trim().toUpperCase();
        Role role = roleRepository.findByNameContainingIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        user.setRoles(List.of(role));

        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser.getId().toString());
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            java.security.Principal principal) { // Gets the logged-in user's email

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 1. Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return new ResponseEntity<>(
                    new ErrorResponse("Incorrect current password", 400, false),
                    HttpStatus.BAD_REQUEST
            );
        }

        // 2. Encode and save new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new ResponseEntity<>("Password changed successfully", HttpStatus.OK);
    }
    // admin delete the employee

}

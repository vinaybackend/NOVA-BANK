package com.nova.bank.auth.controllers;
import com.nova.bank.auth.dto.*;
import com.nova.bank.auth.entites.Role;
import com.nova.bank.auth.entites.User;
import com.nova.bank.auth.exception.UserNotFound;
import com.nova.bank.auth.repositories.RoleRepository;
import com.nova.bank.auth.repositories.UserRepository;
import com.nova.bank.auth.security.JwtHelper;
import com.nova.bank.auth.services.UserService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthenticationController {

    private final ModelMapper modelMapper;

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final JwtHelper jwtHelper;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminAuthenticationController(ModelMapper modelMapper, UserService userService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtHelper jwtHelper, UserRepository userRepository, RoleRepository roleRepository) {
        this.modelMapper = modelMapper;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtHelper = jwtHelper;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/role/add")
    public String roleAdd(@RequestBody Role role){

        if (!roleRepository.existsByName(role.getName())){
            Role role1= new Role();
            role1.setId(UUID.randomUUID().toString());
            role1.setName(role.getName());
            roleRepository.save(role1);
            return "role registered successfully ";
        }
        return "role exits : " +role.getName();

    }

    // assign the role to the employee

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/assign/role/{id}")
    public String assignRole(@PathVariable Long id,@RequestBody Role role){
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFound("user not found :" + id));
        Role role1 = roleRepository.findByName(role.getName()).orElseThrow();
        user.setRoles(List.of(role1));
        userRepository.save(user);
        return "Role assign successfully " + role.getName();
    }

    @PostMapping("/employee/register")
    public ResponseEntity<String> register(@RequestBody AdminRegisterUser adminRegisterUser) {
            userService.adminRegisterEmployee(adminRegisterUser);
        return new ResponseEntity<>("Register successfully", HttpStatus.CREATED);

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
            return new ResponseEntity<>(new ErrorResponse("refresh token is null", 400, false), HttpStatus.BAD_REQUEST);
        }

        if (!jwtHelper.isRefreshToken(refreshToken)) {
            return new ResponseEntity<>(new ErrorResponse("Refresh token is invalid ", 400, false), HttpStatus.BAD_REQUEST);
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
            return new ResponseEntity<>(new ErrorResponse("token is invalid ", 400, false), HttpStatus.BAD_REQUEST);
        }

    }
}

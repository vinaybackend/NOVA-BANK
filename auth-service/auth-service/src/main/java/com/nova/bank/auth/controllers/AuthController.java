package com.nova.bank.auth.controllers;
import com.nova.bank.auth.dto.ErrorResponse;
import com.nova.bank.auth.dto.JwtResponse;
import com.nova.bank.auth.dto.LoginRequest;
import com.nova.bank.auth.dto.UserDto;
import com.nova.bank.auth.entites.User;
import com.nova.bank.auth.exception.ResourceNotFound;
import com.nova.bank.auth.repositories.UserRepository;
import com.nova.bank.auth.security.JwtHelper;
import com.nova.bank.auth.services.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;
    private UserDetailsService userDetailsService;
    private JwtHelper jwtHelper;
    private UserService userService;
    private UserRepository userRepository;
    private ModelMapper modelMapper;

    public AuthController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtHelper jwtHelper, UserService userService, UserRepository userRepository, ModelMapper modelMapper) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtHelper = jwtHelper;
        this.userService = userService;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest ){

        // generate the token

        try{
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());
            this.authenticationManager.authenticate(authentication);

            //generate the token

            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.username());

            String token = jwtHelper.generateAccessToken(userDetails);
            String refreshToken=jwtHelper.generateRefreshToken(userDetails);

            User user = userRepository.findByEmail(loginRequest.username()).orElseThrow(() -> new UsernameNotFoundException("User not exists in database"));
            JwtResponse jwtResponse= new JwtResponse(
                    token,
                     refreshToken,
                     modelMapper.map(user, UserDto.class)

            );

            return new ResponseEntity<>(jwtResponse, HttpStatus.OK);


        }catch (BadCredentialsException e){
            System.out.println("Invalid Credential ");
            ErrorResponse errorResponse=new ErrorResponse("Username and Password entered is incorrect : ",401,false);

           return new ResponseEntity<>(errorResponse,HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserDto userDto){

        userService.registerUser(userDto);

        return new ResponseEntity<>("Register Successfully",HttpStatus.CREATED);

    }


    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody(required = false) String refreshToken ){

        if (refreshToken==null){
            return new ResponseEntity<>(new ErrorResponse("refresh token is null",400,false),HttpStatus.BAD_REQUEST);
        }

        if (!jwtHelper.isRefreshToken(refreshToken)){
            return new ResponseEntity<>(new ErrorResponse("Refresh token is invalid ",400,false),HttpStatus.BAD_REQUEST);
        }

        String usernameFromToken = jwtHelper.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(usernameFromToken);

        if (jwtHelper.isTokenValid(refreshToken,userDetails)){

            String accessToken = jwtHelper.generateAccessToken(userDetails);
            String newRefreshToken1 = jwtHelper.generateRefreshToken(userDetails);
            User user = userRepository.findByEmail(usernameFromToken).orElseThrow(() -> new ResourceNotFound("username not found "));

            UserDto userDto = modelMapper.map(user, UserDto.class);

            return new ResponseEntity<>(new JwtResponse(accessToken,newRefreshToken1,userDto),HttpStatus.OK);


        }else {
            return new ResponseEntity<>(new ErrorResponse("token is invalid ",400,false),HttpStatus.BAD_REQUEST);
        }
    }

}

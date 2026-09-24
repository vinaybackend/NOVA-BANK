package com.nova.bank.auth.services;
import com.nova.bank.auth.dto.AdminRegisterUser;
import com.nova.bank.auth.dto.UserDto;
import com.nova.bank.auth.entites.Role;
import com.nova.bank.auth.entites.RoleConstants;
import com.nova.bank.auth.entites.User;
import com.nova.bank.auth.exception.ResourceNotFound;
import com.nova.bank.auth.repositories.RoleRepository;
import com.nova.bank.auth.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private ModelMapper modelMapper;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registerUser(UserDto userDto) {
        User user = modelMapper.map(userDto, User.class);
        Role roleCustomer = roleRepository.findByName(RoleConstants.CUSTOMER).orElseThrow(() -> new ResourceNotFound("Server not configure properly, please contact support"));
        user.setRoles(List.of(roleCustomer));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateAt(LocalDateTime.now());
        User saveUser = userRepository.save(user);

    }

    public void adminRegisterEmployee(AdminRegisterUser adminRegisterUser) {
        User user = modelMapper.map(adminRegisterUser, User.class);
        Role roleAdmin = roleRepository.findByName(adminRegisterUser.getRole().getName()).orElseThrow(() -> new ResourceNotFound("first create role then assign "));
        user.setRoles(List.of(roleAdmin));
        user.setCreateAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saveAdmin = userRepository.save(user);
    }
}

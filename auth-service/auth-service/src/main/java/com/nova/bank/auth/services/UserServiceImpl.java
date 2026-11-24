package com.nova.bank.auth.services;
import com.nova.bank.auth.dto.AdminRegisterUser;
import com.nova.bank.auth.dto.RegisterRequest;
import com.nova.bank.auth.entites.Role;
import com.nova.bank.auth.entites.RoleConstants;
import com.nova.bank.auth.entites.User;
import com.nova.bank.auth.exception.ResourceNotFound;
import com.nova.bank.auth.repositories.RoleRepository;
import com.nova.bank.auth.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    @Transactional
    public void registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        User user = modelMapper.map(request, User.class);

        Role roleCustomer = roleRepository
                .findByNameContainingIgnoreCase(RoleConstants.CUSTOMER)
                .orElseThrow(() ->
                        new ResourceNotFound(
                                "Server not configured properly, please contact support"
                        )
                );

        user.setRoles(List.of(roleCustomer));

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        user.setCreateAt(LocalDateTime.now());

        User save = userRepository.save(user);
        System.out.println(save);
    }

    public Long adminRegisterEmployee(AdminRegisterUser adminRegisterUser) {
        if (userRepository.existsByEmail(adminRegisterUser.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByPhone(adminRegisterUser.getPhone())) {
            throw new IllegalArgumentException("Phone number already registered");
        }
        String roleName = adminRegisterUser.getRoleName().trim().toUpperCase();


        User user = new User();
        user.setName(adminRegisterUser.getName());
        user.setPassword(passwordEncoder.encode(adminRegisterUser.getPassword()));
        user.setCreateAt(LocalDateTime.now());
        user.setPhone(adminRegisterUser.getPhone());
        user.setEmail(adminRegisterUser.getEmail());

        Role employeeRole = roleRepository.findByNameContainingIgnoreCase(roleName).orElseThrow(() -> new ResourceNotFound("Create the role before registering an employee."));
        user.setRoles(List.of(employeeRole));
        User saveAdmin = userRepository.save(user);
        Long id = saveAdmin.getId();
        return id;
    }
}

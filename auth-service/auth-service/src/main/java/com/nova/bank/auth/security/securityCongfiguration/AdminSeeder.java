package com.nova.bank.auth.security.securityCongfiguration;
import com.nova.bank.auth.entites.Role;
import com.nova.bank.auth.entites.RoleConstants;
import com.nova.bank.auth.entites.User;
import com.nova.bank.auth.repositories.RoleRepository;
import com.nova.bank.auth.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }


    @Override
    public void run(String... args) {

        String adminEmail = AdminSeederConfig.ADMIN_EMAIL;
        String adminPassword = AdminSeederConfig.ADMIN_PASSWORD;

        if (!roleRepository.existsByName("ADMIN")){
            Role adminRole= new Role();
            adminRole.setId(UUID.randomUUID().toString());
            adminRole.setName("ADMIN");
            this.roleRepository.save(adminRole);
        }
        if (!roleRepository.existsByName("CUSTOMER")){
            Role CustomerRole = new Role();
            CustomerRole.setId(UUID.randomUUID().toString());
            CustomerRole.setName("CUSTOMER");
            this.roleRepository.save(CustomerRole);
        }

        // Check if admin already exists
        if (userRepository.existsByEmail(adminEmail)) {
            System.out.println("Admin already exists: " + adminEmail);
            return;
        }

        // Create admin
        User admin = new User();

        admin.setEmail(adminEmail);
        Role role = roleRepository.findByName(RoleConstants.ADMIN).orElseThrow();

        // Hash password before saving
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRoles(List.of(role));
        userRepository.save(admin);

        System.out.println("=================================");
        System.out.println("Admin created successfully!");
        System.out.println("Email: " + adminEmail);
        System.out.println("=================================");
    }
}

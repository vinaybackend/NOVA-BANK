package com.nova.bank.auth;

import com.nova.bank.auth.entites.Role;

import com.nova.bank.auth.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class AuthServiceApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {


    }
}

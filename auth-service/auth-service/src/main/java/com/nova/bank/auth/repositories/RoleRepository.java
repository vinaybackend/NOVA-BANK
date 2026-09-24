package com.nova.bank.auth.repositories;

import com.nova.bank.auth.entites.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,String> {

   Optional<Role>findByName(String name);
    boolean existsByName(String name);
}

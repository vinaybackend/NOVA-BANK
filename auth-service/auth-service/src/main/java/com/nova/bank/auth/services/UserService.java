package com.nova.bank.auth.services;

import com.nova.bank.auth.dto.AdminRegisterUser;
import com.nova.bank.auth.dto.UserDto;

public interface UserService {

    void registerUser(UserDto userDto);

    void adminRegisterEmployee(AdminRegisterUser adminRegisterUser);
}

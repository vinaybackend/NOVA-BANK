package com.nova.bank.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CustomerCreateWrapper {
    private String customer; // This will hold the JSON string
    private MultipartFile profileImage; // The file
}
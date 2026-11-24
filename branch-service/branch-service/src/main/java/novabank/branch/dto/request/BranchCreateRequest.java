package novabank.branch.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import novabank.branch.entities.BranchType;

import java.time.LocalDate;
import java.time.LocalTime;

public record BranchCreateRequest(

        @NotBlank(message = "Branch name is required")
        @Size(max = 120, message = "Branch name cannot exceed 120 characters")
        String branchName,

        @Pattern(
                regexp = "^[A-Z]{4}00[A-Z0-9]$",
                message = "IFSC code must contain 4 uppercase letters, 00, and 1 uppercase letter or digit"
        )
        String ifscCode,

        @NotNull(message = "Branch type is required")
        BranchType branchType,

        @NotBlank(message = "Address is required")
        @Size(max = 150)
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 80)
        String city,

        @NotBlank(message = "District is required")
        @Size(max = 80)
        String district,

        @NotBlank(message = "State is required")
        @Size(max = 80)
        String state,

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must contain 6 digits")
        String pinCode,

        @NotBlank(message = "Country is required")
        @Size(max = 60)
        String country,

        @Pattern(regexp = "^[0-9+() -]{10}$", message = "Invalid phone number format")
        String phone,

        @Email(message = "Invalid email address")
        @Size(max = 120)
        String email,

        LocalDate openingDate,

        LocalTime openingTime,

        LocalTime closingTime,

        @Size(max = 50)
        String timeZone
) {
}


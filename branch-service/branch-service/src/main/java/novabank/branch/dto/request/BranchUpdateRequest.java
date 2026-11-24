package novabank.branch.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record BranchUpdateRequest(

        @NotBlank
        @Size(max = 120)
        String branchName,

        @NotBlank
        @Size(max = 300)
        String address,

        @NotBlank
        @Size(max = 80)
        String city,

        @NotBlank
        @Size(max = 80)
        String district,

        @NotBlank
        @Size(max = 80)
        String state,

        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$")
        String pinCode,

        @NotBlank
        @Size(max = 60)
        String country,

        @Pattern(regexp = "^[0-9+() -]{7,20}$")
        String phone,

        @Email
        @Size(max = 120)
        String email,

        LocalDate openingDate,

        LocalTime openingTime,

        LocalTime closingTime,

        @Size(max = 50)
        String timeZone
) {
}
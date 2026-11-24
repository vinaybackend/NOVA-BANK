package novabank.employee.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeUpdateRequest(

        @NotBlank
        @Size(max = 80)
        String name,

        @NotBlank
        @Email
        @Size(max = 120)
        String email,

        @Pattern(regexp = "^[0-9+() -]{7,20}$")
        String mobileNumber,

        @NotBlank
        @Size(max = 100)
        String jobTitle,

        @NotBlank
        @Size(max = 80)
        String department

) {}
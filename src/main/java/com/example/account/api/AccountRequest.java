package com.example.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
public final class AccountRequest {

    @NotBlank(message = "Account name is required")
    @Length(min = 1, max = 15, message = "Account name must be at most 15 characters long")
    @Pattern(regexp = "[A-Za-z]*$", message = "Account name must contain letters only")
    private String name;
}

package com.smartwatch.leaderboard.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    private String fullName;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull
    private Long deviceId;
}
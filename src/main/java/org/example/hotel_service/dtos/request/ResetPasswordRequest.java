package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {

    @NotBlank(message = "Token khong duoc de trong")
    String token;

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Size(min = 6, max = 100, message = "Mat khau moi phai tu 6-100 ky tu")
    String newPassword;
}


package org.example.hotel_service.entities;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRoleId implements Serializable {
    Long userId;
    Integer roleId;
}

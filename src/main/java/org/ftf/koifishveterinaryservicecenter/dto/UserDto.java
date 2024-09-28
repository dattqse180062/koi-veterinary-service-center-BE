package org.ftf.koifishveterinaryservicecenter.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Integer userId;

    private String username;

    private String email;

    private String password;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String avatar;
}

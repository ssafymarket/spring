package org.ssafy.ssafymarket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "class", length = 50)
    private String className;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    private UserRole role;

    public enum UserRole {
        ROLE_USER,
        ROLE_ADMIN
    }
}

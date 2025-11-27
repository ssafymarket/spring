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

    @Enumerated(EnumType.STRING)
    @Column(name = "campus", nullable = false, length = 20)
    private Campus campus;

    public enum UserRole {
        ROLE_USER,
        ROLE_ADMIN
    }

    public enum Campus {
        SEOUL,      // 서울
        BUULGYEONG, // 부울경
        DAEJEON,    // 대전
        GUMI,       // 구미
        GWANGJU     // 광주
    }
}

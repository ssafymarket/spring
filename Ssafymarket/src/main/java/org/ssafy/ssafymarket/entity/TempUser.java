package org.ssafy.ssafymarket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "temp_user")
public class TempUser {


    @Id
    @Column(name = "student_id", nullable = false, length = 50)
    private String studentId;  // 학번

    @Column(name = "name", nullable = false, length = 50)
    private String name;       // 이름

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;  // 반

    @Column(name = "password", nullable = false, length = 255)
    private String password;   // 비밀번호 (임시)

    @Column(name="approve",nullable = false)
    private int approve=0;
}
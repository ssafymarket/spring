package org.ssafy.ssafymarket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ssafy.ssafymarket.entity.TempUser;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.service.AdminService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "관리자 회원 관리", description = "관리자 회원 관리 API")
public class AdminController {
    private final AdminService adminService;


    //승인 목록
    @Operation(
            summary = "승인 목록",
            description = "학번 ,이름,반,비밀 번호를 나타난다."
    )
    @GetMapping("/list")
    public ResponseEntity<Map<String,Object>> findAll(){
        List<TempUser> list=adminService.findAll();

        return ResponseEntity.ok(Map.of("success",true,"list",list));
    }

    //승인
    @Operation(
            summary = "승인",
            description = "학번를 가지고 승인을 진행한다."
    )
    @PutMapping("/users/{id}/status")
    public ResponseEntity<Map<String,Object>> updateStatus(@PathVariable String id) {
       User user= adminService.updateStatus(id);
       return ResponseEntity.ok(Map.of("success",true,"user",user));

    }
}

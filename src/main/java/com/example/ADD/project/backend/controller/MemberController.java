package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.ApiResponse;
import com.example.ADD.project.backend.dto.MemberDetailResponse;
import com.example.ADD.project.backend.dto.MemberSearchResponse;
import com.example.ADD.project.backend.dto.UserDto;
import com.example.ADD.project.backend.dto.UserLoginRequest;
import com.example.ADD.project.backend.entity.User;
import com.example.ADD.project.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 관련 API 엔드포인트를 제공하는 컨트롤러
 * 기본 경로: /api/member
 */
@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final UserService userService;

    public MemberController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 새로운 회원을 생성합니다.
     * @param userDto 생성할 회원의 정보가 담긴 DTO
     * @return 생성된 회원 정보
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        
        User savedUser = userService.saveUser(user);
        
        return ResponseEntity.ok(new UserDto(savedUser.getId(), savedUser.getName(), savedUser.getEmail()));
    }

    /**
     * 모든 회원의 목록을 조회합니다.
     * @return 전체 회원 DTO 리스트
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(user -> new UserDto(user.getId(), user.getName(), user.getEmail()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    /**
     * 특정 ID(PK)를 가진 회원의 정보를 조회합니다.
     * @param id 조회할 회원의 ID
     * @return 해당 회원 정보 DTO (존재하지 않으면 404 반환)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UserDto(user.getId(), user.getName(), user.getEmail()));
    }

    /**
     * 회원 코드를 사용하여 로그인을 진행합니다.
     * @param request 회원 코드(userCode)가 포함된 로그인 요청 객체
     * @return 성공 여부 및 메시지 (성공 시 200 OK, 실패 시 401 Unauthorized)
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginRequest request) {
        User user = userService.login(request.getUserCode());
        
        if (user != null) {
             // TODO: JWT 토큰 생성 또는 세션 처리 등 실제 인증 로직 추가
            return ResponseEntity.ok("회원 로그인 성공 (ID: " + user.getId() + ", Name: " + user.getName() + ")");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 회원 코드입니다.");
        }
    }

    /**
     * 이름으로 회원을 검색하고 이름과 프로필 사진 정보를 반환합니다.
     * (부분 일치 검색을 지원합니다.)
     * @param name 검색할 이름 
     * @return 검색된 회원들의 이름 및 프로필 사진 URL 목록
     */
    @GetMapping("/search")
    public ResponseEntity<List<MemberSearchResponse>> searchMembersByName(@RequestParam("name") String name) {
        List<User> users = userService.searchByName(name);
        
        // Entity 객체를 Response DTO 객체로 변환
        List<MemberSearchResponse> response = users.stream()
                .map(user -> new MemberSearchResponse(user.getName(), user.getProfileImageUrl()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 회원의 상세 정보를 조회합니다.
     * (입소 일자, 부서 정보, 입소 당시의 부서원 리스트 포함)
     * @param id 조회할 회원 ID
     * @return 공통 응답 포맷으로 감싸진 회원 상세 정보 DTO
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<MemberDetailResponse>> getMemberDetail(@PathVariable("id") Long id) {
        MemberDetailResponse detailResponse = userService.getMemberDetail(id);

        if (detailResponse == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MEMBER_NOT_FOUND", "해당 회원을 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(
                ApiResponse.success("MEMBER_DETAIL_SUCCESS", "회원 상세 정보를 조회했습니다.", detailResponse)
        );
    }
}
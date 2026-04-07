package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.member.*;
import com.example.ADD.project.backend.dto.ApiResponse;
import com.example.ADD.project.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/member/{memberId}")
    public ApiResponse<MemberDetailResponseDto> getMemberDetail(@PathVariable Long memberId) {
        return ApiResponse.success("200", "조회 성공", memberService.getMemberDetail(memberId));
    }

    @PostMapping("/admin/import/files")
    public ApiResponse<String> importMembersByFile(@RequestParam("file") MultipartFile file) {
        // Excel 파싱 및 벌크 인서트 로직 추가 위치
        return ApiResponse.success("200", "엑셀 업로드 성공", null);
    }

    @PostMapping("/admin/import/single")
    public ApiResponse<String> registerSingleMember(@RequestBody SingleMemberRegisterRequestDto request) {
        memberService.registerSingleMember(request);
        return ApiResponse.success("200", "회원 단일 등록 성공", null);
    }

    @PutMapping("/members/{memberId}")
    public ApiResponse<String> updateMember(@PathVariable Long memberId, @RequestBody MemberUpdateRequestDto request) {
        memberService.updateMember(memberId, request);
        return ApiResponse.success("200", "회원 정보 수정 성공", null);
    }

    @GetMapping("/members/search")
    public ApiResponse<List<MemberSearchResponseDto>> searchMembers(@RequestParam String name) {
        return ApiResponse.success("200", "검색 성공", memberService.searchMembers(name));
    }

    @GetMapping("/members/admission-years/{year}")
    public ApiResponse<List<MemberSearchResponseDto>> getMembersByYear(@PathVariable int year) {
        return ApiResponse.success("200", "연도별 조회 성공", memberService.getMembersByAdmissionYear(year));
    }

    @GetMapping("/members")
    public ApiResponse<List<MemberSearchResponseDto>> getAllMembers() {
        return ApiResponse.success("200", "전체 회원 조회 성공", memberService.getAllMembers());
    }

    @GetMapping("/admin/members")
    public ApiResponse<Page<MemberSearchResponseDto>> getAllMembersAdmin(Pageable pageable) {
        return ApiResponse.success("200", "전체 회원 조회 성공", memberService.getAllMembersAdmin(pageable));
    }

    @GetMapping("/admin/members/{memberId}")
    public ApiResponse<AdminMemberDetailResponseDto> getAdminMemberDetail(@PathVariable Long memberId) {
        return ApiResponse.success("200", "조회 성공", memberService.getAdminMemberDetail(memberId));
    }
}
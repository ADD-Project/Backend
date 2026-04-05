package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.MemberDetailResponse;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.User;
import com.example.ADD.project.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 새로운 회원을 저장하거나 기존 회원 정보를 수정합니다.
     * @param user 저장할 회원 엔티티
     * @return 저장된 회원 엔티티
     */
    @Transactional // 쓰기 작업이므로 별도의 트랜잭션 적용
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 전체 회원 목록을 조회합니다.
     * @return 전체 회원 리스트
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * ID(PK)를 기준으로 회원을 조회합니다.
     * @param id 회원 ID
     * @return 조회된 회원 (없을 경우 null)
     */
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 회원 코드를 이용해 로그인을 처리합니다.
     * @param userCode 회원 고유 코드
     * @return 코드가 일치하는 회원 (없을 경우 null)
     */
    public User login(String userCode) {
        return userRepository.findByUserCode(userCode).orElse(null);
    }

    /**
     * 이름으로 회원을 검색합니다. (부분 일치 지원)
     * @param name 검색할 이름
     * @return 이름이 포함된 회원 리스트
     */
    public List<User> searchByName(String name) {
        return userRepository.findByNameContaining(name);
    }

    /**
     * 회원의 상세 정보를 조회합니다.
     * 입소 일자와 부서 정보를 포함하며, 입소 당시의 부서원(입소일자 기준)을 함께 반환합니다.
     * @param id 조회할 회원 ID
     * @return 회원 상세 정보 DTO (존재하지 않으면 null)
     */
    public MemberDetailResponse getMemberDetail(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return null;
        }

        MemberDetailResponse response = new MemberDetailResponse();
        response.setMemberId(user.getId());
        response.setMemberCode(user.getUserCode());
        response.setName(user.getName());
        response.setProfileImageUrl(user.getProfileImageUrl());
        response.setAdmissionDate(user.getAdmissionDate());

        Department department = user.getDepartment();
        if (department != null) {
            MemberDetailResponse.DepartmentDto deptDto = new MemberDetailResponse.DepartmentDto(
                    department.getId(),
                    department.getDepartmentCode(),
                    department.getDepartmentName()
            );
            response.setAdmissionDepartment(deptDto);

            // 해당 부서에서 본인 입소일 이전(또는 같은 날)에 입소한 부서원 목록 조회 (본인 제외)
            List<User> pastMembers = userRepository.findDepartmentMembersBeforeOrAtAdmissionDate(
                    department.getId(), 
                    user.getAdmissionDate(), 
                    user.getId()
            );

            List<MemberDetailResponse.DepartmentMemberDto> pastMembersDto = pastMembers.stream()
                    .map(m -> new MemberDetailResponse.DepartmentMemberDto(
                            m.getId(),
                            m.getUserCode(),
                            m.getName(),
                            m.getProfileImageUrl()
                    ))
                    .collect(Collectors.toList());
            
            response.setDepartmentMembers(pastMembersDto);
        }

        return response;
    }
}
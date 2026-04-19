package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.department.DepartmentRequestDto;
import com.example.ADD.project.backend.dto.department.DepartmentResponseDto;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
import com.example.ADD.project.backend.repository.DepartmentNameHistoryRepository;
import com.example.ADD.project.backend.repository.DepartmentRepository;
import com.example.ADD.project.backend.repository.MemberDepartmentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentNameHistoryRepository departmentNameHistoryRepository;
    private final MemberDepartmentHistoryRepository memberDepartmentHistoryRepository;

    /**
     * 부서 전체 조회
     * 부서의 '모든 이름 변경 이력'을 시작일(startDate)과 함께 펼쳐서(Flat) 반환합니다.
     * 이름 변경 이력이 여러 개인 부서는 여러 개의 항목으로 나뉘어 반환됩니다.
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .flatMap(d -> {
                    List<DepartmentNameHistory> histories = departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(d);
                    
                    if (histories.isEmpty()) {
                        return java.util.stream.Stream.of(DepartmentResponseDto.builder()
                                .departmentId(d.getDepartmentId())
                                .deptCd(d.getDeptCd())
                                .deptName(d.getDeptCd())
                                .build());
                    }
                    
                    return histories.stream().map(h -> DepartmentResponseDto.builder()
                            .departmentId(d.getDepartmentId())
                            .deptCd(d.getDeptCd())
                            .deptName(h.getDeptName())
                            .startDate(h.getStartDate())
                            .departmentNameHistoryId(h.getDeptNameHistId())
                            .build());
                })
                .collect(Collectors.toList());
    }

    /**
     * 신규 부서 생성
     */
    @Transactional
    public void createDepartment(DepartmentRequestDto request) {
        String deptCode = request.getDeptCd();
        
        // 1. 부서코드를 입력하지 않는 경우, 임의로 'RANDOM_XXXX' 형식으로 세팅
        if (deptCode == null || deptCode.trim().isEmpty()) {
            deptCode = "RANDOM_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } else {
            if (departmentRepository.findByDeptCd(deptCode).isPresent()) {
                throw new RuntimeException("이미 존재하는 부서 코드입니다.");
            }
        }
        
        Department department = Department.builder().deptCd(deptCode).build();
        departmentRepository.save(department);

        // 2. 부서명이 함께 제공된 경우, 부서명 이력 생성
        if (request.getDeptName() != null && !request.getDeptName().trim().isEmpty()) {
            LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
            DepartmentNameHistory nameHistory = DepartmentNameHistory.builder()
                    .department(department).deptName(request.getDeptName()).startDate(startDate)
                    .build();
            departmentNameHistoryRepository.save(nameHistory);
        }
    }

    /**
     * 부서 이름 변경 이력 수정 (departmentNameHistoryId 사용)
     */
    @Transactional
    public void updateDepartmentHistory(Long historyId, DepartmentRequestDto request) {
        DepartmentNameHistory history = departmentNameHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("해당 부서 이력을 찾을 수 없습니다."));

        Department department = history.getDepartment();

        // 1. 부서 코드 업데이트
        if (request.getDeptCd() != null && !request.getDeptCd().trim().isEmpty() 
            && !department.getDeptCd().equals(request.getDeptCd())) {
            
            // RANDOM으로 시작하는 기존 부서의 부서코드 수정 로직
            if (department.getDeptCd().startsWith("RANDOM")) {
                // 입력하려는 부서코드가 이미 존재하는지 확인
                Optional<Department> existDept = departmentRepository.findByDeptCd(request.getDeptCd());
                if (existDept.isPresent() && !existDept.get().getDepartmentId().equals(department.getDepartmentId())) {
                    throw new RuntimeException("이미 존재하는 부서 코드입니다.");
                }
                department.updateDeptCd(request.getDeptCd());
            } else {
                throw new RuntimeException("기존에 지정된 정상 부서코드는 변경할 수 없습니다.");
            }
        }

        // 2. 부서명 변경 (이력 업데이트)
        if (request.getDeptName() != null && !request.getDeptName().trim().isEmpty()) {
            history.updateDeptName(request.getDeptName());
        }

        // 3. 시작일 변경 (이력 업데이트)
        if (request.getStartDate() != null) {
            history.updateStartDate(request.getStartDate());
        }
        
        // 연관된 MemberDepartmentHistory 로직은 부서코드(DeptCd)와 부서ID에 주로 의존하며,
        // MemberService에서 조회 시 departmentNameHistoryRepository.findDeptNameAtTime()을 통해
        // 동적으로 해당 시점의 부서명을 찾아오므로 부서명 이력만 수정하면 조회 시 자동으로 반영됩니다.
    }

    /**
     * 부서 이름 이력 개별 삭제 (departmentNameHistoryId 사용)
     */
    @Transactional
    public void deleteDepartmentHistory(Long historyId) {
        DepartmentNameHistory history = departmentNameHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("해당 부서 이력을 찾을 수 없습니다."));
        
        Department department = history.getDepartment();
        
        departmentNameHistoryRepository.delete(history);
        
        // 방금 삭제한 이력을 제외하고 이 부서에 남은 이력이 있는지 확인
        List<DepartmentNameHistory> remainingHistories = departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(department);
        
        // 남은 이력이 없다면 부서 자체와 관련된 사원의 부서 이력도 모두 삭제
        if (remainingHistories.isEmpty()) {
            // 해당 부서에 속했던 사원들의 부서 이력 삭제
            List<MemberDepartmentHistory> memberHistories = memberDepartmentHistoryRepository.findByDepartment(department);
            if (!memberHistories.isEmpty()) {
                memberDepartmentHistoryRepository.deleteAll(memberHistories);
            }
            // 부서 자체 삭제
            departmentRepository.delete(department);
        }
    }
}

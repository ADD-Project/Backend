# 사원 및 부서 관리 API 로직 문서 (Member & Department Service)

이 문서는 시스템 내 사원(Member)과 부서(Department)의 조회, 등록, 부서 이동, 부서 삭제(Soft Delete)와 관련된 비즈니스 로직 및 설계 의도를 팀원들에게 공유하기 위해 작성되었습니다.

## 1. 회원 상세 조회 (일반 사용자용)
**API:** `GET /member/{memberId}`  
**메서드:** `MemberService.getMemberDetail(Long memberId)`

일반 사용자가 다른 사원이나 본인의 상세 정보를 조회할 때 사용됩니다.
* **주요 반환 데이터:** 현재 속한 부서가 아닌, **처음 회사에 입사(입소)했을 당시의 부서명과 부서 코드**, 그리고 당시 해당 부서에 함께 속해있던 **동료 리스트(사번 포함)**입니다.
* **설계 의도:** 사원의 초기 소속 정보를 보여주는 화면 기획에 맞추어, 부서 이력 중 **최초 이력(index 0)**만 추출하여 반환하도록 설계되었습니다. 부서명이 추후에 변경되거나 부서 자체가 삭제되었더라도 이력 데이터(DepartmentNameHistory)를 바탕으로 "입사 당시의 부서 이름"을 온전하게 복원해냅니다.

## 2. 사원 단일 등록 및 부서 이력 생성
**API:** `POST /admin/import/single`  
**메서드:** `MemberService.registerSingleMember(SingleMemberRegisterRequestDto request)`

엑셀 등 파일 업로드가 아닌 관리자가 직접 사원 1명의 정보를 등록하거나 부서를 배치할 때 사용됩니다.
* **신규/기존 사원 분기:** 이미 등록된 사번(`memberCode`)인지 `findByMemberCode`로 조회합니다. 사원이 없으면 새로 생성하고, 이미 있다면 **기존 사원 객체를 재사용**하여 새로운 부서 이력만 추가합니다.
* **부서 이력(History) 종료일 업데이트:** 사원이 기존에 다른 부서에 있었거나, 혹은 같은 부서였지만 "부서명"이 바뀐 경우 새로운 이력을 추가합니다. 이때, **가장 최근 이력의 종료일(`endDate`)에 `새로운 시작일 - 1일`을 자동으로 입력**하여 이력 단절 없이 연속되도록 보장합니다.
* **중복 방지 조건:** 이전 이력의 `Department ID`와 `입사일 기준 부서명`을 모두 체크합니다. 부서 ID가 같더라도 이름이 바뀌었다면 새로운 이력으로 인정하며, ID와 이름이 모두 완전히 똑같다면 중복 삽입 에러를 반환합니다.
* **한글 데이터 매핑:** 부서 내 지역 정보(예: "부산", "마산")는 Enum(`RegionType`)으로 관리되지만 DB 제약조건 충돌을 방지하기 위해 JPA `@Converter`(`RegionTypeConverter`)를 통해 한글 문자열로 안전하게 변환되어 저장됩니다.

## 3. 부서 삭제 (Soft Delete) 처리
**API:** `DELETE /departments/{departmentId}`  
**메서드:** `DepartmentService.deleteDepartment(Long departmentId)`

관리자가 부서를 삭제할 때 사용하는 로직입니다.
* **설계 의도:** 부서를 물리적으로 삭제(`DELETE` 쿼리)할 경우, 해당 부서에 속했던 사원들의 과거 이력을 조회할 수 없는 치명적인 문제가 발생합니다.
* **해결 방안:** 물리적 삭제 대신 **Soft Delete**를 도입했습니다. 부서 삭제 시 `Department` 엔티티의 `closedAt`(종료일) 필드에 현재 날짜(`LocalDate.now()`)를 기록합니다.
* **조회 조건 분리:** 
  * 일반적인 부서 전체 목록 조회(`getAllDepartments`) 시에는 `findByClosedAtIsNull()` 메서드를 호출하여 **삭제된 부서를 필터링**하여 노출하지 않습니다.
  * 그러나 사원의 과거 이력 조회(`findDeptNameAtTime`) 시에는 `closedAt`과 상관없이 고유 ID로 조인하여 과거 부서의 이름을 정상적으로 가져옵니다.

## 4. 회원 상세 조회 (관리자용)
**API:** `GET /admin/members/{memberId}`  
**메서드:** `MemberService.getAdminMemberDetail(Long memberId)`

관리자가 특정 사원의 전체 이력을 한눈에 파악하기 위해 사용됩니다.
* **주요 반환 데이터:** 사번(`memberCode`), **최초 입소 부서명, 입소 날짜**, 그리고 **모든 부서 이동 이력 리스트**를 반환합니다.
* **부서 이동 이력(`departmentHistories`):** `MemberDepartmentHistory` 테이블에 기록된 모든 이력을 순회합니다. 각 이력이 생성되었던 시점(`startDate`)을 기준으로 `DepartmentNameHistory`에서 당시의 부서명을 찾아 반환합니다. 즉, 사원이 거쳐 간 부서들의 이름이 시간에 따라 어떻게 바뀌었는지 정확하게 보여줍니다.

## 5. 관리자 사원 전체 조회 (페이지네이션)
**API:** `GET /admin/members`  
**메서드:** `MemberService.getAllMembersAdmin(Pageable pageable)`

관리자가 다수의 사원 목록을 관리할 수 있도록, 기존 전체 조회와 달리 **Spring Data JPA의 `Pageable`을 활용한 페이지네이션**이 적용되어 있습니다. 클라이언트 측에서 `page`, `size` 파라미터를 통해 효율적으로 데이터를 로딩할 수 있습니다.

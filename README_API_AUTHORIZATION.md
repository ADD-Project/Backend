# API 권한 및 접근 제어 안내 (일반 사원 vs 관리자)

이 문서는 ADD 프로젝트의 백엔드 시스템에서 **일반 사원(Member)**과 **관리자(Admin)**가 접근할 수 있는 API 목록과 권한 제어 방식에 대해 설명합니다.

---

## 1. 권한 제어 방식 (Session + Interceptor)
본 시스템은 `HttpSession`을 이용한 세션 기반 인증을 사용합니다. 로그인을 수행하면 서버에서 세션을 발급하며, 스프링 인터셉터(`HandlerInterceptor`)가 요청의 URL 패턴에 따라 세션에 저장된 권한(`Role`)을 확인합니다.

*   **`MemberCheckInterceptor`**: 로그인이 되어 있는지(세션에 `LOGIN_MEMBER_ID`가 존재하는지) 검사합니다.
*   **`AdminCheckInterceptor`**: 로그인이 되어 있으면서, 세션의 `LOGIN_MEMBER_ROLE` 값이 `"ADMIN"`인지 검사합니다.

### 인터셉터 적용 규칙 (`WebConfig.java`)
*   `/admin/**`: **관리자 전용 API** (단, `/admin/login`은 권한 검사 제외)
*   `/members/**`, `/member/**`, `/departments/**`: **일반 사원 및 관리자 공통 접근 API** (단, `/member/login`은 권한 검사 제외)

---

## 2. 권한별 접근 가능 API 목록

### 🔓 모두 접근 가능 (비로그인 상태)
*   `POST /member/login`: 일반 사원 로그인
*   `POST /admin/login`: 관리자 로그인

### 🧑‍💼 일반 사원 (Member) 접근 가능 API
로그인에 성공하여 세션을 발급받은 모든 사원이 호출할 수 있는 API입니다. 관리자도 이 API들을 호출할 수 있습니다.

*   **회원 조회**
    *   `GET /member/{memberId}`: 사원 상세 정보 조회 (입소 부서 이력 및 동료 리스트 반환)
    *   `GET /members/search?name={이름}`: 사원 이름으로 목록 검색
    *   `GET /members/admission-years/{year}`: 특정 연도의 입소 사원 목록 조회
    *   `GET /members`: 전체 사원 목록 조회
*   **부서 조회**
    *   `GET /departments`: 현재 활성화된 전체 부서 목록 조회
*   **기타**
    *   `POST /logout`: 로그아웃

### 👑 관리자 (Admin) 전용 API
`/admin/` 경로로 시작하는 API들은 관리자 로그인 세션이 있어야만 호출 가능합니다. 일반 사원이 접근 시 `403 Forbidden` 처리됩니다.

*   **관리자 기능**
    *   `POST /admin/password`: 관리자 비밀번호(고유번호) 변경 (영문+숫자 8자리 이상 유효성 검증 포함)
*   **사원 관리 (CRUD 및 대량 등록)**
    *   `GET /admin/members`: 관리자용 사원 전체 목록 조회 (페이징 적용 - `Pageable` 사용)
    *   `GET /admin/members/{memberId}`: 관리자용 사원 1명 상세 조회 (부서 이동 이력 리스트 포함, 입소 부서 동료 목록 미포함)
    *   `POST /admin/import/single`: 관리자가 사원 1명 단일 등록 및 부서 배치
    *   `POST /admin/import/files`: 엑셀 파일을 통한 대량 사원 등록 및 부서 이력 자동 처리
    *   `PUT /members/{memberId}`: 특정 사원 정보(이름, 프로필 이미지 등) 수정 (URL 패턴은 `/members/` 이지만, 보통 관리자 화면에서 호출하도록 기획됨)
*   **부서 관리 (CRUD)**
    *   `POST /departments`: 신규 부서 생성
    *   `PUT /departments/{departmentId}`: 기존 부서 이름 또는 부서 코드 수정
    *   `DELETE /departments/{departmentId}`: 부서 삭제 (물리적 삭제가 아닌 `closedAt`을 설정하는 Soft Delete 처리)

---

## 3. 프론트엔드 연동 시 주의사항
*   로그인 후 서버에서 내려주는 **`Set-Cookie: JSESSIONID=...`** 값을 브라우저나 클라이언트가 저장하고, 이후 API 요청 시 해당 쿠키를 헤더에 포함시켜 전송해야 권한 인증이 정상적으로 이루어집니다.

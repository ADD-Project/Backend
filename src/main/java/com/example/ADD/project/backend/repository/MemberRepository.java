package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberCode(String memberCode);
    List<Member> findByNameContaining(String name);
    List<Member> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
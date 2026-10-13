package com.alumni.management.student.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alumni.management.student.entity.StudentProfile;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

	Optional<StudentProfile> findByUserId(Long userId);

	boolean existsByUserId(Long userId);
}

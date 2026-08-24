package com.izischool.parent.repository;

import com.izischool.parent.domain.StudentParent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentParentRepository extends JpaRepository<StudentParent, UUID> {

    @EntityGraph(attributePaths = {"student", "parent"})
    List<StudentParent> findByStudent_Id(UUID studentId);

    @EntityGraph(attributePaths = {"student", "parent"})
    List<StudentParent> findByParent_Id(UUID parentId);

    @EntityGraph(attributePaths = {"student", "parent"})
    Optional<StudentParent> findByStudent_IdAndParent_Id(UUID studentId, UUID parentId);

    boolean existsByStudent_IdAndParent_Id(UUID studentId, UUID parentId);

    @EntityGraph(attributePaths = {"student", "parent"})
    @Query("SELECT sp FROM StudentParent sp WHERE sp.student.id = :studentId AND sp.isFinancialContact = true")
    Optional<StudentParent> findFinancialContactByStudentId(@Param("studentId") UUID studentId);
}

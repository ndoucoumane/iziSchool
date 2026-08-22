package com.izischool.parent.repository;

import com.izischool.parent.domain.StudentParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentParentRepository extends JpaRepository<StudentParent, UUID> {

    List<StudentParent> findByStudent_Id(UUID studentId);

    List<StudentParent> findByParent_Id(UUID parentId);

    Optional<StudentParent> findByStudent_IdAndParent_Id(UUID studentId, UUID parentId);

    @Query("SELECT sp FROM StudentParent sp WHERE sp.student.id = :studentId AND sp.isFinancialContact = true")
    Optional<StudentParent> findFinancialContactByStudentId(@Param("studentId") UUID studentId);
}

package com.izischool.integration;

import com.izischool.common.exception.TenantAccessException;
import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import com.izischool.school.service.SchoolService;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.repository.StudentRepository;
import com.izischool.student.service.StudentService;
import com.izischool.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenantIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Isolation stricte multi-tenant en base de données entre School A et School B")
    void testMultiTenantDataIsolation() {
        // 1. Création de deux écoles distinctes
        School schoolA = schoolService.createSchool(School.builder()
                .name("École Primaire A")
                .code("SCH-A-" + UUID.randomUUID().toString().substring(0, 6))
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build());

        School schoolB = schoolService.createSchool(School.builder()
                .name("École Primaire B")
                .code("SCH-B-" + UUID.randomUUID().toString().substring(0, 6))
                .currency("XOF")
                .status(SchoolStatus.ACTIVE)
                .build());

        // 2. Création d'élèves pour chaque école
        Student studentA1 = studentService.createStudent(Student.builder()
                .school(schoolA)
                .firstName("Mamadou")
                .lastName("Sow")
                .gender(Gender.MALE)
                .status(StudentStatus.ACTIVE)
                .build());

        Student studentA2 = studentService.createStudent(Student.builder()
                .school(schoolA)
                .firstName("Aïcha")
                .lastName("Diop")
                .gender(Gender.FEMALE)
                .status(StudentStatus.ACTIVE)
                .build());

        Student studentB1 = studentService.createStudent(Student.builder()
                .school(schoolB)
                .firstName("Jean")
                .lastName("Gomis")
                .gender(Gender.MALE)
                .status(StudentStatus.ACTIVE)
                .build());

        // 3. Vérification des requêtes de liste filtrées par école
        Page<Student> studentsSchoolA = studentService.getStudentsBySchool(schoolA.getId(), PageRequest.of(0, 10));
        assertThat(studentsSchoolA.getContent()).extracting(Student::getId)
                .containsExactlyInAnyOrder(studentA1.getId(), studentA2.getId())
                .doesNotContain(studentB1.getId());

        Page<Student> studentsSchoolB = studentService.getStudentsBySchool(schoolB.getId(), PageRequest.of(0, 10));
        assertThat(studentsSchoolB.getContent()).extracting(Student::getId)
                .containsExactlyInAnyOrder(studentB1.getId())
                .doesNotContain(studentA1.getId(), studentA2.getId());

        // 4. Test d'accès inter-tenant avec contexte utilisateur actif
        TenantContext.set(TenantContext.builder()
                .schoolId(schoolA.getId())
                .schoolCode(schoolA.getCode())
                .keycloakUserId("user-director-school-a")
                .roles(Set.of("ROLE_DIRECTOR"))
                .superAdmin(false)
                .build());

        // L'utilisateur de School A tente de charger les élèves de School B
        assertThatThrownBy(() -> studentService.getStudentsBySchool(schoolB.getId(), PageRequest.of(0, 10)))
                .isInstanceOf(TenantAccessException.class)
                .hasMessageContaining("Access denied");

        // L'utilisateur de School A tente de charger l'élève de School B
        assertThatThrownBy(() -> studentService.getStudentById(studentB1.getId(), schoolB.getId()))
                .isInstanceOf(TenantAccessException.class);
    }
}

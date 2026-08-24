package com.izischool.student.service;

import com.izischool.academic.repository.SchoolClassRepository;
import com.izischool.common.exception.BusinessException;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentRelationship;
import com.izischool.parent.domain.ParentStatus;
import com.izischool.parent.repository.ParentRepository;
import com.izischool.parent.service.ParentService;
import com.izischool.school.domain.School;
import com.izischool.student.domain.Gender;
import com.izischool.student.domain.ImportStatus;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentImport;
import com.izischool.student.domain.StudentStatus;
import com.izischool.student.dto.StudentImportValidationResponse;
import com.izischool.student.dto.StudentImportValidationResponse.ImportRowError;
import com.izischool.student.repository.StudentImportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final StudentImportRepository studentImportRepository;
    private final StudentService studentService;
    private final StudentEnrollmentService enrollmentService;
    private final ParentService parentService;
    private final ParentRepository parentRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public StudentImportValidationResponse parseAndValidateImport(MultipartFile file, School school) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "import.csv";
        List<ImportRowError> errors = new ArrayList<>();
        List<ParsedStudentRow> validRows = new ArrayList<>();
        int rowNumber = 1;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            for (CSVRecord record : csvParser) {
                rowNumber++;
                String firstName = record.isMapped("firstName") ? record.get("firstName") : null;
                String lastName = record.isMapped("lastName") ? record.get("lastName") : null;
                String dateOfBirthStr = record.isMapped("dateOfBirth") ? record.get("dateOfBirth") : null;
                String genderStr = record.isMapped("gender") ? record.get("gender") : "MALE";
                String className = record.isMapped("class") ? record.get("class") : null;
                String parentFirstName = record.isMapped("parentFirstName") ? record.get("parentFirstName") : null;
                String parentLastName = record.isMapped("parentLastName") ? record.get("parentLastName") : null;
                String parentPhone = record.isMapped("parentPhone") ? record.get("parentPhone") : null;

                if (firstName == null || firstName.isBlank()) {
                    errors.add(new ImportRowError(rowNumber, "firstName", "First name is required"));
                    continue;
                }
                if (lastName == null || lastName.isBlank()) {
                    errors.add(new ImportRowError(rowNumber, "lastName", "Last name is required"));
                    continue;
                }
                if (parentPhone == null || parentPhone.isBlank()) {
                    errors.add(new ImportRowError(rowNumber, "parentPhone", "Parent phone number is required"));
                    continue;
                }

                LocalDate dob = null;
                if (dateOfBirthStr != null && !dateOfBirthStr.isBlank()) {
                    try {
                        dob = LocalDate.parse(dateOfBirthStr, DateTimeFormatter.ISO_DATE);
                    } catch (Exception e) {
                        dob = LocalDate.of(2012, 1, 1);
                    }
                }

                Gender gender = Gender.MALE;
                try {
                    if (genderStr != null) gender = Gender.valueOf(genderStr.toUpperCase());
                } catch (Exception ignored) {
                }

                validRows.add(new ParsedStudentRow(firstName, lastName, dob, gender, className, parentFirstName, parentLastName, parentPhone));
            }
        } catch (Exception e) {
            log.error("Failed to parse student import file", e);
            throw new BusinessException("Failed to parse import file: " + e.getMessage());
        }

        StudentImport studentImport = StudentImport.builder()
                .school(school)
                .fileName(filename)
                .totalRows(rowNumber - 1)
                .successRows(validRows.size())
                .failedRows(errors.size())
                .status(errors.isEmpty() ? ImportStatus.COMPLETED : ImportStatus.PARTIALLY_COMPLETED)
                .build();
        StudentImport savedImport = studentImportRepository.save(studentImport);

        // Execute import of valid rows immediately
        for (ParsedStudentRow row : validRows) {
            try {
                Student student = Student.builder()
                        .school(school)
                        .firstName(row.firstName)
                        .lastName(row.lastName)
                        .dateOfBirth(row.dob)
                        .gender(row.gender)
                        .status(StudentStatus.ACTIVE)
                        .build();
                Student created = studentService.createStudent(student);

                // Parent link
                if (row.parentPhone != null && !row.parentPhone.isBlank()) {
                    Parent parent = parentRepository.findBySchool_IdAndPhoneAndDeletedFalse(school.getId(), row.parentPhone)
                            .orElseGet(() -> parentService.createParent(Parent.builder()
                                    .school(school)
                                    .firstName(row.parentFirstName != null ? row.parentFirstName : "Parent")
                                    .lastName(row.parentLastName != null ? row.parentLastName : row.lastName)
                                    .phone(row.parentPhone)
                                    .status(ParentStatus.ACTIVE)
                                    .build()));

                    parentService.linkStudentAndParent(created, parent, ParentRelationship.FATHER, true, true);
                }
            } catch (Exception e) {
                log.warn("Failed to create imported student {}", row.firstName, e);
            }
        }

        return StudentImportValidationResponse.builder()
                .importId(savedImport.getId())
                .totalRows(rowNumber - 1)
                .validRows(validRows.size())
                .invalidRows(errors.size())
                .errors(errors)
                .build();
    }

    private record ParsedStudentRow(
            String firstName,
            String lastName,
            LocalDate dob,
            Gender gender,
            String className,
            String parentFirstName,
            String parentLastName,
            String parentPhone
    ) {}
}

package com.izischool.dashboard.service;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.repository.AcademicYearRepository;
import com.izischool.academic.repository.SchoolClassRepository;
import com.izischool.common.exception.ResourceNotFoundException;
import com.izischool.common.response.PageResponse;
import com.izischool.common.util.MoneyUtils;
import com.izischool.dashboard.dto.ClassEnrollmentStatsResponse;
import com.izischool.dashboard.dto.DirectorDashboardResponse;
import com.izischool.dashboard.dto.DirectorPaymentSummaryResponse;
import com.izischool.dashboard.dto.FeeCategoryBreakdownResponse;
import com.izischool.dashboard.dto.FeeTypeSummaryResponse;
import com.izischool.dashboard.dto.GlobalEnrollmentStatsResponse;
import com.izischool.dashboard.dto.MonthlyScheduleStatusResponse;
import com.izischool.dashboard.dto.OverdueStudentPaymentResponse;
import com.izischool.dashboard.dto.PaymentEvolutionResponse;
import com.izischool.dashboard.dto.PaymentMethodBreakdownResponse;
import com.izischool.dashboard.dto.StudentFinancialStatusResponse;
import com.izischool.dashboard.dto.UpcomingDueDateResponse;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import com.izischool.finance.repository.FeeRepository;
import com.izischool.finance.repository.PaymentScheduleRepository;
import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.StudentParent;
import com.izischool.parent.repository.StudentParentRepository;
import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentMethod;
import com.izischool.payment.domain.PaymentStatus;
import com.izischool.payment.repository.PaymentRepository;
import com.izischool.school.domain.School;
import com.izischool.school.service.SchoolService;
import com.izischool.student.domain.EnrollmentStatus;
import com.izischool.student.domain.Student;
import com.izischool.student.domain.StudentEnrollment;
import com.izischool.student.repository.StudentEnrollmentRepository;
import com.izischool.student.repository.StudentRepository;
import com.izischool.tenant.service.TenantValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorDashboardService {

    private final SchoolService schoolService;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final PaymentRepository paymentRepository;
    private final FeeRepository feeRepository;
    private final StudentParentRepository studentParentRepository;
    private final TenantValidationService tenantValidationService;

    // ---------------------------------------------------------------------------------------------
    // Helper: Academic Year Resolution
    // ---------------------------------------------------------------------------------------------
    public Optional<AcademicYear> resolveAcademicYear(UUID schoolId, UUID requestedYearId) {
        if (requestedYearId != null) {
            return academicYearRepository.findByIdAndSchool_Id(requestedYearId, schoolId);
        }
        Optional<AcademicYear> activeYear = academicYearRepository.findBySchool_IdAndStatus(schoolId, AcademicYearStatus.ACTIVE);
        if (activeYear.isPresent()) {
            return activeYear;
        }
        List<AcademicYear> years = academicYearRepository.findBySchool_IdOrderByStartDateDesc(schoolId);
        return years.isEmpty() ? Optional.empty() : Optional.of(years.get(0));
    }

    private String getSchoolCurrency(UUID schoolId) {
        School school = schoolService.getSchoolById(schoolId);
        return (school != null && school.getCurrency() != null) ? school.getCurrency() : "XOF";
    }

    // ---------------------------------------------------------------------------------------------
    // 1. Vue Globale Tout-en-un
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public DirectorDashboardResponse getDirectorDashboard(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        UUID resolvedYearId = yearOpt.map(AcademicYear::getId).orElse(null);
        String yearName = yearOpt.map(AcademicYear::getName).orElse("N/A");
        String currency = getSchoolCurrency(schoolId);

        LocalDate today = LocalDate.now();

        GlobalEnrollmentStatsResponse enrollment = getGlobalEnrollmentStats(schoolId, resolvedYearId);
        DirectorPaymentSummaryResponse payments = getPaymentSummary(schoolId, resolvedYearId);
        FeeTypeSummaryResponse registrations = getFeeTypeSummary(schoolId, resolvedYearId, FeeType.REGISTRATION);
        FeeTypeSummaryResponse tuition = getFeeTypeSummary(schoolId, resolvedYearId, FeeType.TUITION);
        MonthlyScheduleStatusResponse currentMonthSchedules = getMonthlyScheduleStatus(schoolId, resolvedYearId, today.getMonthValue(), today.getYear());

        List<ClassEnrollmentStatsResponse> classes = getClassesEnrollmentStats(schoolId, resolvedYearId);
        List<FeeCategoryBreakdownResponse> feeCategories = getFeeCategoryBreakdown(schoolId, resolvedYearId);
        List<PaymentMethodBreakdownResponse> paymentMethods = getPaymentMethodBreakdown(schoolId, resolvedYearId, null, null);
        List<PaymentEvolutionResponse> recentEvolution = getPaymentEvolution(schoolId, resolvedYearId, "DAY", today.minusDays(14), today);

        PageResponse<OverdueStudentPaymentResponse> overduePage = getOverdueStudents(schoolId, resolvedYearId, null, PageRequest.of(0, 5));
        List<UpcomingDueDateResponse> upcomingDueDates = getUpcomingDueDates(schoolId, resolvedYearId, 30);

        return DirectorDashboardResponse.builder()
                .academicYearId(resolvedYearId)
                .academicYearName(yearName)
                .currency(currency)
                .enrollment(enrollment)
                .payments(payments)
                .registrations(registrations)
                .tuition(tuition)
                .currentMonthSchedules(currentMonthSchedules)
                .classes(classes)
                .feeCategories(feeCategories)
                .paymentMethods(paymentMethods)
                .recentEvolution(recentEvolution)
                .recentOverdue(overduePage.getContent())
                .upcomingDueDates(upcomingDueDates)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Effectifs Globaux
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public GlobalEnrollmentStatsResponse getGlobalEnrollmentStats(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);

        long totalStudents = studentRepository.countBySchool_IdAndDeletedFalse(schoolId);

        if (yearOpt.isEmpty()) {
            return GlobalEnrollmentStatsResponse.builder()
                    .totalStudents(totalStudents)
                    .totalClasses(0)
                    .totalCapacity(0)
                    .totalEnrolledStudents(0)
                    .totalAvailableSeats(0)
                    .globalEnrollmentRate(0.0)
                    .pendingRegistrations(0)
                    .activeRegistrations(0)
                    .completedRegistrations(0)
                    .cancelledRegistrations(0)
                    .build();
        }

        AcademicYear year = yearOpt.get();
        List<SchoolClass> classes = schoolClassRepository.findBySchool_IdAndAcademicYear_Id(schoolId, year.getId());
        long totalCapacity = classes.stream()
                .mapToLong(c -> c.getCapacity() != null ? c.getCapacity() : 0)
                .sum();

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, year.getId());

        long pending = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.PENDING).count();
        long active = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE).count();
        long completed = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
        long cancelled = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.CANCELLED).count();

        long enrolledStudents = enrollments.stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.CANCELLED)
                .count();

        long availableSeats = Math.max(0, totalCapacity - enrolledStudents);
        double enrollmentRate = totalCapacity > 0
                ? BigDecimal.valueOf(enrolledStudents * 100.0 / totalCapacity).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return GlobalEnrollmentStatsResponse.builder()
                .academicYearId(year.getId())
                .academicYearName(year.getName())
                .totalStudents(totalStudents)
                .totalClasses(classes.size())
                .totalCapacity(totalCapacity)
                .totalEnrolledStudents(enrolledStudents)
                .totalAvailableSeats(availableSeats)
                .globalEnrollmentRate(enrollmentRate)
                .pendingRegistrations(pending)
                .activeRegistrations(active)
                .completedRegistrations(completed)
                .cancelledRegistrations(cancelled)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Situation des Classes
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ClassEnrollmentStatsResponse> getClassesEnrollmentStats(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        if (yearOpt.isEmpty()) {
            return Collections.emptyList();
        }

        AcademicYear year = yearOpt.get();
        String currency = getSchoolCurrency(schoolId);
        List<SchoolClass> classes = schoolClassRepository.findBySchool_IdAndAcademicYear_Id(schoolId, year.getId());

        List<StudentEnrollment> allEnrollments = studentEnrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, year.getId());
        List<PaymentSchedule> allSchedules = paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, year.getId());

        Map<UUID, List<StudentEnrollment>> enrollmentsByClass = allEnrollments.stream()
                .collect(Collectors.groupingBy(e -> e.getSchoolClass().getId()));

        Map<UUID, List<PaymentSchedule>> schedulesByClass = allSchedules.stream()
                .filter(ps -> ps.getEnrollment() != null && ps.getEnrollment().getSchoolClass() != null)
                .collect(Collectors.groupingBy(ps -> ps.getEnrollment().getSchoolClass().getId()));

        List<ClassEnrollmentStatsResponse> result = new ArrayList<>();
        for (SchoolClass c : classes) {
            List<StudentEnrollment> classEnrollments = enrollmentsByClass.getOrDefault(c.getId(), Collections.emptyList());
            List<PaymentSchedule> classSchedules = schedulesByClass.getOrDefault(c.getId(), Collections.emptyList());

            long enrolledCount = classEnrollments.stream()
                    .filter(e -> e.getStatus() != EnrollmentStatus.CANCELLED)
                    .count();
            long pendingCount = classEnrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.PENDING)
                    .count();
            long activeCount = classEnrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .count();

            int capacity = c.getCapacity() != null ? c.getCapacity() : 0;
            long availableSeats = Math.max(0, capacity - enrolledCount);
            double occupancyRate = capacity > 0
                    ? BigDecimal.valueOf(enrolledCount * 100.0 / capacity).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            BigDecimal expected = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            BigDecimal remaining = BigDecimal.ZERO;

            for (PaymentSchedule ps : classSchedules) {
                if (ps.getStatus() != PaymentScheduleStatus.CANCELLED) {
                    expected = expected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                    collected = collected.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                    remaining = remaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
                }
            }

            BigDecimal collectionRate = BigDecimal.ZERO;
            if (MoneyUtils.isPositive(expected)) {
                collectionRate = collected.multiply(BigDecimal.valueOf(100)).divide(expected, 2, RoundingMode.HALF_UP);
            }

            result.add(ClassEnrollmentStatsResponse.builder()
                    .classId(c.getId())
                    .className(c.getName())
                    .classCode(c.getCode())
                    .gradeLevelName(c.getGradeLevel() != null ? c.getGradeLevel().getName() : "N/A")
                    .capacity(capacity)
                    .enrolledCount(enrolledCount)
                    .availableSeats(availableSeats)
                    .occupancyRate(occupancyRate)
                    .pendingCount(pendingCount)
                    .activeCount(activeCount)
                    .totalExpected(MoneyUtils.scale(expected))
                    .totalCollected(MoneyUtils.scale(collected))
                    .totalRemaining(MoneyUtils.scale(remaining))
                    .collectionRate(collectionRate)
                    .currency(currency)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public ClassEnrollmentStatsResponse getClassEnrollmentStats(UUID schoolId, UUID classId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        SchoolClass c = schoolClassRepository.findByIdAndSchool_Id(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass", classId));

        String currency = getSchoolCurrency(schoolId);
        UUID academicYearId = c.getAcademicYear() != null ? c.getAcademicYear().getId() : null;

        List<StudentEnrollment> classEnrollments = academicYearId != null
                ? studentEnrollmentRepository.findBySchool_IdAndAcademicYear_IdAndSchoolClass_Id(schoolId, academicYearId, classId)
                : Collections.emptyList();

        List<PaymentSchedule> classSchedules = paymentScheduleRepository.findBySchool_IdAndEnrollment_SchoolClass_IdAndDeletedFalse(schoolId, classId);

        long enrolledCount = classEnrollments.stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.CANCELLED)
                .count();
        long pendingCount = classEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.PENDING)
                .count();
        long activeCount = classEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .count();

        int capacity = c.getCapacity() != null ? c.getCapacity() : 0;
        long availableSeats = Math.max(0, capacity - enrolledCount);
        double occupancyRate = capacity > 0
                ? BigDecimal.valueOf(enrolledCount * 100.0 / capacity).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;

        for (PaymentSchedule ps : classSchedules) {
            if (ps.getStatus() != PaymentScheduleStatus.CANCELLED) {
                expected = expected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                collected = collected.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                remaining = remaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
            }
        }

        BigDecimal collectionRate = BigDecimal.ZERO;
        if (MoneyUtils.isPositive(expected)) {
            collectionRate = collected.multiply(BigDecimal.valueOf(100)).divide(expected, 2, RoundingMode.HALF_UP);
        }

        return ClassEnrollmentStatsResponse.builder()
                .classId(c.getId())
                .className(c.getName())
                .classCode(c.getCode())
                .gradeLevelName(c.getGradeLevel() != null ? c.getGradeLevel().getName() : "N/A")
                .capacity(capacity)
                .enrolledCount(enrolledCount)
                .availableSeats(availableSeats)
                .occupancyRate(occupancyRate)
                .pendingCount(pendingCount)
                .activeCount(activeCount)
                .totalExpected(MoneyUtils.scale(expected))
                .totalCollected(MoneyUtils.scale(collected))
                .totalRemaining(MoneyUtils.scale(remaining))
                .collectionRate(collectionRate)
                .currency(currency)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // 4. Synthèse Financière & Recouvrement
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public DirectorPaymentSummaryResponse getPaymentSummary(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        String currency = getSchoolCurrency(schoolId);
        LocalDate today = LocalDate.now();

        List<PaymentSchedule> schedules = yearOpt.isPresent()
                ? paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, yearOpt.get().getId())
                : paymentScheduleRepository.findBySchool_IdAndDeletedFalse(schoolId);

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;

        Map<UUID, List<PaymentSchedule>> studentSchedulesMap = new HashMap<>();

        for (PaymentSchedule ps : schedules) {
            if (ps.getStatus() != PaymentScheduleStatus.CANCELLED) {
                totalExpected = totalExpected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                totalCollected = totalCollected.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                totalRemaining = totalRemaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

                boolean isOverdue = ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                        (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today));
                if (isOverdue) {
                    totalOverdue = totalOverdue.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
                }

                if (ps.getStudent() != null) {
                    studentSchedulesMap.computeIfAbsent(ps.getStudent().getId(), k -> new ArrayList<>()).add(ps);
                }
            }
        }

        BigDecimal collectionRate = BigDecimal.ZERO;
        if (MoneyUtils.isPositive(totalExpected)) {
            collectionRate = totalCollected.multiply(BigDecimal.valueOf(100)).divide(totalExpected, 2, RoundingMode.HALF_UP);
        }

        // Today & This Month payments
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        BigDecimal todayCollected = MoneyUtils.scale(paymentRepository.sumSuccessfulPaymentsBetween(schoolId, startOfDay, endOfDay));

        YearMonth currentYearMonth = YearMonth.now();
        Instant startOfMonth = currentYearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfMonth = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        BigDecimal thisMonthCollected = MoneyUtils.scale(paymentRepository.sumSuccessfulPaymentsBetween(schoolId, startOfMonth, endOfMonth));

        long upToDateCount = 0;
        long partialCount = 0;
        long unpaidCount = 0;
        long overdueCount = 0;

        for (Map.Entry<UUID, List<PaymentSchedule>> entry : studentSchedulesMap.entrySet()) {
            List<PaymentSchedule> studentSchedules = entry.getValue();
            BigDecimal stdExpected = BigDecimal.ZERO;
            BigDecimal stdPaid = BigDecimal.ZERO;
            BigDecimal stdRemaining = BigDecimal.ZERO;
            boolean hasOverdue = false;

            for (PaymentSchedule ps : studentSchedules) {
                stdExpected = stdExpected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                stdPaid = stdPaid.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                stdRemaining = stdRemaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

                if (ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                        (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today) && MoneyUtils.isPositive(ps.getRemainingAmount()))) {
                    hasOverdue = true;
                }
            }

            if (hasOverdue) {
                overdueCount++;
            } else if (MoneyUtils.isZero(stdRemaining) && MoneyUtils.isPositive(stdExpected)) {
                upToDateCount++;
            } else if (MoneyUtils.isPositive(stdPaid) && MoneyUtils.isPositive(stdRemaining)) {
                partialCount++;
            } else if (MoneyUtils.isZero(stdPaid) && MoneyUtils.isPositive(stdExpected)) {
                unpaidCount++;
            } else {
                upToDateCount++;
            }
        }

        long totalStudents = studentRepository.countBySchool_IdAndDeletedFalse(schoolId);

        return DirectorPaymentSummaryResponse.builder()
                .totalExpected(MoneyUtils.scale(totalExpected))
                .totalCollected(MoneyUtils.scale(totalCollected))
                .totalRemaining(MoneyUtils.scale(totalRemaining))
                .totalOverdue(MoneyUtils.scale(totalOverdue))
                .collectionRate(collectionRate)
                .todayCollected(todayCollected)
                .thisMonthCollected(thisMonthCollected)
                .upToDateStudentsCount(upToDateCount)
                .partialPaymentStudentsCount(partialCount)
                .unpaidStudentsCount(unpaidCount)
                .overdueStudentsCount(overdueCount)
                .totalStudentsCount(totalStudents)
                .currency(currency)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // 5. Situation par Type de Frais (Inscriptions & Scolarités)
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public FeeTypeSummaryResponse getFeeTypeSummary(UUID schoolId, UUID academicYearId, FeeType feeType) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        String currency = getSchoolCurrency(schoolId);
        LocalDate today = LocalDate.now();

        List<PaymentSchedule> schedules = yearOpt.isPresent()
                ? paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndFee_FeeTypeAndDeletedFalse(schoolId, yearOpt.get().getId(), feeType)
                : paymentScheduleRepository.findBySchool_IdAndFee_FeeTypeAndDeletedFalse(schoolId, feeType);

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;

        Map<UUID, List<PaymentSchedule>> studentSchedulesMap = new HashMap<>();

        for (PaymentSchedule ps : schedules) {
            if (ps.getStatus() != PaymentScheduleStatus.CANCELLED) {
                totalExpected = totalExpected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                totalCollected = totalCollected.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                totalRemaining = totalRemaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

                boolean isOverdue = ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                        (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today));
                if (isOverdue) {
                    totalOverdue = totalOverdue.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
                }

                if (ps.getStudent() != null) {
                    studentSchedulesMap.computeIfAbsent(ps.getStudent().getId(), k -> new ArrayList<>()).add(ps);
                }
            }
        }

        BigDecimal collectionRate = BigDecimal.ZERO;
        if (MoneyUtils.isPositive(totalExpected)) {
            collectionRate = totalCollected.multiply(BigDecimal.valueOf(100)).divide(totalExpected, 2, RoundingMode.HALF_UP);
        }

        long paidCount = 0;
        long partialCount = 0;
        long unpaidCount = 0;
        long overdueCount = 0;

        for (Map.Entry<UUID, List<PaymentSchedule>> entry : studentSchedulesMap.entrySet()) {
            List<PaymentSchedule> studentSchedules = entry.getValue();
            BigDecimal stdExpected = BigDecimal.ZERO;
            BigDecimal stdPaid = BigDecimal.ZERO;
            BigDecimal stdRemaining = BigDecimal.ZERO;
            boolean hasOverdue = false;

            for (PaymentSchedule ps : studentSchedules) {
                stdExpected = stdExpected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                stdPaid = stdPaid.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                stdRemaining = stdRemaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

                if (ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                        (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today) && MoneyUtils.isPositive(ps.getRemainingAmount()))) {
                    hasOverdue = true;
                }
            }

            if (hasOverdue) {
                overdueCount++;
            } else if (MoneyUtils.isZero(stdRemaining) && MoneyUtils.isPositive(stdExpected)) {
                paidCount++;
            } else if (MoneyUtils.isPositive(stdPaid) && MoneyUtils.isPositive(stdRemaining)) {
                partialCount++;
            } else if (MoneyUtils.isZero(stdPaid) && MoneyUtils.isPositive(stdExpected)) {
                unpaidCount++;
            } else {
                paidCount++;
            }
        }

        String label = feeType == FeeType.REGISTRATION
                ? "Frais d'inscription"
                : (feeType == FeeType.TUITION ? "Frais de scolarité / Mensualités" : "Autres frais");

        return FeeTypeSummaryResponse.builder()
                .feeType(feeType)
                .feeTypeName(label)
                .totalExpected(MoneyUtils.scale(totalExpected))
                .totalCollected(MoneyUtils.scale(totalCollected))
                .totalRemaining(MoneyUtils.scale(totalRemaining))
                .totalOverdue(MoneyUtils.scale(totalOverdue))
                .collectionRate(collectionRate)
                .fullyPaidStudentsCount(paidCount)
                .partialStudentsCount(partialCount)
                .unpaidStudentsCount(unpaidCount)
                .overdueStudentsCount(overdueCount)
                .currency(currency)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // 6. Situation des Échéances / Mensualités du Mois
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public MonthlyScheduleStatusResponse getMonthlyScheduleStatus(UUID schoolId, UUID academicYearId, Integer month, Integer year) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        String currency = getSchoolCurrency(schoolId);
        LocalDate today = LocalDate.now();

        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();
        int targetYear = (year != null && year > 2000) ? year : today.getYear();

        LocalDate startOfMonth = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        List<PaymentSchedule> allSchedules = yearOpt.isPresent()
                ? paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, yearOpt.get().getId())
                : paymentScheduleRepository.findBySchool_IdAndDeletedFalse(schoolId);

        List<PaymentSchedule> monthSchedules = allSchedules.stream()
                .filter(ps -> ps.getDueDate() != null && !ps.getDueDate().isBefore(startOfMonth) && !ps.getDueDate().isAfter(endOfMonth))
                .filter(ps -> ps.getStatus() != PaymentScheduleStatus.CANCELLED)
                .toList();

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;

        long paidSchedules = 0;
        long partialSchedules = 0;
        long overdueSchedules = 0;
        long pendingSchedules = 0;

        Map<UUID, List<PaymentSchedule>> studentMonthMap = new HashMap<>();

        for (PaymentSchedule ps : monthSchedules) {
            totalExpected = totalExpected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
            totalCollected = totalCollected.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
            totalRemaining = totalRemaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

            boolean isOverdue = ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                    (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today) && MoneyUtils.isPositive(ps.getRemainingAmount()));

            if (isOverdue) {
                totalOverdue = totalOverdue.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
                overdueSchedules++;
            } else if (ps.getStatus() == PaymentScheduleStatus.PAID || MoneyUtils.isZero(ps.getRemainingAmount())) {
                paidSchedules++;
            } else if (MoneyUtils.isPositive(ps.getAmountPaid())) {
                partialSchedules++;
            } else {
                pendingSchedules++;
            }

            if (ps.getStudent() != null) {
                studentMonthMap.computeIfAbsent(ps.getStudent().getId(), k -> new ArrayList<>()).add(ps);
            }
        }

        BigDecimal collectionRate = BigDecimal.ZERO;
        if (MoneyUtils.isPositive(totalExpected)) {
            collectionRate = totalCollected.multiply(BigDecimal.valueOf(100)).divide(totalExpected, 2, RoundingMode.HALF_UP);
        }

        long upToDateStudents = 0;
        long overdueStudents = 0;

        for (Map.Entry<UUID, List<PaymentSchedule>> entry : studentMonthMap.entrySet()) {
            boolean studentHasOverdue = entry.getValue().stream().anyMatch(ps ->
                    ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                            (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today) && MoneyUtils.isPositive(ps.getRemainingAmount())));
            if (studentHasOverdue) {
                overdueStudents++;
            } else {
                upToDateStudents++;
            }
        }

        String periodLabel = startOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        periodLabel = periodLabel.substring(0, 1).toUpperCase(Locale.FRENCH) + periodLabel.substring(1);

        return MonthlyScheduleStatusResponse.builder()
                .month(targetMonth)
                .year(targetYear)
                .periodLabel(periodLabel)
                .totalExpected(MoneyUtils.scale(totalExpected))
                .totalCollected(MoneyUtils.scale(totalCollected))
                .totalRemaining(MoneyUtils.scale(totalRemaining))
                .totalOverdue(MoneyUtils.scale(totalOverdue))
                .collectionRate(collectionRate)
                .totalSchedulesCount(monthSchedules.size())
                .paidSchedulesCount(paidSchedules)
                .partialSchedulesCount(partialSchedules)
                .overdueSchedulesCount(overdueSchedules)
                .pendingSchedulesCount(pendingSchedules)
                .upToDateStudentsCount(upToDateStudents)
                .overdueStudentsCount(overdueStudents)
                .currency(currency)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // 7. Situation Financière Individuelle par Élève (Paginée)
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public PageResponse<StudentFinancialStatusResponse> getStudentsFinancialStatus(
            UUID schoolId, UUID academicYearId, UUID classId, String status, String search, Pageable pageable) {

        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        String currency = getSchoolCurrency(schoolId);
        LocalDate today = LocalDate.now();

        List<StudentEnrollment> enrollments;
        if (yearOpt.isPresent() && classId != null) {
            enrollments = studentEnrollmentRepository.findBySchool_IdAndAcademicYear_IdAndSchoolClass_Id(schoolId, yearOpt.get().getId(), classId);
        } else if (yearOpt.isPresent()) {
            enrollments = studentEnrollmentRepository.findBySchool_IdAndAcademicYear_Id(schoolId, yearOpt.get().getId());
        } else {
            enrollments = studentEnrollmentRepository.findBySchool_Id(schoolId);
        }

        List<PaymentSchedule> allSchedules = yearOpt.isPresent()
                ? paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, yearOpt.get().getId())
                : paymentScheduleRepository.findBySchool_IdAndDeletedFalse(schoolId);

        Map<UUID, List<PaymentSchedule>> schedulesByStudent = allSchedules.stream()
                .filter(ps -> ps.getStudent() != null && ps.getStatus() != PaymentScheduleStatus.CANCELLED)
                .collect(Collectors.groupingBy(ps -> ps.getStudent().getId()));

        List<StudentFinancialStatusResponse> fullList = new ArrayList<>();

        for (StudentEnrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            if (student == null) continue;

            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase();
                boolean matchNumber = student.getStudentNumber() != null && student.getStudentNumber().toLowerCase().contains(q);
                boolean matchFirst = student.getFirstName() != null && student.getFirstName().toLowerCase().contains(q);
                boolean matchLast = student.getLastName() != null && student.getLastName().toLowerCase().contains(q);
                if (!matchNumber && !matchFirst && !matchLast) {
                    continue;
                }
            }

            List<PaymentSchedule> studentSchedules = schedulesByStudent.getOrDefault(student.getId(), Collections.emptyList());

            BigDecimal expected = BigDecimal.ZERO;
            BigDecimal paid = BigDecimal.ZERO;
            BigDecimal remaining = BigDecimal.ZERO;
            BigDecimal overdue = BigDecimal.ZERO;
            boolean hasOverdue = false;

            for (PaymentSchedule ps : studentSchedules) {
                expected = expected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                paid = paid.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                remaining = remaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

                if (ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                        (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today) && MoneyUtils.isPositive(ps.getRemainingAmount()))) {
                    overdue = overdue.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
                    hasOverdue = true;
                }
            }

            String financialStatus;
            if (hasOverdue) {
                financialStatus = "OVERDUE";
            } else if (MoneyUtils.isZero(remaining) && MoneyUtils.isPositive(expected)) {
                financialStatus = "UP_TO_DATE";
            } else if (MoneyUtils.isPositive(paid) && MoneyUtils.isPositive(remaining)) {
                financialStatus = "PARTIALLY_PAID";
            } else if (MoneyUtils.isZero(paid) && MoneyUtils.isPositive(expected)) {
                financialStatus = "UNPAID";
            } else {
                financialStatus = "UP_TO_DATE";
            }

            if (status != null && !status.isBlank() && !financialStatus.equalsIgnoreCase(status)) {
                continue;
            }

            Optional<Payment> lastPayment = paymentRepository.findFirstBySchool_IdAndStudent_IdAndStatusAndDeletedFalseOrderByPaymentDateDesc(
                    schoolId, student.getId(), PaymentStatus.SUCCESS);

            LocalDate lastPaymentDate = null;
            BigDecimal lastPaymentAmount = null;
            if (lastPayment.isPresent()) {
                Payment p = lastPayment.get();
                if (p.getPaymentDate() != null) {
                    lastPaymentDate = p.getPaymentDate().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                lastPaymentAmount = MoneyUtils.scale(p.getAmount());
            }

            fullList.add(StudentFinancialStatusResponse.builder()
                    .studentId(student.getId())
                    .studentNumber(student.getStudentNumber())
                    .studentName(student.getFullName())
                    .classId(enrollment.getSchoolClass() != null ? enrollment.getSchoolClass().getId() : null)
                    .className(enrollment.getSchoolClass() != null ? enrollment.getSchoolClass().getName() : "N/A")
                    .totalExpected(MoneyUtils.scale(expected))
                    .totalPaid(MoneyUtils.scale(paid))
                    .remainingAmount(MoneyUtils.scale(remaining))
                    .overdueAmount(MoneyUtils.scale(overdue))
                    .status(financialStatus)
                    .lastPaymentDate(lastPaymentDate)
                    .lastPaymentAmount(lastPaymentAmount)
                    .currency(currency)
                    .build());
        }

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = pageNumber * pageSize;
        List<StudentFinancialStatusResponse> pagedContent;

        if (fromIndex >= fullList.size()) {
            pagedContent = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + pageSize, fullList.size());
            pagedContent = fullList.subList(fromIndex, toIndex);
        }

        Page<StudentFinancialStatusResponse> page = new PageImpl<>(pagedContent, pageable, fullList.size());
        return PageResponse.of(page);
    }

    // ---------------------------------------------------------------------------------------------
    // 8. Répartition par Catégorie de Frais
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<FeeCategoryBreakdownResponse> getFeeCategoryBreakdown(UUID schoolId, UUID academicYearId) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        String currency = getSchoolCurrency(schoolId);
        LocalDate today = LocalDate.now();

        List<Fee> fees = yearOpt.isPresent()
                ? feeRepository.findBySchool_IdAndAcademicYear_Id(schoolId, yearOpt.get().getId())
                : feeRepository.findBySchool_Id(schoolId);

        List<PaymentSchedule> schedules = yearOpt.isPresent()
                ? paymentScheduleRepository.findBySchool_IdAndEnrollment_AcademicYear_IdAndDeletedFalse(schoolId, yearOpt.get().getId())
                : paymentScheduleRepository.findBySchool_IdAndDeletedFalse(schoolId);

        Map<UUID, List<PaymentSchedule>> schedulesByFee = schedules.stream()
                .filter(ps -> ps.getFee() != null && ps.getStatus() != PaymentScheduleStatus.CANCELLED)
                .collect(Collectors.groupingBy(ps -> ps.getFee().getId()));

        BigDecimal totalSchoolCollected = BigDecimal.ZERO;
        for (PaymentSchedule ps : schedules) {
            if (ps.getStatus() != PaymentScheduleStatus.CANCELLED && ps.getAmountPaid() != null) {
                totalSchoolCollected = totalSchoolCollected.add(ps.getAmountPaid());
            }
        }

        List<FeeCategoryBreakdownResponse> result = new ArrayList<>();

        for (Fee fee : fees) {
            List<PaymentSchedule> feeSchedules = schedulesByFee.getOrDefault(fee.getId(), Collections.emptyList());

            BigDecimal expected = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            BigDecimal remaining = BigDecimal.ZERO;
            BigDecimal overdue = BigDecimal.ZERO;
            long paidCount = 0;

            for (PaymentSchedule ps : feeSchedules) {
                expected = expected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                collected = collected.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                remaining = remaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

                if (ps.getStatus() == PaymentScheduleStatus.PAID || MoneyUtils.isZero(ps.getRemainingAmount())) {
                    paidCount++;
                }

                if (ps.getStatus() == PaymentScheduleStatus.OVERDUE ||
                        (ps.getStatus() != PaymentScheduleStatus.PAID && ps.getDueDate() != null && ps.getDueDate().isBefore(today) && MoneyUtils.isPositive(ps.getRemainingAmount()))) {
                    overdue = overdue.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
                }
            }

            BigDecimal pctOfTotal = BigDecimal.ZERO;
            if (MoneyUtils.isPositive(totalSchoolCollected)) {
                pctOfTotal = collected.multiply(BigDecimal.valueOf(100)).divide(totalSchoolCollected, 2, RoundingMode.HALF_UP);
            }

            BigDecimal collectionRate = BigDecimal.ZERO;
            if (MoneyUtils.isPositive(expected)) {
                collectionRate = collected.multiply(BigDecimal.valueOf(100)).divide(expected, 2, RoundingMode.HALF_UP);
            }

            result.add(FeeCategoryBreakdownResponse.builder()
                    .feeId(fee.getId())
                    .feeName(fee.getName())
                    .feeCode(fee.getCode())
                    .feeType(fee.getFeeType())
                    .expectedAmount(MoneyUtils.scale(expected))
                    .collectedAmount(MoneyUtils.scale(collected))
                    .remainingAmount(MoneyUtils.scale(remaining))
                    .overdueAmount(MoneyUtils.scale(overdue))
                    .percentageOfTotalCollected(pctOfTotal)
                    .collectionRate(collectionRate)
                    .totalSchedulesCount(feeSchedules.size())
                    .paidSchedulesCount(paidCount)
                    .currency(currency)
                    .build());
        }

        return result;
    }

    // ---------------------------------------------------------------------------------------------
    // 9. Répartition par Moyen de Paiement
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<PaymentMethodBreakdownResponse> getPaymentMethodBreakdown(
            UUID schoolId, UUID academicYearId, LocalDate startDate, LocalDate endDate) {

        tenantValidationService.validateSchoolAccess(schoolId);
        String currency = getSchoolCurrency(schoolId);

        List<Object[]> rawAggregates;
        if (startDate != null && endDate != null) {
            Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endInstant = endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
            rawAggregates = paymentRepository.sumAmountByPaymentMethodBetween(schoolId, startInstant, endInstant);
        } else {
            rawAggregates = paymentRepository.sumAmountByPaymentMethod(schoolId);
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        Map<PaymentMethod, Object[]> statsMap = new HashMap<>();

        for (Object[] row : rawAggregates) {
            PaymentMethod method = (PaymentMethod) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal amount = MoneyUtils.scale((BigDecimal) row[2]);
            statsMap.put(method, new Object[]{count, amount});
            grandTotal = grandTotal.add(amount);
        }

        List<PaymentMethodBreakdownResponse> list = new ArrayList<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            Object[] row = statsMap.get(method);
            long count = row != null ? (long) row[0] : 0L;
            BigDecimal amount = row != null ? (BigDecimal) row[1] : BigDecimal.ZERO;

            BigDecimal pct = BigDecimal.ZERO;
            if (MoneyUtils.isPositive(grandTotal)) {
                pct = amount.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, RoundingMode.HALF_UP);
            }

            String label = switch (method) {
                case ORANGE_MONEY -> "Orange Money";
                case WAVE -> "Wave";
                case MOBILE_MONEY -> "Mobile Money";
                case CASH -> "Espèces";
                case BANK_TRANSFER -> "Virement bancaire";
                case CHEQUE -> "Chèque";
                case CREDIT_CARD -> "Carte bancaire";
                case OTHER -> "Autre";
            };

            list.add(PaymentMethodBreakdownResponse.builder()
                    .paymentMethod(method)
                    .paymentMethodLabel(label)
                    .totalAmount(MoneyUtils.scale(amount))
                    .transactionCount(count)
                    .percentage(pct)
                    .currency(currency)
                    .build());
        }

        list.sort(Comparator.comparing(PaymentMethodBreakdownResponse::getTotalAmount).reversed());
        return list;
    }

    // ---------------------------------------------------------------------------------------------
    // 10. Évolution Chronologique des Encaissements
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<PaymentEvolutionResponse> getPaymentEvolution(
            UUID schoolId, UUID academicYearId, String groupBy, LocalDate dateFrom, LocalDate dateTo) {

        tenantValidationService.validateSchoolAccess(schoolId);
        LocalDate today = LocalDate.now();
        String groupMode = groupBy != null ? groupBy.toUpperCase() : "MONTH";

        LocalDate start = dateFrom != null
                ? dateFrom
                : ("DAY".equals(groupMode) ? today.minusDays(30) : ("WEEK".equals(groupMode) ? today.minusWeeks(12) : today.minusMonths(6).withDayOfMonth(1)));
        LocalDate end = dateTo != null ? dateTo : today;

        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        Instant startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = end.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        List<Payment> payments = paymentRepository.findSuccessfulPaymentsBetween(schoolId, startInstant, endInstant);

        Map<String, BigDecimal> collectedByPeriod = new LinkedHashMap<>();
        Map<String, Long> countByPeriod = new LinkedHashMap<>();

        // Pre-fill keys in chronological order
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = formatPeriodKey(cursor, groupMode);
            collectedByPeriod.putIfAbsent(key, BigDecimal.ZERO);
            countByPeriod.putIfAbsent(key, 0L);

            if ("DAY".equals(groupMode)) {
                cursor = cursor.plusDays(1);
            } else if ("WEEK".equals(groupMode)) {
                cursor = cursor.plusWeeks(1);
            } else {
                cursor = cursor.plusMonths(1);
            }
        }

        for (Payment p : payments) {
            if (p.getPaymentDate() == null) continue;
            LocalDate pDate = p.getPaymentDate().atZone(ZoneId.systemDefault()).toLocalDate();
            String key = formatPeriodKey(pDate, groupMode);

            if (collectedByPeriod.containsKey(key)) {
                collectedByPeriod.put(key, collectedByPeriod.get(key).add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO));
                countByPeriod.put(key, countByPeriod.get(key) + 1);
            }
        }

        List<PaymentEvolutionResponse> result = new ArrayList<>();
        collectedByPeriod.forEach((period, collected) -> {
            result.add(PaymentEvolutionResponse.builder()
                    .period(period)
                    .collectedAmount(MoneyUtils.scale(collected))
                    .expectedAmount(MoneyUtils.scale(collected)) // Can represent base collected or expected baseline
                    .transactionCount(countByPeriod.getOrDefault(period, 0L))
                    .build());
        });

        return result;
    }

    private String formatPeriodKey(LocalDate date, String groupMode) {
        if ("DAY".equals(groupMode)) {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else if ("WEEK".equals(groupMode)) {
            WeekFields weekFields = WeekFields.of(Locale.FRANCE);
            int week = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            return String.format("%d-W%02d", year, week);
        } else {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 11. Impayés & Retards avec Ancienneté et Contact Parent
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public PageResponse<OverdueStudentPaymentResponse> getOverdueStudents(
            UUID schoolId, UUID academicYearId, UUID classId, Pageable pageable) {

        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        String currency = getSchoolCurrency(schoolId);
        LocalDate today = LocalDate.now();

        List<PaymentSchedule> overdueSchedules;
        if (yearOpt.isPresent() && classId != null) {
            overdueSchedules = paymentScheduleRepository.findOverdueSchedulesByAcademicYearAndClass(schoolId, yearOpt.get().getId(), classId, today);
        } else if (yearOpt.isPresent()) {
            overdueSchedules = paymentScheduleRepository.findOverdueSchedulesByAcademicYear(schoolId, yearOpt.get().getId(), today);
        } else {
            overdueSchedules = paymentScheduleRepository.findOverdueSchedules(schoolId, today);
        }

        Map<UUID, List<PaymentSchedule>> studentOverdueMap = overdueSchedules.stream()
                .filter(ps -> ps.getStudent() != null)
                .collect(Collectors.groupingBy(ps -> ps.getStudent().getId()));

        List<OverdueStudentPaymentResponse> fullList = new ArrayList<>();

        for (Map.Entry<UUID, List<PaymentSchedule>> entry : studentOverdueMap.entrySet()) {
            List<PaymentSchedule> schedules = entry.getValue();
            if (schedules.isEmpty()) continue;

            PaymentSchedule first = schedules.get(0);
            Student student = first.getStudent();

            BigDecimal overdueAmount = BigDecimal.ZERO;
            LocalDate oldestDueDate = null;

            for (PaymentSchedule ps : schedules) {
                overdueAmount = overdueAmount.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);
                if (oldestDueDate == null || (ps.getDueDate() != null && ps.getDueDate().isBefore(oldestDueDate))) {
                    oldestDueDate = ps.getDueDate();
                }
            }

            long daysOverdue = oldestDueDate != null ? ChronoUnit.DAYS.between(oldestDueDate, today) : 0;
            String bucket = daysOverdue <= 7 ? "0-7j" : (daysOverdue <= 30 ? "8-30j" : "30j+");

            SchoolClass schoolClass = (first.getEnrollment() != null) ? first.getEnrollment().getSchoolClass() : null;

            // Financial Parent Contact
            Optional<StudentParent> parentLink = studentParentRepository.findFinancialContactByStudentId(student.getId());
            if (parentLink.isEmpty()) {
                List<StudentParent> links = studentParentRepository.findByStudent_Id(student.getId());
                if (!links.isEmpty()) {
                    parentLink = Optional.of(links.get(0));
                }
            }

            String parentName = "N/A";
            String parentPhone = "N/A";
            if (parentLink.isPresent() && parentLink.get().getParent() != null) {
                Parent p = parentLink.get().getParent();
                parentName = p.getFullName();
                parentPhone = p.getPhone() != null ? p.getPhone() : "N/A";
            }

            fullList.add(OverdueStudentPaymentResponse.builder()
                    .studentId(student.getId())
                    .studentNumber(student.getStudentNumber())
                    .studentName(student.getFullName())
                    .classId(schoolClass != null ? schoolClass.getId() : null)
                    .className(schoolClass != null ? schoolClass.getName() : "N/A")
                    .overdueAmount(MoneyUtils.scale(overdueAmount))
                    .overdueSchedulesCount(schedules.size())
                    .oldestDueDate(oldestDueDate)
                    .daysOverdue(daysOverdue)
                    .arrearsBucket(bucket)
                    .parentName(parentName)
                    .parentPhone(parentPhone)
                    .currency(currency)
                    .build());
        }

        fullList.sort(Comparator.comparing(OverdueStudentPaymentResponse::getDaysOverdue).reversed());

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = pageNumber * pageSize;
        List<OverdueStudentPaymentResponse> pagedContent;

        if (fromIndex >= fullList.size()) {
            pagedContent = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + pageSize, fullList.size());
            pagedContent = fullList.subList(fromIndex, toIndex);
        }

        Page<OverdueStudentPaymentResponse> page = new PageImpl<>(pagedContent, pageable, fullList.size());
        return PageResponse.of(page);
    }

    // ---------------------------------------------------------------------------------------------
    // 12. Échéances Prévisionnelles à Venir
    // ---------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<UpcomingDueDateResponse> getUpcomingDueDates(UUID schoolId, UUID academicYearId, int daysAhead) {
        tenantValidationService.validateSchoolAccess(schoolId);
        Optional<AcademicYear> yearOpt = resolveAcademicYear(schoolId, academicYearId);
        String currency = getSchoolCurrency(schoolId);

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead > 0 ? daysAhead : 30);

        List<PaymentSchedule> schedules;
        if (yearOpt.isPresent()) {
            schedules = paymentScheduleRepository.findUpcomingSchedulesByAcademicYear(schoolId, yearOpt.get().getId(), today, endDate);
        } else {
            schedules = paymentScheduleRepository.findUpcomingSchedules(schoolId, today, endDate);
        }

        // Group by (dueDate, fee.id)
        Map<String, List<PaymentSchedule>> groupedSchedules = new LinkedHashMap<>();
        for (PaymentSchedule ps : schedules) {
            if (ps.getDueDate() == null || ps.getFee() == null) continue;
            String groupKey = ps.getDueDate().toString() + "_" + ps.getFee().getId().toString();
            groupedSchedules.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(ps);
        }

        List<UpcomingDueDateResponse> result = new ArrayList<>();

        for (List<PaymentSchedule> group : groupedSchedules.values()) {
            if (group.isEmpty()) continue;
            PaymentSchedule sample = group.get(0);
            Fee fee = sample.getFee();
            LocalDate dueDate = sample.getDueDate();

            BigDecimal expected = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            BigDecimal remaining = BigDecimal.ZERO;
            long paidCount = 0;
            long pendingCount = 0;

            for (PaymentSchedule ps : group) {
                expected = expected.add(ps.getAmountDue() != null ? ps.getAmountDue() : BigDecimal.ZERO);
                collected = collected.add(ps.getAmountPaid() != null ? ps.getAmountPaid() : BigDecimal.ZERO);
                remaining = remaining.add(ps.getRemainingAmount() != null ? ps.getRemainingAmount() : BigDecimal.ZERO);

                if (ps.getStatus() == PaymentScheduleStatus.PAID || MoneyUtils.isZero(ps.getRemainingAmount())) {
                    paidCount++;
                } else {
                    pendingCount++;
                }
            }

            long daysRemaining = ChronoUnit.DAYS.between(today, dueDate);

            result.add(UpcomingDueDateResponse.builder()
                    .dueDate(dueDate)
                    .feeName(fee != null ? fee.getName() : "N/A")
                    .feeType(fee != null ? fee.getFeeType() : FeeType.OTHER)
                    .totalAmountExpected(MoneyUtils.scale(expected))
                    .totalAmountCollected(MoneyUtils.scale(collected))
                    .remainingAmount(MoneyUtils.scale(remaining))
                    .totalStudentsCount(group.size())
                    .paidStudentsCount(paidCount)
                    .pendingStudentsCount(pendingCount)
                    .isToday(dueDate.isEqual(today))
                    .daysRemaining(daysRemaining)
                    .currency(currency)
                    .build());
        }

        result.sort(Comparator.comparing(UpcomingDueDateResponse::getDueDate));
        return result;
    }
}

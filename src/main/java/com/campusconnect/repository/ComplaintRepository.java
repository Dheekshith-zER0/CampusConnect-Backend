package com.campusconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.entity.Complaint;
import com.campusconnect.entity.ComplaintCategory;
import com.campusconnect.entity.ComplaintStatus;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // Newest first (ids are auto-incremented)
    List<Complaint> findByStudentIdOrderByIdDesc(Long studentId);

    long countByStatus(ComplaintStatus status);

    /**
     * Admin dashboard search. Every filter is optional - pass null to ignore it.
     * The search text is matched against title, location, student name and student email.
     */
    @Query("""
            SELECT c FROM Complaint c JOIN FETCH c.student s
            WHERE (:status IS NULL OR c.status = :status)
              AND (:category IS NULL OR c.category = :category)
              AND (:search IS NULL
                   OR LOWER(c.title) LIKE CONCAT('%', :search, '%')
                   OR LOWER(c.location) LIKE CONCAT('%', :search, '%')
                   OR LOWER(s.name) LIKE CONCAT('%', :search, '%')
                   OR LOWER(s.email) LIKE CONCAT('%', :search, '%'))
            ORDER BY c.id DESC
            """)
    List<Complaint> search(@Param("status") ComplaintStatus status,
                           @Param("category") ComplaintCategory category,
                           @Param("search") String search);
}

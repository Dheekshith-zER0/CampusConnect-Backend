package com.campusconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusconnect.entity.StatusUpdate;

public interface StatusUpdateRepository extends JpaRepository<StatusUpdate, Long> {

    List<StatusUpdate> findByComplaintIdOrderByIdAsc(Long complaintId);

    void deleteByComplaintId(Long complaintId);
}

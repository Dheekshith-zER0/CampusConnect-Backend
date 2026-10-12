package com.campusconnect.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.dto.ComplaintDetailResponse;
import com.campusconnect.dto.ComplaintRequest;
import com.campusconnect.dto.ComplaintResponse;
import com.campusconnect.dto.DashboardStats;
import com.campusconnect.dto.StatusUpdateRequest;
import com.campusconnect.dto.StatusUpdateResponse;
import com.campusconnect.entity.Complaint;
import com.campusconnect.entity.ComplaintCategory;
import com.campusconnect.entity.ComplaintStatus;
import com.campusconnect.entity.Role;
import com.campusconnect.entity.StatusUpdate;
import com.campusconnect.entity.User;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.ComplaintRepository;
import com.campusconnect.repository.StatusUpdateRepository;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepo;

    @Autowired
    private StatusUpdateRepository statusUpdateRepo;

    // ---------- Student operations ----------

    @Transactional
    public ComplaintResponse createComplaint(ComplaintRequest request, User student) {
        if (student.getRole() != Role.STUDENT) {
            throw ApiException.forbidden("Only students can submit complaints");
        }

        Complaint complaint = new Complaint();
        applyRequest(complaint, request);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setStudent(student);
        complaintRepo.save(complaint);

        statusUpdateRepo.save(new StatusUpdate(complaint, ComplaintStatus.SUBMITTED,
                "Complaint submitted", student.getName()));
        return ComplaintResponse.from(complaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMyComplaints(User student) {
        return complaintRepo.findByStudentIdOrderByIdDesc(student.getId()).stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintDetails(Long id, User user) {
        Complaint complaint = findComplaint(id);
        checkCanView(complaint, user);

        List<StatusUpdateResponse> history = statusUpdateRepo.findByComplaintIdOrderByIdAsc(id).stream()
                .map(StatusUpdateResponse::from)
                .toList();
        return new ComplaintDetailResponse(ComplaintResponse.from(complaint), history);
    }

    /** Students may edit their own complaint until an administrator starts working on it. */
    @Transactional
    public ComplaintResponse updateComplaint(Long id, ComplaintRequest request, User user) {
        Complaint complaint = findComplaint(id);
        if (!isOwner(complaint, user)) {
            throw ApiException.forbidden("You can only edit your own complaints");
        }
        if (complaint.getStatus() != ComplaintStatus.SUBMITTED) {
            throw ApiException.conflict("This complaint is already " + complaint.getStatus().getLabel()
                    + " and can no longer be edited");
        }

        applyRequest(complaint, request);
        return ComplaintResponse.from(complaintRepo.saveAndFlush(complaint));
    }

    /** Students may withdraw their own unprocessed complaint; administrators may delete any complaint. */
    @Transactional
    public void deleteComplaint(Long id, User user) {
        Complaint complaint = findComplaint(id);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAdmin) {
            if (!isOwner(complaint, user)) {
                throw ApiException.forbidden("You can only delete your own complaints");
            }
            if (complaint.getStatus() != ComplaintStatus.SUBMITTED) {
                throw ApiException.conflict("Only complaints that are still Submitted can be withdrawn");
            }
        }

        statusUpdateRepo.deleteByComplaintId(id);
        complaintRepo.delete(complaint);
    }

    // ---------- Administrator operations ----------

    @Transactional(readOnly = true)
    public List<ComplaintResponse> searchComplaints(ComplaintStatus status, ComplaintCategory category, String search) {
        String text = (search == null || search.isBlank()) ? null : search.trim().toLowerCase(Locale.ROOT);
        return complaintRepo.search(status, category, text).stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    @Transactional
    public ComplaintResponse updateStatus(Long id, StatusUpdateRequest request, User admin) {
        Complaint complaint = findComplaint(id);
        String remarks = request.remarks() == null ? null : request.remarks().trim();
        boolean hasRemarks = remarks != null && !remarks.isEmpty();

        if (request.status() == ComplaintStatus.REJECTED && !hasRemarks) {
            throw ApiException.badRequest("Please give a reason when rejecting a complaint");
        }

        complaint.setStatus(request.status());
        if (hasRemarks) {
            complaint.setAdminRemarks(remarks);
        }
        complaintRepo.saveAndFlush(complaint);

        statusUpdateRepo.save(new StatusUpdate(complaint, request.status(),
                hasRemarks ? remarks : null, admin.getName()));
        return ComplaintResponse.from(complaint);
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        return new DashboardStats(
                complaintRepo.count(),
                complaintRepo.countByStatus(ComplaintStatus.SUBMITTED),
                complaintRepo.countByStatus(ComplaintStatus.IN_PROGRESS),
                complaintRepo.countByStatus(ComplaintStatus.RESOLVED),
                complaintRepo.countByStatus(ComplaintStatus.REJECTED));
    }

    // ---------- Helpers ----------

    private Complaint findComplaint(Long id) {
        return complaintRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Complaint " + id + " was not found"));
    }

    private void checkCanView(Complaint complaint, User user) {
        if (user.getRole() != Role.ADMIN && !isOwner(complaint, user)) {
            throw ApiException.forbidden("You do not have access to this complaint");
        }
    }

    private boolean isOwner(Complaint complaint, User user) {
        return complaint.getStudent().getId().equals(user.getId());
    }

    private void applyRequest(Complaint complaint, ComplaintRequest request) {
        complaint.setTitle(request.title().trim());
        complaint.setDescription(request.description().trim());
        complaint.setCategory(request.category());
        String location = request.location() == null ? "" : request.location().trim();
        complaint.setLocation(location.isEmpty() ? null : location);
    }
}

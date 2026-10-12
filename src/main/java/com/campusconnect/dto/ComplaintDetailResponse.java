package com.campusconnect.dto;

import java.util.List;

public record ComplaintDetailResponse(ComplaintResponse complaint, List<StatusUpdateResponse> history) {
}

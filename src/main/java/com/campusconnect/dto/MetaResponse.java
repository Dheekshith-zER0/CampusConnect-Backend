package com.campusconnect.dto;

import java.util.List;

public record MetaResponse(List<OptionResponse> categories, List<OptionResponse> statuses) {
}

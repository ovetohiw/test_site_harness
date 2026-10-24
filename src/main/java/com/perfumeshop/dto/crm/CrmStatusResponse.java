package com.perfumeshop.dto.crm;

import java.util.List;

public record CrmStatusResponse(
        boolean enabled,
        List<ProviderStatus> providers
) {
    public record ProviderStatus(
            String name,
            boolean enabled,
            String mode,
            long successCount
    ) {
    }
}

package novabank.branch.dto.request;

import jakarta.validation.constraints.NotNull;
import novabank.branch.entities.BranchStatus;

public record BranchStatusUpdateRequest(

        @NotNull
        BranchStatus status,

        String reason
) {
}

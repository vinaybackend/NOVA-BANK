package novabank.branch.dto.response;

import novabank.branch.dto.request.BranchCreateRequest;
import novabank.branch.entities.Branch;
import novabank.branch.entities.BranchStatus;

public final class BranchMapper {

    private BranchMapper() {
    }

    public static Branch toEntity(BranchCreateRequest request) {
        Branch branch = new Branch();
        branch.setBranchName(request.branchName().trim());
        branch.setIfscCode(request.ifscCode().trim().toUpperCase());
        branch.setBranchType(request.branchType());

        branch.setAddress(request.address().trim());
        branch.setCity(request.city().trim());
        branch.setDistrict(request.district().trim());
        branch.setState(request.state().trim());
        branch.setPinCode(request.pinCode().trim());
        branch.setCountry(request.country().trim());

        branch.setPhone(request.phone());
        branch.setEmail(request.email());
        branch.setOpeningDate(request.openingDate());
        branch.setOpeningTime(request.openingTime());
        branch.setClosingTime(request.closingTime());

        if (request.timeZone() != null && !request.timeZone().isBlank()) {
            branch.setTimeZone(request.timeZone().trim());
        }

        branch.setStatus(BranchStatus.ACTIVE);

        return branch;
    }

    public static BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getBranchCode(),
                branch.getBranchName(),
                branch.getIfscCode(),
                branch.getBranchType(),
                branch.getAddress(),
                branch.getCity(),
                branch.getDistrict(),
                branch.getState(),
                branch.getPinCode(),
                branch.getCountry(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getStatus(),
                branch.getOpeningDate(),
                branch.getOpeningTime(),
                branch.getClosingTime(),
                branch.getTimeZone(),
                branch.getCreatedAt(),
                branch.getUpdatedAt(),
                branch.getVersion()
        );
    }
}

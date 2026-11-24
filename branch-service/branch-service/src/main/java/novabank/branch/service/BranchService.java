package novabank.branch.service;
import novabank.branch.dto.request.BranchCreateRequest;
import novabank.branch.dto.request.BranchStatusUpdateRequest;
import novabank.branch.dto.request.BranchUpdateRequest;
import novabank.branch.dto.response.BranchMapper;
import novabank.branch.dto.response.BranchResponse;
import novabank.branch.entities.Branch;
import novabank.branch.entities.BranchStatus;
import novabank.branch.entities.BranchStatusHistory;
import novabank.branch.entities.BranchType;
import novabank.branch.exception.BranchNotFoundException;
import novabank.branch.exception.DuplicateBranchException;
import novabank.branch.repository.BranchRepository;
import novabank.branch.repository.BranchStatusHistoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchStatusHistoryRepository historyRepository;

    public BranchService(BranchRepository branchRepository, BranchStatusHistoryRepository branchStatusHistoryRepository) {
        this.branchRepository = branchRepository;
        this.historyRepository = branchStatusHistoryRepository;
    }


    @Transactional
    public BranchResponse createBranch(BranchCreateRequest request) {

        String ifscCode = request.ifscCode().trim();

        if (branchRepository.existsByIfscCodeIgnoreCase(ifscCode)) {
            throw new DuplicateBranchException(
                    "IFSC code already exists: " + ifscCode
            );
        }

        validateOperatingHours(request);
        validateOpeningDate(request);
        validateTimeZone(request);

        Branch branch = BranchMapper.toEntity(request);
        String branchCode = generateBranchCode();
        branch.setBranchCode(branchCode);


        try {
            // First save: obtain the database-generated ID.
            Branch savedBranch = branchRepository.saveAndFlush(branch);


            return BranchMapper.toResponse(savedBranch);

        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateBranchException(
                    "Branch code or IFSC code already exists."
            );
        }
    }


    private void validateOperatingHours(BranchCreateRequest request) {

        if ((request.openingTime() == null)
                != (request.closingTime() == null)) {
            throw new IllegalArgumentException("Opening time and closing time must both be provided.");
        }

        if (request.openingTime() != null
                && !request.closingTime().isAfter(request.openingTime())) {
            throw new IllegalArgumentException("Closing time must be after opening time.");
        }
    }

    private void validateOpeningDate(BranchCreateRequest request) {

        if (request.openingDate() != null
                && request.openingDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Opening date cannot be in the future.");
        }
    }

    private void validateTimeZone(BranchCreateRequest request) {

        if (request.timeZone() != null && !request.timeZone().isBlank()) {
            try {
                ZoneId.of(request.timeZone().trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid time zone: " + request.timeZone());
            }
        }
    }


    @Transactional(readOnly = true)
    public BranchResponse getBranchCode(String branchCode) {

        Branch branch = branchRepository.findByBranchCode(branchCode).orElseThrow(() -> new RuntimeException("Branch not found: " + branchCode));

        return BranchMapper.toResponse(branch);

    }


    @Transactional(readOnly = true)
    public Page<BranchResponse> getBranches(String city, BranchStatus status, int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "branchName"));

        Page<Branch> branches;

        if (city != null && !city.isBlank() && status != null) {
            branches = branchRepository.findByCityContainingIgnoreCaseAndStatus(city.trim(), status, pageable);
        } else if (city != null && !city.isBlank()) {
            branches = branchRepository.findByCityContainingIgnoreCase(city.trim(), pageable);
        } else if (status != null) {
            branches = branchRepository.findByStatus(status, pageable);
        } else {
            branches = branchRepository.findAll(pageable);
        }

        return branches.map(BranchMapper::toResponse);
    }

    @Transactional
    public BranchResponse updateBranch(Long id, BranchUpdateRequest request) {
        Branch branch = branchRepository.findById(id).orElseThrow(() -> new BranchNotFoundException(id));

        branch.setBranchName(request.branchName().trim());
        branch.setAddress(request.address().trim());
        branch.setCity(request.city().trim());
        branch.setDistrict(request.district().trim());
        branch.setState(request.state().trim());
        branch.setPinCode(request.pinCode().trim());
        branch.setCountry(request.country().trim());
        branch.setPhone(request.phone());
        branch.setEmail(request.email() == null ? null : request.email().trim());
        branch.setOpeningDate(request.openingDate());
        branch.setOpeningTime(request.openingTime());
        branch.setClosingTime(request.closingTime());

        branch.setTimeZone(request.timeZone() == null || request.timeZone().isBlank() ? null : request.timeZone().trim());

        Branch updatedBranch = branchRepository.save(branch);

        return BranchMapper.toResponse(updatedBranch);
    }

    @Transactional
    public BranchResponse updateBranchStatus(Long id, BranchStatusUpdateRequest request) {
        Branch branch = branchRepository.findById(id).orElseThrow(() -> new BranchNotFoundException(id));

        BranchStatus currentStatus = branch.getStatus();
        BranchStatus newStatus = request.status();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String changedBy = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName()) ? authentication.getName() : "UNKNOWN";

        String reason = request.reason() == null ? "" : request.reason().trim();

        if (reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required for every branch status change.");
        }

        BranchStatusHistory history = new BranchStatusHistory(branch.getBranchCode(), currentStatus, newStatus, reason, changedBy);

        branch.setStatus(newStatus);

        Branch savedBranch = branchRepository.save(branch);
        historyRepository.save(history);

        return BranchMapper.toResponse(savedBranch);

    }

    @Transactional(readOnly = true)
    public List<BranchStatusHistory> getBranchStatusHistory(String branchCode) {

        if (!branchRepository.existsByBranchCodeIgnoreCase(branchCode)) {
            throw new RuntimeException("Branch not found with code: " + branchCode);
        }

        return historyRepository.findByBranchCodeOrderByChangedAtDesc(branchCode);
    }

    @Transactional(readOnly = true)
    public Page<BranchResponse> searchBranches(String branchCode, String ifscCode, String city, BranchStatus status, BranchType branchType, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "branchName"));

        Page<Branch> result;

        if (branchCode != null && !branchCode.isBlank()) {
            result = branchRepository.findByBranchCodeContainingIgnoreCase(branchCode.trim(), pageable);
        } else if (ifscCode != null && !ifscCode.isBlank()) {
            result = branchRepository.findByIfscCodeContainingIgnoreCase(ifscCode.trim(), pageable);
        } else if (city != null && !city.isBlank() && status != null) {
            result = branchRepository.findByCityContainingIgnoreCaseAndStatus(city.trim(), status, pageable);
        } else if (city != null && !city.isBlank()) {
            result = branchRepository.findByCityContainingIgnoreCase(city.trim(), pageable);
        } else if (status != null) {
            result = branchRepository.findByStatus(status, pageable);
        } else if (branchType != null) {
            result = branchRepository.findByBranchType(branchType, pageable);
        } else {
            result = branchRepository.findAll(pageable);
        }

        return result.map(BranchMapper::toResponse);
    }


    // branch code
    public String generateBranchCode() {
        SecureRandom random = new SecureRandom();

        int number = random.nextInt(100); // 00–99
        char letter = (char) ('A' + random.nextInt(26)); // A–Z

        return String.format("NOVA%02d%c", number, letter);
    }

}

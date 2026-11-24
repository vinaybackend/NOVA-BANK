package novabank.employee.exception;

public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(Long branchId) {
        super("Branch not found with id: " + branchId);
    }
}
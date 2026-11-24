package novabank.employee.exception;
public class BranchServiceException extends RuntimeException {

    public BranchServiceException(String message) {
        super(message);
    }

    public BranchServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
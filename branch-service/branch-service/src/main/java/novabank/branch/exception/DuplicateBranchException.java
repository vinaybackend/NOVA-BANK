package novabank.branch.exception;

public class DuplicateBranchException extends RuntimeException {

    public DuplicateBranchException(String message) {
        super(message);
    }
}

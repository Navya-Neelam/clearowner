package ai.clearowner.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String type, String id) {
        super("%s '%s' was not found".formatted(type, id));
    }
}

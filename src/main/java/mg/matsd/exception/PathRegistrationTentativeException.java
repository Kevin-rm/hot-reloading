package mg.matsd.exception;

import mg.matsd.javaframework.core.exceptions.BaseException;

public final class PathRegistrationTentativeException extends BaseException {
    private static final String PREFIX = "Erreur lors d'une tentative d'enregistrement de path";

    public PathRegistrationTentativeException(Throwable cause) {
        super(PREFIX, cause);
    }
}

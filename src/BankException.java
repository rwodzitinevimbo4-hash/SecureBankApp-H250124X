import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compatibility exception class.
 *
 * The banking service exposes its custom exception type as BankService.BankException,
 * but this file must still remain a valid Java source file when compiled in this project.
 */
class BankException extends Exception {
    private static final long serialVersionUID = 1L;

    BankException(String message) {
        super(message);
    }
}


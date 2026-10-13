import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PasswordHasher
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR? 
 * We must NEVER save a real password in a file. If someone stole the file
 * they would see everybody's passwords. Instead we save a "hash": a
 * scrambled fingerprint of the password that cannot be turned back into
 * the original.
 *             
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 600_000; // how many times we repeat the scrambling
    private static final int KEY_BITS = 256;       // length of the resulting fingerprint
    private static final int SALT_BYTES = 16;      // length of the random salt

    // SecureRandom = a random number generator that is safe for security uses.
    // (The normal "Random" is predictable and must NOT be used for this.)
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** Creates a brand-new random salt, returned as text (Base64) so it can be saved in a file. */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt); // fill the array with random bytes
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Scrambles a password with a salt and returns the fingerprint as text.
     * The password is a char[] (not a String) so we can wipe it from memory afterwards.
     */
    public static String hash(char[] password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] result = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(result);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // This should never happen on a normal Java install.
            throw new IllegalStateException("Password hashing is not available on this system.", e);
        } finally {
            spec.clearPassword(); // wipe the copy of the password held inside 'spec'
        }
    }

    /**
     * Checks a typed password against the saved salt + fingerprint.
     *
     * SECURE CODING IDEA: we compare with MessageDigest.isEqual, which takes
     * the same amount of time whether the first character or the last one
     * differs. A normal "equals" can finish sooner on a mismatch, which can
     * leak tiny clues to an attacker who measures the timing.
     */
    public static boolean matches(char[] typedPassword, String saltBase64, String expectedHashBase64) {
        String actual = hash(typedPassword, saltBase64);
        return MessageDigest.isEqual(actual.getBytes(), expectedHashBase64.getBytes());
    }

    /** Overwrites a password array with blanks so it does not linger in memory. */
    public static void wipe(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;



/**
 * Several static methods useful for handling String objects. Should be
 * replaced by a third-party library, such as Apache Commons.
 * 
 * 
 */
public class StringUtilities {
        /**
         * Returns current date and time using a long format.
         *
         * @return current date and time
         */
        public static String getDate() {
                String format = "hh:mm a 'on' EEE, MMM dd, yyyy";
                return getDate(format);
        }

        /**
         * Returns current date and time using provided format.
         *
         * @return current date and time
         */
        public static String getDate(String format) {
                DateFormat dateFormat = new SimpleDateFormat(format);
                return dateFormat.format(Calendar.getInstance().getTime());
        }

        /**
         * Checks to see if a String is null or empty.
         * @param text - String to check
         * @return true if null or empty
         */
        public static boolean checkString(String text) {
                return (text == null) || text.trim().isEmpty();
        }

        /**
         * Returns a random byte array. Note: Does not use a secure random number
         * generator.
         *
         * @param size - number of bytes to return
         * @return random byte array
         *
         * @see {@link SecureRandom}
         */
        public static byte[] randomBytes(int size) {
                byte[] saltBytes = new byte[size];
                new Random().nextBytes(saltBytes);
                return saltBytes;
        }

        /**
         * Returns the hex encoding of a byte array.
         *
         * @param bytes - byte array to encode
         * @param length - desired length of encoding
         * @return hex encoded byte array
         */
        public static String encodeHex(byte[] bytes, int length) {
                BigInteger bigint = new BigInteger(1, bytes);
                String hex = String.format("%0" + length + "X", bigint);

                assert hex.length() == length;
                return hex;
        }

        /**
         * Calculates the hash of a password and salt using SHA-256.
         *
         * @param password - password to hash
         * @param salt - salt associated with user
         * @return hashed password, or null if unable to hash
         */
        public static String getHash(String password, String salt) {
                String salted = salt + password;
                String hashed = salted;

                try {
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        md.update(salted.getBytes());
                        hashed = encodeHex(md.digest(), 64);
                }
                catch (Exception ex) {
                        hashed = null;
                }

                return hashed;
        }

        /**
         * Returns the {@link Status} object for the provided name, if valid.
         * Otherwise, returns {@link Status#ERROR}.
         *
         * @param name
         * @return status
         */
        public static Status getStatus(String name) {
                Status status = Status.OK;

                try {
                        status = Status.valueOf(name);
                }
                catch (IllegalArgumentException ex) {
                	
                        status = Status.ERROR;
                }

                return status;
        }

        /**
         * Returns the {@link Status} object for the provided ordinal, if valid.
         * Otherwise, returns {@link Status#ERROR}.
         *
         * @param name
         * @return status
         */
        public static Status getStatus(int ordinal) {
                Status status = Status.OK;

                try {
                        status = Status.values()[ordinal];
                }
                catch (ArrayIndexOutOfBoundsException ex) {
                        status = Status.ERROR;
                }

                return status;
        }

        public static void main(String[] args) {
                System.out.println(getDate());
                System.out.println(getDate("MMM dd, yyyy"));

                byte[] bytes = randomBytes(4);
                String salt = encodeHex(bytes, 8);
                String test = getHash("test", salt);

                System.out.println(Arrays.toString(bytes));
                System.out.println(salt);
                System.out.println(test);

                System.out.println(getStatus("ERROR").name());
                System.out.println(getStatus("HELLO").name());

                System.out.println(getStatus(4).name());
                System.out.println(getStatus(-4).name());
        }
}
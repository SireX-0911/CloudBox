package is.com.cryptobayt.crypt;

/**
 * Created by developer on 05.05.2017.
 */

public class TokenParser {
    public static String getRoomID(String token) {
        String[] parts = token.split("/");
        return parts[0];
    }

    public static String getAESKey(String token) {
        String[] parts = token.split("/");
        return parts[1];
    }

    public static String parseToken(String roomID, String aesKey) {
        String s = roomID + "/" + aesKey;
        return s;
    }
}

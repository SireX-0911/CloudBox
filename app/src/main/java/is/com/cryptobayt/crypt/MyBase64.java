package is.com.cryptobayt.crypt;

import android.util.Base64;

/**
 * Created by developer on 05.05.2017.
 */

public class MyBase64 {
    public static String base64Encode(byte[] key){
        String s = Base64.encodeToString(key, Base64.URL_SAFE);
        return s.substring(0,s.length()-1);


    }

    public static byte[] base64Decode(String FirebaseKey){

        return Base64.decode(FirebaseKey, Base64.URL_SAFE);

    }

}

package com.trackcell.securetalk;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class RC4
{
    public int[] encrypt(String clearText, String key)
    {
        byte[] clearText_;
        byte[] cipherText;
        int[] returnText = new int[clearText.length()];
        try {
            Cipher rc4 = Cipher.getInstance("RC4");
            SecretKeySpec rc4Key = new SecretKeySpec(key.getBytes("ASCII"), "RC4");
            rc4.init(Cipher.ENCRYPT_MODE, rc4Key);
            cipherText = rc4.update(clearText.getBytes("ASCII"));
            int counter = 0;
            while (counter < cipherText.length) {
                returnText[counter] = cipherText[counter];
                counter++;
            }
            return returnText;
        } catch (Exception e) { return null; }
    }

    public String decrypt ( int[] ciphertext, String key ) {
        byte[] clearText;
        byte[] cipherText = new byte[ciphertext.length];
        try {
            int counter = 0;
            while (counter < ciphertext.length) {
                cipherText[counter] = (byte)ciphertext[counter];
                counter++;
            }
            Cipher rc4 = Cipher.getInstance("RC4");
            SecretKeySpec rc4Key = new SecretKeySpec(key.getBytes("ASCII"), "RC4");
            rc4.init(Cipher.DECRYPT_MODE, rc4Key);
            clearText = rc4.update(cipherText);
            System.out.println(new String(clearText, "ASCII"));
            return new String(clearText, "ASCII");
        } catch (Exception e) { return ""; }

    }

}
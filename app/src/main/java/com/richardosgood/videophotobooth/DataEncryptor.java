package com.richardosgood.videophotobooth;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.res.Resources;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

public class DataEncryptor {
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String KEY_ALIAS = "VideoBoothKey";
    private KeyStore keyStore;

    public DataEncryptor() {

    }

    public boolean initialize() {
        if (!openKeystore()) {
            return false;
        }
        // Try to get the app's secret key in case it already exists
        try {
            java.security.Key key = getSecretKey();
        } catch (Exception e) {
            // It doesn't exist or couldn't open, so generate a new one
            e.printStackTrace();
            try {
                generateEncryptionKey();
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private boolean openKeystore() {
        // Try to open the keystore for storing PIN
        try {
            keyStore = KeyStore.getInstance(AndroidKeyStore);
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void generateEncryptionKey() throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
            keyGenerator.init(
                    new KeyGenParameterSpec.Builder(KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build());
            keyGenerator.generateKey();
        }
    }

    private java.security.Key getSecretKey() throws Exception {
        return keyStore.getKey(KEY_ALIAS, null);
    }

    public String encryptData(byte[] input, String iv) {
        String encryptedBase64Encoded = null;

        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, iv.getBytes()));
            byte[] encodedBytes = c.doFinal(input);
            encryptedBase64Encoded = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting pin: " + e.getMessage());
        }
        return encryptedBase64Encoded;
    }

    public String decryptData(String encryptedBase64Encoded, String iv) {
        String decoded = null;
        byte[] encrypted = Base64.decode(encryptedBase64Encoded, Base64.DEFAULT);

        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, iv.getBytes()));
            byte[] decodedBytes = c.doFinal(encrypted);
            decoded = new String(decodedBytes);
        } catch (Exception e) {
            Log.e(TAG, Resources.getSystem().getString(R.string.error_enc_decrypt));
            return null;
        }

        return decoded;
    }
}

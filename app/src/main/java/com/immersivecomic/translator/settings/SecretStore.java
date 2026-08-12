package com.immersivecomic.translator.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecretStore {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String MASTER_ALIAS = "comic_translation_mvp_master";
    private static final String PREF = "secure_secrets";

    private final SharedPreferences preferences;

    public SecretStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveSecret(String reference, String value) throws Exception {
        if (value == null || value.isEmpty()) {
            preferences.edit().remove(reference + "_iv").remove(reference + "_cipher").apply();
            return;
        }
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        preferences.edit()
                .putString(reference + "_iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString(reference + "_cipher", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .apply();
    }

    public String readSecret(String reference) throws Exception {
        String ivText = preferences.getString(reference + "_iv", null);
        String cipherText = preferences.getString(reference + "_cipher", null);
        if (ivText == null || cipherText == null) return "";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                new GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP)));
        byte[] decoded = cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP));
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public boolean hasSecret(String reference) {
        return preferences.contains(reference + "_cipher");
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        if (keyStore.containsAlias(MASTER_ALIAS)) {
            return (SecretKey) keyStore.getKey(MASTER_ALIAS, null);
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                MASTER_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }
}

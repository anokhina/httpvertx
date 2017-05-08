/*
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.jsecure;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;

//http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html
//https://docs.oracle.com/javase/8/docs/api/javax/crypto/Cipher.html
//https://tersesystems.com/2015/12/17/the-right-way-to-use-securerandom/
public class Enc {
    public static final String ENCODING = "UTF-8";
    public static final String CIPHER_NAME = "AES/CBC/PKCS5Padding";
    public static final String ALGORITHM = "AES";
    
    private final IvParameterSpec vector;
    private final SecretKeySpec key;
    private final Cipher cipher;
    
    public Enc(String v, String k) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException {
        this(v.getBytes(ENCODING), k.getBytes(ENCODING));
    }
    public Enc(byte[] v, byte[] k) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this(v, k, CIPHER_NAME, ALGORITHM);
    }
    public Enc(byte[] v, byte[] k, String cipherName, String algorithm) throws NoSuchAlgorithmException, NoSuchPaddingException {
        key = new SecretKeySpec(k, algorithm);
        vector = new IvParameterSpec(v);
        cipher = Cipher.getInstance(cipherName);
    }
    
    public String encryptStr(String inb) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return new String(encrypt(inb), ENCODING);
    }
    public byte[] encrypt(String inb) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return encrypt(inb.getBytes(ENCODING));
    }
    public byte[] encrypt(byte[] inb) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        cipher.init(Cipher.ENCRYPT_MODE, key, vector);
        return cipher.doFinal(inb);
    }
    
    public String decryptStr(String inb) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return new String(decrypt(inb.getBytes(ENCODING)), ENCODING);
    }
    public byte[] decrypt(String inb) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
        return decrypt(inb.getBytes(ENCODING));
    }
    public byte[] decrypt(byte[] inb) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        cipher.init(Cipher.DECRYPT_MODE, key, vector);
        return cipher.doFinal(inb);
    }
    public static void main(String[] args) throws Exception {
        //https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SecureRandomImp
        SecureRandom randomSecureRandom = SecureRandom.getInstanceStrong();
        byte[] vector = new byte[Cipher.getInstance(CIPHER_NAME).getBlockSize()];
        byte[] vector1 = new byte[vector.length];
        randomSecureRandom.nextBytes(vector);
        randomSecureRandom.nextBytes(vector1);
        System.out.println("1?>>>>>>>>"+vector.length);

        KeyGenerator kgen = KeyGenerator.getInstance(ALGORITHM);
        SecretKey skey = kgen.generateKey();
        SecretKey skey1 = kgen.generateKey();
        byte[] key = skey.getEncoded();        
        byte[] key1 = skey1.getEncoded();        
        System.out.println("2?>>>>>>>>"+key.length);
        Enc enc = new Enc(vector, key);
        Enc enc1 = new Enc(vector1, key);
        String str = "This is a string!";
        byte[] encrypted = enc.encrypt(str);
        System.out.println("en>>>>>>>>"+new String(encrypted, ENCODING));
        
        byte[] decrypted = enc.decrypt(encrypted);
        System.out.println("de>>>>>>>>"+new String(decrypted, ENCODING));
        
        byte[] decrypted1 = enc1.decrypt(encrypted);
        System.out.println("de>>>>>>>>"+new String(decrypted1, ENCODING));
    }
}

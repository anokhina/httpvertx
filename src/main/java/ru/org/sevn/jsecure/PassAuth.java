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

import java.io.Console;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * @author avn
 */
public class PassAuth {
    //http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html
    //https://tools.ietf.org/html/rfc2898#page-8
    public static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int SIZE = 512;
    private static final int ITERATIONS = 1000;

    private final SecretKeyFactory secretKeyFactory;
    private final String saltPrefix;
    private final int keySize;
    private final int iterations;
    private final Base64.Encoder encoder = Base64.getEncoder();
    
    public PassAuth(String saltPrefix) throws NoSuchAlgorithmException {
        this(ALGORITHM, saltPrefix, SIZE, ITERATIONS);
    }
    public PassAuth(String algorithm, String saltPrefix, int keySize, int iterations) throws NoSuchAlgorithmException {
        secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
        this.saltPrefix = saltPrefix;
        this.keySize = keySize;
        this.iterations = iterations;
    }
    
    public SecretKey generateSecret(KeySpec spec) throws InvalidKeySpecException {
        return secretKeyFactory.generateSecret(spec);
    }
    
    public String getFullSalt(String salt) {
        return saltPrefix + salt;
    }
    
    public PBEKeySpec makePBEKeySpec(String pss, String salt) {
        try {        
            return new PBEKeySpec(pss.toCharArray(), getFullSalt(salt).getBytes("UTF-8"), iterations, keySize);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(PassAuth.class.getName()).log(Level.SEVERE, null, ex);
            return new PBEKeySpec(pss.toCharArray(), getFullSalt(salt).getBytes(), iterations, keySize);
        }
    }
    
    public byte[] getHashBytes(String pss, String salt) {
        try {
            return generateSecret(makePBEKeySpec(pss, salt)).getEncoded();
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(PassAuth.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public String getHashString(String pss, String salt) {
        byte[] bytes = getHashBytes(pss, salt);
        if (bytes != null) {
            return encoder.encodeToString(bytes);
        }
        return null;
    }
    
    public boolean authenticate(String pss, String salt, String hash) {
        String newHash = getHashString(pss, salt);
        if (newHash != null) {
            return newHash.equals(hash);
        }
        return false;
    }
    
    public static String generateSalt(int len) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[len];
        random.nextBytes(salt);
        return new String(salt);
    }
    
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        if (args.length < 2) {
            System.out.println("Usage: java " + PassAuth.class.getName() + " <salt> <user> 2> out.txt");
            args = new String[] {"salt", "user"};
        }            
        Console console = System.console();
        String pss = "fake";
        if (console == null) {
            System.out.println("Couldn't get Console instance!s");
        } else {
            System.out.print("Password:");
            pss = new String(console.readPassword());
        }
        PassAuth auth = new PassAuth(args[0]);
        String hash = auth.getHashString(pss, args[1]);
        System.err.println(hash);
    }
}

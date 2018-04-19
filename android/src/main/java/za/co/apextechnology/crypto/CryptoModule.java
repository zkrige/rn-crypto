
package za.co.apextechnology.crypto;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;
import android.util.Log;
import static android.os.Build.VERSION.SDK_INT;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class CryptoModule extends ReactContextBaseJavaModule {

    private static String KEYPAIR_RSA = "RSA";
    private static String SIG_SHA256_WITH_RSA = "SHA256withRSA";
    private static String STORE_ANDROID_KEY_STORE = "AndroidKeyStore";
    private static String LOGTAG = "RNKEYSTORE";
    private static int RANDOM_SEED_SIZE = 64;

    private KeyStore keyStore;
    private int level;

    public CryptoModule(ReactApplicationContext rctx) {
        super(rctx);
        this.level = SDK_INT;
        Log.v(LOGTAG, "constructor CryptoModule");
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("STORE_ANDROID_KEY_STORE", STORE_ANDROID_KEY_STORE);
        constants.put("KEYPAIR_RSA", KEYPAIR_RSA);
        constants.put("SIG_SHA256_WITH_RSA", SIG_SHA256_WITH_RSA);
        return constants;
    }

    public String getName() {
        return "Crypto";
    }

    /**
     * Check if a keystore is loaded
     */
    @ReactMethod
    public void getKeyStoreIsLoaded(Promise promise) {
        promise.resolve(this.keyStore != null);
    }

    /**
     * Get all the key aliases from the currently loaded keystore
     */
    @ReactMethod
    public void getKeyAliases(Promise promise) {
        if (this.rejectIfNotLoaded(promise)) return;
        try {
            WritableNativeArray arr = new WritableNativeArray();
            Enumeration<String> aliases = this.keyStore.aliases();
            while (aliases.hasMoreElements()) {
                arr.pushString(aliases.nextElement());
            }
            promise.resolve(arr);
        } catch (KeyStoreException e) {
            promise.reject(e);
        }
    }

    /**
     * Check if the currently loaded store contains a key with the given alias
     * @param The alias of the key
     */
    @ReactMethod
    public void containsKeyAlias(String alias, Promise promise) {
        if (this.rejectIfNotLoaded(promise)) return;
        try {
            promise.resolve(this.keyStore.containsAlias(alias));
        } catch (KeyStoreException e) {
            promise.reject(e);
        }
    }

    /**
     * Delete an key entry from the currently loaded keystore
     * @param The alias of the key entry
     */
    @ReactMethod
    public void deleteKeyEntry(String alias, Promise promise) {
        if (this.rejectIfNotLoaded(promise)) return;
        try {
            this.keyStore.deleteEntry(alias);
            this.keyStore.store(null);
            promise.resolve(null);
        } catch (KeyStoreException e) {
            promise.reject(e);
        } catch (IOException e) {
            promise.reject(e);
        } catch (NoSuchAlgorithmException e) {
            promise.reject(e);
        } catch (CertificateException e) {
            promise.reject(e);
        }
    }

    /**
     * Get a certificate from the currently loaded keystore by alias
     * @param The alias of the certificate
     * @return The certificate
     */
    @ReactMethod
    public void getKeyStoreCertificate(String alias, Promise promise) {
        if (this.rejectIfNotLoaded(promise)) return;
        try {
            promise.resolve(this.keyStore.getCertificate(alias).toString());
        } catch (KeyStoreException e) {
            promise.reject(e);
        }
    }

    /**
     * Get the number of keys in the currently loaded keystore
     * @return The number of keys
     */
    @ReactMethod
    public void keyStoreSize(Promise promise) {
        if (this.rejectIfNotLoaded(promise)) return;
        try {
            promise.resolve(this.keyStore.size());
        } catch (KeyStoreException e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void loadKeyStore(Promise promise) {
        Log.v(LOGTAG, "entered loadKeyStore");
        try {
            if (this.keyStore == null) this.init();
            this.keyStore.load(null);
            promise.resolve(null);
        } catch (NoSuchAlgorithmException e) {
            promise.reject(e);
        } catch (FileNotFoundException e) {
            promise.reject(e);
        } catch (KeyStoreException e) {
            promise.reject(e);
        } catch (CertificateException e) {
            promise.reject(e);
        } catch (IOException e) {
            promise.reject(e);
        }
    }

    /**
     * Set the keystore to null
     */
    @ReactMethod
    public void unloadKeyStore(Promise promise) {
        try {
            this.keyStore.store(null);
            this.keyStore = null;
            promise.resolve(null);
        } catch (KeyStoreException kse) {
            promise.reject(kse);
        } catch (IOException ioe) {
            promise.reject(ioe);
        } catch (NoSuchAlgorithmException nsae) {
            promise.reject(nsae);
        } catch (CertificateException ce) {
            promise.reject(ce);
        }
    }

    /**
     * Sign a message with a key
     */
    @ReactMethod
    public void sign(
            String dataString,
            String alias,
            Promise promise
    ) {
        if (this.rejectIfNotLoaded(promise)) return;
        // Sign with keys
        try {
            PrivateKey privateKey = (PrivateKey) this.keyStore.getKey(alias, null);
            Signature sig = Signature.getInstance(SIG_SHA256_WITH_RSA);
            sig.initSign(privateKey, this.newSecureRandom());
            byte[] data = dataString.getBytes(StandardCharsets.UTF_8);
            sig.update(data);
            byte[] signature = sig.sign();
            promise.resolve(Base64.encodeToString(signature, Base64.NO_WRAP));
        } catch (NoSuchAlgorithmException e) {
            promise.reject(e);
        } catch (KeyStoreException e) {
            promise.reject(e);
        } catch (InvalidKeyException e) {
            promise.reject(e);
        } catch (SignatureException e) {
            promise.reject(e);
        } catch (UnrecoverableKeyException e) {
            promise.reject(e);
        }
    }

    /**
     * Verify a message
     */
    @ReactMethod
    public void verify(
            String signature,
            String alias,
            Promise promise
    ) {
        if (this.rejectIfNotLoaded(promise)) return;
        // Verify with keys
        try {
            PublicKey publicKey = (PublicKey) this.keyStore.getKey(alias, null);
            Signature sig = Signature.getInstance(SIG_SHA256_WITH_RSA);
            sig.initVerify(publicKey);
            promise.resolve(sig.verify(Base64.decode(signature, Base64.DEFAULT)));
        } catch (InvalidKeyException e) {
            promise.reject(e);
        } catch (KeyStoreException e) {
            promise.reject(e);
        } catch (NoSuchAlgorithmException e) {
            promise.reject(e);
        } catch (SignatureException e) {
            promise.reject(e);
        } catch (UnrecoverableKeyException e) {
            promise.reject(e);
        }
    }

    /**
     * Get a specified key formatted as a pem string
     * @param alias The alias of the keypair
     */
    @ReactMethod
    public void getKeyAsPem(
            String alias,
            Promise promise
    ) {
        if (this.rejectIfNotLoaded(promise)) return;
        try {
            Key key = (Key) this.keyStore.getKey(alias, null);
            if (key == null) {
                promise.reject(
                        new IllegalStateException("Key with alias " + alias + " does not exist")
                );
                return;
            }
            promise.resolve(this.toPemString(key));
        } catch (KeyStoreException e) {
            promise.reject(e);
        } catch (NoSuchAlgorithmException e) {
            promise.reject(e);
        } catch (UnrecoverableKeyException e) {
            promise.reject(e);
        }
    }

    /**
     * Generate a new keypair and save it to the currently loaded store
     * @param alias The alias of the keypair
     * @param certificateFilename The filename/path of the X509 certificate the key is signed with
     * @param promise com.facebook.react.bridge.Promise;
     * @return void
     */
    @ReactMethod
    public void addKeyPair(
            String alias,
            String certificateFilename,
            Promise promise
    ) {
        Log.v(LOGTAG, "entered addKeyPair");
        if (this.rejectIfNotLoaded(promise)) return;

        boolean fingerprint = alias.contains("fingerprint");

        try {
            // Check if already exists
            Enumeration<String> aliases = this.keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String existing_alias = aliases.nextElement();
                if (existing_alias.equals(alias)) {
                    promise.reject(new IllegalStateException("A key with that alias already exists"));
                    return;
                }
            }

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEYPAIR_RSA);
            if (this.level == 23) {
                keyPairGenerator.initialize((
                        new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                                .setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4))
                                .setDigests(KeyProperties.DIGEST_SHA256)
                                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                                .setUserAuthenticationRequired(fingerprint)
                                .setUserAuthenticationValidityDurationSeconds(60)
                                .build()
                ), this.newSecureRandom());
            } else {
                keyPairGenerator.initialize(4096, this.newSecureRandom());
                Log.v(LOGTAG, "initialize kpg");
            }

            // Create a keypair
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            Log.v(LOGTAG, "kpg generate key pair");
            PrivateKey privateKey = keyPair.getPrivate();
            Log.v(LOGTAG, "keypair get private");
            PublicKey publicKey = keyPair.getPublic();
            Log.v(LOGTAG, "keypair get public");

            WritableMap map = Arguments.createMap();

            // Load certificate
            Certificate[] certChain = {this.loadCertificate(certificateFilename)};
            Log.v(LOGTAG, "load certificate");
            this.keyStore.setKeyEntry(alias, privateKey, null, certChain);
            Log.v(LOGTAG, "set key entry private");

            map.putString("publicKeyPem", this.toPemString(publicKey));
            promise.resolve(map);
        } catch (KeyStoreException kse) {
            Log.v(LOGTAG, "KSE - " + kse.getStackTrace());
            promise.reject(kse);
        } catch (NoSuchAlgorithmException nsae) {
            Log.v(LOGTAG, "NSAE - " + nsae.getStackTrace());
            promise.reject(nsae);
        } catch (InvalidAlgorithmParameterException iape) {
            Log.v(LOGTAG, "IAPE - " + iape.getStackTrace());
            promise.reject(iape);
        } catch (IOException ioe) {
            Log.v(LOGTAG, "IOE - " + ioe.getStackTrace());
            promise.reject(ioe);
        } catch (CertificateException ce) {
            Log.v(LOGTAG, "CE - " + ce.getStackTrace());
            promise.reject(ce);
        }
    }

    @ReactMethod
    public void secureRandom(Promise promise) {
        SecureRandom rand = this.newSecureRandom();
        byte[] randBytes = new byte[32];
        rand.nextBytes(randBytes);
        promise.resolve(Base64.encodeToString(randBytes, Base64.NO_WRAP));
    }

    private boolean rejectIfNotLoaded(Promise promise) {
        if (this.keyStore == null) {
            promise.reject(
                    new IllegalStateException(
                            "Keystore must first be loaded before calling addKeyPair()"
                    )
            );
            return true;
        }
        return false;
    }

    /**
     * Create a new random seed
     * @return A new seeded SecureRandom
     */
    private SecureRandom newSecureRandom() {
        SecureRandom random = new SecureRandom();
        byte[] seed = random.generateSeed(RANDOM_SEED_SIZE);
        random.setSeed(seed);
        return random;
    }

    /**
     * Load a certificate from the given location
     */
    private Certificate loadCertificate(String certificateFilename) throws IOException, CertificateException {
        AssetManager assetManager = getReactApplicationContext().getAssets();
        BufferedInputStream bis = new BufferedInputStream(assetManager.open(certificateFilename));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(bis);
    }

    /**
     * Initialise the keystore
     */
    private void init() throws KeyStoreException, IOException {
        this.keyStore = KeyStore.getInstance(STORE_ANDROID_KEY_STORE);
    }

    private String toPemString(Key key) {
        return key != null ? (
                "-----BEGIN PUBLIC KEY-----\n" +
                        Base64.encodeToString(key.getEncoded(), Base64.DEFAULT) +
                        "-----END PUBLIC KEY-----"
        ) : "";
    }
}

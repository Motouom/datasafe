package de.adorsys.docusafe2.business.impl.keystore.service;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.docusafe2.business.api.keystore.KeyStoreService;
import de.adorsys.docusafe2.business.api.keystore.types.*;
import de.adorsys.docusafe2.business.impl.keystore.generator.KeyStoreCreationConfigImpl;
import de.adorsys.docusafe2.business.impl.keystore.generator.KeyStoreServiceImplBaseFunctions;
import de.adorsys.docusafe2.business.impl.keystore.generator.PasswordCallbackHandler;
import de.adorsys.docusafe2.business.impl.keystore.generator.ProviderUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class KeyStoreServiceTest {

    private KeyStoreService keyStoreService = new KeyStoreServiceImpl();
    private KeyStoreAuth keyStoreAuth;

    @BeforeEach
    public void setUp() {
        ReadStorePassword readStorePassword = new ReadStorePassword("keystorepass");
        ReadKeyPassword readKeyPassword = new ReadKeyPassword("keypass");
        keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);
    }

    @Test
    public void createKeyStore() throws Exception {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(1, 0, 1);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);

        Assertions.assertNotNull(keyStore);

        List<String> list = Collections.list(keyStore.aliases());
        Assertions.assertEquals(2, list.size());

        Assertions.assertEquals("UBER", keyStore.getType());
        Assertions.assertEquals(Security.getProvider("BC"), keyStore.getProvider());
    }

    @Test
    public void createKeyStoreEmptyConfig() throws Exception {
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, null);
        Assertions.assertNotNull(keyStore);
        List<String> list = Collections.list(keyStore.aliases());
        Assertions.assertEquals(15, list.size());
    }

    @Test
    public void createKeyStoreException() {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(0, 0, 0);

            Assertions.assertThrows(BaseException.class, () ->keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config));
    }

    @Test
    public void getPublicKeys() {
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, null);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        List<PublicKeyIDWithPublicKey> publicKeys = keyStoreService.getPublicKeys(keyStoreAccess);
        Assertions.assertEquals(5, publicKeys.size());
    }

    @Test
    public void getPrivateKey() throws Exception {
        Provider bcProvider = ProviderUtils.bcProvider;
        KeyStore keyStore = KeyStoreServiceImplBaseFunctions.newKeyStore(KeyStoreType.DEFAULT); // UBER

        ReadKeyPassword readKeyPassword = new ReadKeyPassword("keypass");
        CallbackHandler readKeyHandler = new PasswordCallbackHandler(readKeyPassword.getValue().toCharArray());
        KeyStoreCreationConfigImpl keyStoreCreationConfig = new KeyStoreCreationConfigImpl(null);
        KeyPairGenerator encKeyPairGenerator = keyStoreCreationConfig.getEncKeyPairGenerator("KEYSTORE-ID-0");
        String alias = "KEYSTORE-ID-0" + UUID.randomUUID().toString();
        KeyPairEntry keyPairEntry = encKeyPairGenerator.generateEncryptionKey(alias, readKeyHandler);
        KeyStoreServiceImplBaseFunctions.addToKeyStore(keyStore, keyPairEntry);

        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

        String keyID = keyStore.aliases().nextElement();
        PrivateKey privateKey = keyStoreService.getPrivateKey(keyStoreAccess, new KeyID(keyID));
        System.out.println(privateKey);
        System.out.println(keyID);
        Assertions.assertEquals(alias, keyID);
    }

    @Test
    public void getPrivateKeyException() throws Exception {
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, null);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        List<String> list = Collections.list(keyStore.aliases());
        for(String id : list) {
            System.out.println(keyStoreService.getPrivateKey(keyStoreAccess, new KeyID(id)));
        }
    }

    @Test
    public void getSecretKey() throws Exception {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(0, 0, 1);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

        String keyID = keyStore.aliases().nextElement();
        SecretKey secretKey = keyStoreService.getSecretKey(keyStoreAccess, new KeyID(keyID));
        Assertions.assertNotNull(secretKey);
    }

    @Test
    public void getRandomSecretKeyID() {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(10, 10, 1);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        SecretKeyIDWithKey randomSecretKeyID = keyStoreService.getRandomSecretKeyID(keyStoreAccess);
        Assertions.assertNotNull(randomSecretKeyID);
    }

    @Test
    public void getRandomSecretKeyIDException() {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(10, 10, 0);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        Assertions.assertThrows(BaseException.class, () ->keyStoreService.getRandomSecretKeyID(keyStoreAccess));
    }
}
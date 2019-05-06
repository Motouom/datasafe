package de.adorsys.datasafe.business.impl.encryption.pathencryption;

import de.adorsys.datasafe.business.api.encryption.keystore.KeyStoreService;
import de.adorsys.datasafe.business.api.encryption.pathencryption.PathEncryption;
import de.adorsys.datasafe.business.api.encryption.pathencryption.encryption.SymmetricPathEncryptionService;
import de.adorsys.datasafe.business.api.profile.keys.PrivateKeyService;
import de.adorsys.datasafe.business.api.types.UserIDAuth;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.net.URI;

import static de.adorsys.datasafe.business.api.types.keystore.KeyStoreCreationConfig.PATH_KEY_ID;

public class PathEncryptionImpl implements PathEncryption {

    private final SymmetricPathEncryptionService bucketPathEncryptionService;
    private final PrivateKeyService privateKeyService;
    private final KeyStoreService keyStoreService;

    @Inject
    public PathEncryptionImpl(SymmetricPathEncryptionService symmetricPathEncryptionService,
                              PrivateKeyService privateKeyService, KeyStoreService keyStoreService) {
        this.bucketPathEncryptionService = symmetricPathEncryptionService;
        this.privateKeyService = privateKeyService;
        this.keyStoreService = keyStoreService;
    }

    @Override
    public URI encrypt(UserIDAuth forUser, URI path) {
        SecretKeySpec keySpec = keyStoreService.getSecretKey(
                privateKeyService.keystore(forUser),
                PATH_KEY_ID
        );

        return bucketPathEncryptionService.encrypt(keySpec, path);
    }

    @Override
    public URI decrypt(UserIDAuth forUser, URI path) {
        SecretKeySpec keySpec = keyStoreService.getSecretKey(
                privateKeyService.keystore(forUser),
                PATH_KEY_ID
        );

        return bucketPathEncryptionService.decrypt(keySpec, path);
    }
}

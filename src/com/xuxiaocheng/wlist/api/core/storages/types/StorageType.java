package com.xuxiaocheng.wlist.api.core.storages.types;

import com.xuxiaocheng.wlist.api.core.CoreClient;
import com.xuxiaocheng.wlist.api.core.storages.configs.Config;
import com.xuxiaocheng.wlist.api.core.storages.information.StorageInformation;
import com.xuxiaocheng.wlist.api.impl.ClientStarter;
import com.xuxiaocheng.wlist.api.impl.enums.Functions;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * An interface that represents a type of storage.
 * Each implementation should be a singleton.
 * (e.g., An enumeration class with only {@code Instance}.)
 */
public sealed interface StorageType<C extends Config> extends Serializable permits Lanzou {
    Functions functionAdd();
    Functions functionUpdate();
    Functions functionCheck();

    ClientStarter.PackFunction configPacker(final C config);


    /**
     * Add a new storage.
     * @param client the core client.
     * @param token the core token.
     * @param storage the name of the storage to add.
     * @param config the storage configuration.
     * @return a future, with the information of the new storage.
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.DuplicateStorageException
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.IncorrectStorageAccountException
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.InvalidStorageConfigException
     * @see com.xuxiaocheng.wlist.api.common.exceptions.TooLargeDataException
     */
    default CompletableFuture<StorageInformation> add(final CoreClient client, final String token, final String storage, final C config) {
        return ClientStarter.client(client, this.functionAdd(), packer -> {
            packer.packString(token);
            packer.packString(storage);
            this.configPacker(config).pack(packer);
        }, StorageInformation::deserialize);
    }

    /**
     * Reset the config of the storage.
     * @param client the core client.
     * @param token the core token.
     * @param storage the id of the storage.
     * @param config the new configuration.
     * @return a future.
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.IncorrectStorageAccountException
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.InvalidStorageConfigException
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.StorageTypeMismatchedException
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.StorageInLockException
     * @see com.xuxiaocheng.wlist.api.common.exceptions.TooLargeDataException
     */
    default CompletableFuture<Void> update(final CoreClient client, final String token, final long storage, final C config) {
        return ClientStarter.client(client, this.functionUpdate(), packer -> {
            packer.packString(token);
            packer.packLong(storage);
            this.configPacker(config).pack(packer);
        }, ClientStarter::deserializeVoid);
    }

    /**
     * Check whether the configuration is valid.
     * Note that this method won't check whether the account is correct/available.
     * @param client the core client.
     * @param token the core token.
     * @param config the configuration to check.
     * @return a future, normal completion means the configuration is valid.
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.InvalidStorageConfigException
     * @see com.xuxiaocheng.wlist.api.common.exceptions.TooLargeDataException
     */
    default CompletableFuture<Void> checkConfig(final CoreClient client, final String token, final C config) {
        return ClientStarter.client(client, this.functionCheck(), packer -> {
            packer.packString(token);
            this.configPacker(config).pack(packer);
        }, ClientStarter::deserializeVoid);
    }

    /**
     * Return true means the storage is private. (User's personal account.)
     * @return true if the storage is private.
     */
    boolean isPrivate();

    /**
     * Return true means the storage is shared. (Other's share link.)
     * @return true if the storage is shared.
     */
    default boolean isShared() { return !this.isPrivate(); }

    static StorageType<?> instanceOf(final String name) {
        return switch (name) {
            case "lanzou" -> Lanzou.Instance;
            default -> throw new IllegalArgumentException("Unknown storage type: " + name);
        };
    }

    static String name(final StorageType<?> type) {
        return switch (type.getClass().getCanonicalName()) {
            case "com.xuxiaocheng.wlist.api.core.storages.types.Lanzou" -> "lanzou";
            default -> throw new IllegalStateException("Unexpected value: " + type.getClass().getCanonicalName());
        };
    }


    /**
     * Return all the suffixes the storage allowed.
     * Note that empty set means all suffixes are valid.
     * This method is only for fast check, some cases may not be covered.
     * @return all the suffixes the storage allowed.
     */
    default Set<String> allowedSuffixes() { return Set.of(); }

    /**
     * Return all the suffixes the storage disallowed.
     * Note that empty set means all suffixes are valid.
     * This method is only for fast check, some cases may not be covered.
     * @return all the suffixes the storage disallowed.
     */
    default Set<String> disallowedSuffixes() { return Set.of(); }

    /**
     * Return all the code points the storage disallowed.
     * Note that empty set means all code points are valid.
     * This method is only for fast check, some cases may not be covered.
     * @return all the code points the storage disallowed.
     */
    default Set<Character> disallowedCharacter() { return Set.of('/', '\\', ':'); }

    /**
     * Return the min length of the filename the storage allowed.
     * This method is only for fast check, some cases may not be covered.
     * @return the min length of the filename the storage allowed.
     */
    default long minFilenameLength() { return 0; }

    /**
     * Return the max length of the filename the storage allowed. (-1 means infinity)
     * This method is only for fast check, some cases may not be covered.
     * @return the max length of the filename the storage allowed.
     */
    default long maxFilenameLength() { return -1; }
}

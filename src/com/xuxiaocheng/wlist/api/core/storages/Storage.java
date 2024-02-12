package com.xuxiaocheng.wlist.api.core.storages;

import com.xuxiaocheng.wlist.api.Main;
import com.xuxiaocheng.wlist.api.core.CoreClient;

import java.util.concurrent.CompletableFuture;

public enum Storage {;
    /**
     * Remove a storage.
     * @param client the core client
     * @param token the core token
     * @param storage the name of the storage to remove.
     * @return a future.
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.StorageNotFoundException
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.StorageInLockedException
     */
    public static CompletableFuture<Void> remove(final CoreClient client, final String token, final String storage) { return Main.future(); }

    /**
     * Add a lanzou storage.
     * @param client the core client
     * @param token the core token
     * @param storage the name of the storage to add.
     * @return a future.
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.StorageExistsException
     * @see com.xuxiaocheng.wlist.api.core.storages.exceptions.IncorrectStorageAccountException
     */
    public static CompletableFuture<Void> addLanzou(final CoreClient client, final String token, final String storage, final LanzouConfig config) { return Main.future(); }
    public record LanzouConfig(String phoneNumber, String password, long rootDirectoryId) { }
}

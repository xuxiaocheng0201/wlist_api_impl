package com.xuxiaocheng.wlist.api.core.files.exceptions.limitations;

import com.xuxiaocheng.wlist.api.impl.enums.Exceptions;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.io.Serial;

/**
 * Thrown when creating/uploading/modifying in a read-only storage.
 */
public class ReadOnlyStorageException extends RuntimeException implements Exceptions.CustomExceptions {
    @Serial
    private static final long serialVersionUID = -5809318309571708372L;

    /**
     * the id of the backend storage.
     */
    protected final long storage;

    /**
     * Internal constructor.
     * @param storage the id of the backend storage.
     */
    private ReadOnlyStorageException(final long storage) {
        super("Readonly storage: " + storage);
        this.storage = storage;
    }

    /**
     * Get the id of the backend storage.
     * @return the id of the backend storage.
     */
    public long getStorage() {
        return this.storage;
    }

    @Override
    public Exceptions identifier() {
        return Exceptions.ReadOnlyStorage;
    }

    @Override
    public void serialize(final MessagePacker packer) throws IOException {
        packer.packLong(this.storage);
    }

    public static ReadOnlyStorageException deserialize(final MessageUnpacker unpacker) throws IOException {
        final long storage = unpacker.unpackLong();
        return new ReadOnlyStorageException(storage);
    }
}

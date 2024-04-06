package com.xuxiaocheng.wlist.api.core.files.exceptions;

import com.xuxiaocheng.wlist.api.core.files.FileLocation;
import com.xuxiaocheng.wlist.api.impl.enums.Exceptions;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.io.Serial;

/**
 * Thrown if the file/directory is locked.
 */
public class FileInLockException extends RuntimeException implements Exceptions.CustomExceptions {
    @Serial
    private static final long serialVersionUID = 2802445176796214285L;

    /**
     * The location of the locked file/directory.
     */
    protected final FileLocation location;

    /**
     * Internal constructor.
     * @param location the location of the locked file/directory.
     * @param message error message.
     */
    private FileInLockException(final FileLocation location, final String message) {
        super(location.toString() + ": " + message);
        this.location = location;
    }

    /**
     * Get the location of the locked file/directory.
     * @return the location of the locked file/directory.
     */
    public FileLocation getLocation() {
        return this.location;
    }

    @Override
    public Exceptions identifier() {
        return Exceptions.FileInLock;
    }

    @Override
    public void serialize(final MessagePacker packer) throws IOException {
        FileLocation.serialize(this.location, packer);
        packer.packString(this.message);
    }

    public static FileInLockException deserialize(final MessageUnpacker unpacker) throws IOException {
        final FileLocation parent = FileLocation.deserialize(unpacker);
        final String message = unpacker.unpackString();
        return new FileInLockException(parent, message);
    }
}

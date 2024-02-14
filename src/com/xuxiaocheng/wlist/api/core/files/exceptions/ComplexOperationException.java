package com.xuxiaocheng.wlist.api.core.files.exceptions;

import java.io.Serial;

/**
 * Thrown when the operation is complex. (Complex means the operation cannot be done in one step.)
 */
public class ComplexOperationException extends Exception {
    @Serial
    private static final long serialVersionUID = -8826485751328024761L;

    /**
     * Internal constructor.
     */
    public ComplexOperationException() {
        super();
    }
}
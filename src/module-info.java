/**
 * The api of wlist project.
 */
module com.xuxiaocheng.wlist.api {
    requires io.netty.common;
    requires io.netty.transport;
    requires io.netty.codec;
    requires io.netty.buffer;
    requires msgpack.core;
    requires htmlunit;

    exports com.xuxiaocheng.wlist.api;
    exports com.xuxiaocheng.wlist.api.common;
    exports com.xuxiaocheng.wlist.api.common.exceptions;
    exports com.xuxiaocheng.wlist.api.common.either;
    exports com.xuxiaocheng.wlist.api.core;
    exports com.xuxiaocheng.wlist.api.core.exceptions;
    exports com.xuxiaocheng.wlist.api.core.storages;
    exports com.xuxiaocheng.wlist.api.core.storages.exceptions;
    exports com.xuxiaocheng.wlist.api.core.storages.configs;
    exports com.xuxiaocheng.wlist.api.core.storages.types;
    exports com.xuxiaocheng.wlist.api.core.storages.options;
    exports com.xuxiaocheng.wlist.api.core.storages.information;
    exports com.xuxiaocheng.wlist.api.core.files;
    exports com.xuxiaocheng.wlist.api.core.files.exceptions;
    exports com.xuxiaocheng.wlist.api.core.files.exceptions.limitations;
    exports com.xuxiaocheng.wlist.api.core.files.options;
    exports com.xuxiaocheng.wlist.api.core.files.confirmations;
    exports com.xuxiaocheng.wlist.api.core.files.tokens;
    exports com.xuxiaocheng.wlist.api.core.files.information;
    exports com.xuxiaocheng.wlist.api.core.files.progresses;
    exports com.xuxiaocheng.wlist.api.core.broadcast;
    exports com.xuxiaocheng.wlist.api.core.broadcast.events;
    exports com.xuxiaocheng.wlist.api.web;

    exports com.xuxiaocheng.wlist.api.impl;
    exports com.xuxiaocheng.wlist.api.impl.data;
    exports com.xuxiaocheng.wlist.api.impl.enums;
    exports com.xuxiaocheng.wlist.api.impl.functions;
    exports com.xuxiaocheng.wlist.api.impl.functions.types;
}

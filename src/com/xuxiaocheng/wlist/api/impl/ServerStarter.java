package com.xuxiaocheng.wlist.api.impl;

import com.xuxiaocheng.wlist.api.common.exceptions.InternalException;
import com.xuxiaocheng.wlist.api.common.exceptions.NetworkException;
import com.xuxiaocheng.wlist.api.common.exceptions.UnavailableApiVersionException;
import com.xuxiaocheng.wlist.api.core.exceptions.MultiInstanceException;
import com.xuxiaocheng.wlist.api.impl.enums.Exceptions;
import com.xuxiaocheng.wlist.api.impl.enums.Functions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePackException;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerStarter {
    private ServerStarter() {
        super();
    }

    public static final EventExecutorGroup CodecExecutors = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 2, new DefaultThreadFactory("CodecExecutors"));
    public static final EventExecutorGroup ServerExecutors = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() << 1, new DefaultThreadFactory("ServerExecutors"));

    /**
     * 0: Stopped,
     * 1: Starting,
     * 2: Started,
     * 3: Stopping,
     */
    private static final AtomicInteger started = new AtomicInteger(0);
    private static final EventExecutorGroup bossGroup = new NioEventLoopGroup(Math.max(1, Runtime.getRuntime().availableProcessors() >>> 1));
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 1);

    public static int start(final int defaultPort) {
        if (!ServerStarter.started.compareAndSet(0, 1)) throw new MultiInstanceException();
        try {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(ServerStarter.workerGroup, ServerStarter.workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);
            serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
            serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) {
                    final ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(ServerStarter.CodecExecutors, "ClosedController", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
                            if (ServerStarter.started.get() != 2)
                                ctx.close();
                            else
                                super.channelActive(ctx);
                        }
                    });
                    pipeline.addLast(ServerStarter.CodecExecutors, "LengthDecoder", new LengthFieldBasedFrameDecoder(ServerStarter.getMaxPacketSize(), 0, 4, 0, 4));
                    pipeline.addLast(ServerStarter.CodecExecutors, "LengthEncoder", new LengthFieldPrepender(4));
                    pipeline.addLast(ServerStarter.CodecExecutors, "Cipher", new ServerCodec());
                    pipeline.addLast(ServerStarter.ServerExecutors, "ServerHandler", ServerStarter.handlerInstance);
                }
            });
            ChannelFuture future = null;
            try {
                boolean flag = false;
                if (defaultPort != 0) {
                    future = serverBootstrap.bind(defaultPort).await();
                    if (future.cause() != null)
                        flag = true;
                }
                if (defaultPort == 0 || flag) {
                    future = serverBootstrap.bind(0).await();
                    if (future.cause() != null)
                        throw new NetworkException("Starting server.", future.cause());
                }
            } catch (final InterruptedException exception) {
                throw new InternalException("Waiting for server to bind on port.");
            }
            final InetSocketAddress address = (InetSocketAddress) future.channel().localAddress();
            ServerStarter.started.set(2);
            return address.getPort();
        } finally {
            ServerStarter.started.compareAndSet(1, 0);
        }
    }

    public static void stop() {
        if (!ServerStarter.started.compareAndSet(2, 3)) return;
        try {
            final Future<?>[] futures = new Future<?>[2];
            futures[0] = ServerStarter.bossGroup.shutdownGracefully();
            futures[1] = ServerStarter.workerGroup.shutdownGracefully();
            for (final Future<?> future: futures)
                future.syncUninterruptibly();
        } catch (final Throwable throwable) {
            throw new InternalException("Stopping server. " + throwable);
        } finally {
            ServerStarter.started.set(0);
        }
    }


    private static native void active(final ServerChannelHandler handler, final ChannelHandlerContext ctx, final String id);
    private static native void inactive(final ServerChannelHandler handler, final ChannelHandlerContext ctx, final String id);
    private static native void exception(final ServerChannelHandler handler, final ChannelHandlerContext ctx, final String id, final Throwable throwable);

    private static final ChannelHandler handlerInstance = new ServerChannelHandler();
    @ChannelHandler.Sharable
    public static class ServerChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            ServerStarter.active(this, ctx, ctx.channel().id().asLongText());
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) {
            ServerStarter.inactive(this, ctx, ctx.channel().id().asLongText());
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
            final CompletableFuture<ByteBuf> future;
            try {
                final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteBufInputStream(msg));
                final Functions functions = Functions.valueOf(unpacker.unpackString());
                future = functions.getHandler().handle(ctx.channel().id().asLongText(), unpacker);
            } catch (final IOException throwable) {
                ctx.fireExceptionCaught(throwable);
                return;
            }
            msg.retain();
            future.handle((result, ex) -> {
                msg.release();
                if (ex != null)
                    ctx.fireExceptionCaught(ex);
                else if (result != null)
                    ctx.writeAndFlush(result)
                            .addListener(f -> { if (!f.isSuccess()) ctx.fireExceptionCaught(f.cause()); });
                return null;
            });
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            if (cause instanceof CodecException || cause instanceof SocketException) ctx.close();
            if (cause instanceof IOException || cause instanceof IllegalArgumentException) return;
            ServerStarter.exception(this, ctx, ctx.channel().id().asLongText(), cause);
        }
    }


    private static native int getMaxPacketSize();
    private static native void encode(final ServerCodec codec, final ChannelHandlerContext ctx, final String id, final ByteBuf msg, final List<Object> out);
    private static native void decode(final ServerCodec codec, final ChannelHandlerContext ctx, final String id, final ByteBuf msg, final List<Object> out);

    public static class ServerCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
        @Override
        protected void encode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out) {
            ServerStarter.encode(this, ctx, ctx.channel().id().asLongText(), msg, out);
        }

        @Override
        protected void decode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out) {
            ServerStarter.decode(this, ctx, ctx.channel().id().asLongText(), msg, out);
        }
    }


    public interface UnpackAndCallFunction<T> {
        CompletableFuture<T> unpackAndCall() throws IOException;
    }

    public interface ReturnAndPackFunction<T> {
        void returnAndPack(final T returned, final MessagePacker packer) throws IOException;
    }

    public static void serializeVoid(final Void ignored, final MessagePacker packer) throws IOException {
        packer.packNil();
    }

    public static <T> CompletableFuture<ByteBuf> server(final UnpackAndCallFunction<? extends T> unpackFunction, final ReturnAndPackFunction<T> packFunction) {
        return CompletableFuture.completedFuture(null)
                .thenCompose(ignored -> {
                    try {
                        return unpackFunction.unpackAndCall();
                    } catch (final MessagePackException | IOException | IllegalArgumentException exception) {
                        throw new UnavailableApiVersionException(exception);
                    }
                })
                .handle((value, throwable) -> {
                    final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
                    try (final MessagePacker packer = MessagePack.newDefaultPacker(new ByteBufOutputStream(buffer))) {
                        if (throwable == null) {
                            packer.packBoolean(true);
                            packFunction.returnAndPack(value, packer);
                        } else {
                            if (throwable instanceof final CompletionException exception)
                                throwable = exception.getCause();
                            packer.packBoolean(false);
                            if (throwable instanceof final Exceptions.CustomExceptions exceptions) {
                                packer.packString(exceptions.identifier().name());
                                exceptions.serialize(packer);
                            } else
                                packer.packString(Exceptions.Internal.name());
                        }
                    } catch (final IOException exception) {
                        throw new NetworkException("Packing msg", exception);
                    }
                    return buffer;
                });
    }
}

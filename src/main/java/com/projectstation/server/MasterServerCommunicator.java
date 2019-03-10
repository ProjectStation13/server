package com.projectstation.server;

import com.projectstation.network.IClientVisit;
import com.projectstation.network.IServerVisit;
import com.projectstation.network.ServerDescription;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

public class MasterServerCommunicator {
    private final Logger logger = LoggerFactory.getLogger(MasterServerCommunicator.class);
    private ServerDescription serverDescription;
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final WebSocketClientHandler clientHandler = new WebSocketClientHandler();

    public MasterServerCommunicator(String masterHost, int masterPort, int hostport, String name, String description, int maxPlayers) {
        serverDescription = new ServerDescription(hostport, name, description, maxPlayers);
        initNetwork(masterHost, masterPort);
    }

    private void initNetwork(String host, int port) {
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(
                                    new ByteArrayEncoder(),
                                    new ByteArrayDecoder(),
                                    clientHandler);
                        }
                    });

            ChannelFuture channel = b.connect(host, port);
            channel.await();

            if(!channel.isSuccess())
                logger.error("Unable to establish connection", channel.cause());

        } catch (InterruptedException ex) {
            logger.error("Unable to establish connection", ex);
            Thread.currentThread().interrupt();
        }
    }

    public void setPlayerCount(int count) {
        if(count == serverDescription.players)
            return;

        serverDescription.players = count;
        clientHandler.sendDescription();
    }

    public void stop() {
        workerGroup.shutdownGracefully();
    }

    public class WebSocketClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private ChannelHandlerContext ctx = null;

        public void sendDescription() {
            try {
                JsonVariable var = new JsonVariable();
                serverDescription.serialize(var);


                ByteArrayOutputStream serializedJson = new ByteArrayOutputStream();
                var.serialize(serializedJson, false);

                clientHandler.send(serializedJson +  "\n\r");
                clientHandler.flush();
            } catch (ValueSerializationException | IOException e) {
                logger.error("Error sending server description.", e);
            }
        }


        public void disconnect() {
            if(ctx != null) {
                ctx.disconnect();
            }
        }

        public void send(String msg) {
            if(ctx == null)
                return;

            ctx.write(msg.getBytes()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(!channelFuture.isSuccess())
                        logger.error("Error transmitting message", channelFuture.cause());
                }
            });
        }

        public void flush() {
            if(ctx != null)
                ctx.flush();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            if(ctx == this.ctx)
                this.ctx = null;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            this.ctx = ctx;
            sendDescription();
        }

        @Override
        protected void messageReceived(ChannelHandlerContext channelHandlerContext, ByteBuf clientVisit) throws Exception { }

        public boolean isConnected() {
            return ctx != null && !ctx.isRemoved();
        }
    }
}

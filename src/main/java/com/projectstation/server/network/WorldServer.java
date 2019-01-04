package com.projectstation.server.network;

import com.projectstation.network.*;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapter;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.server.ServerStationEntityFactory;
import com.projectstation.server.network.entity.ServerNetworkEntityMappings;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

public class WorldServer {
    private static final Logger logger = LoggerFactory.getLogger(WorldServer.class);

    private final World world;
    private final ServerNetworkEntityMappings netEntityMappings = new ServerNetworkEntityMappings();
    private final Map<String, IEntityNetworkAdapter> entityNetworkAdapters = new HashMap<>();
    private final HashSet<IEntityNetworkAdapter> pollRequests = new HashSet<>();

    private final Queue<QueuedMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final Map<ChannelHandlerContext, VisitableServerHandler> clientHandlers = new HashMap<>();

    private final IEntityFactory entityFactory;

    private final WorldServerHandler serverHandler = new WorldServerHandler();

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    public WorldServer(IEntityFactory entityFactory, World world, int port) {
        this.world = world;
        this.entityFactory = entityFactory;
        world.getObservers().add(new WorldObserver());

        for (IEntity e : world.getEntities().all()) {
            registerNetworkEntity(e);
        }

        initNetwork(port);
    }

    private void initNetwork(int port) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                serverHandler);
                    }
                });

        // Bind and start to accept incoming connections.
        b.bind(port).channel();

    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    private void send(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if(!channelFuture.isSuccess())
                logger.error("Error transmitting message", channelFuture.cause());
            }
        });
    }

    public void update(int deltaTime) {

        if(messageQueue.size() > 3) {
            System.out.println(messageQueue.size());
        }

        while (!messageQueue.isEmpty()) {
            QueuedMessage msg = messageQueue.remove();

            try {
                VisitableServerHandler visitable = clientHandlers.get(msg.ctx);
                List<IClientVisit> response = msg.visit.visit(visitable, entityNetworkAdapters);

                if(!response.isEmpty()) {
                    for (IClientVisit v : response) {
                        send(msg.ctx, v);
                    }

                    msg.ctx.flush();
                }

            } catch (VisitException ex) {
                logger.error("World visit message failed.", ex);
            }
        }

        NetworkMessageQueue<ClientWorldVisit> delta = new NetworkMessageQueue<>();
        Set<IEntityNetworkAdapter> oldPr = new HashSet(pollRequests);
        pollRequests.clear();
        for(IEntityNetworkAdapter e : oldPr) {
            try {
                for(WorldVisit v : e.pollDelta(deltaTime))
                    delta.queue(new ClientWorldVisit(v));

            } catch (EntityNetworkAdapterException ex) {
                logger.error("Unable to poll delta of entity.", ex);
            }
        }


        ChannelGroup worldClients = serverHandler.getWorldClients();

        while(!delta.isEmpty())
            worldClients.write(delta.poll());

        worldClients.flush();
    }

    private void registerNetworkEntity(IEntity e) {
        EntityConfigurationDetails details = ServerStationEntityFactory.getConfig(e);

        IEntityNetworkAdapterFactory factory = netEntityMappings.get(e.getClass());
        if (factory != null) {
            if(details == null)
                throw new RuntimeException("Synchronized entity has no configuration details.");

            final IEntityNetworkAdapter net = netEntityMappings.get(e.getClass()).create(e, details, new EntityNetworkAdapterHost(e.getInstanceName()));

            entityNetworkAdapters.put(e.getInstanceName(), net);
            pollRequests.add(net);
        }
    }

    private void unregisterNetworkEntity(IEntity e) {
        entityNetworkAdapters.remove(e.getInstanceName());
    }

    private class EntityNetworkAdapterHost implements IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost {
        private final String name;

        public EntityNetworkAdapterHost(String name) {
            this.name = name;
        }

        @Override
        public void poll() {
            pollRequests.add(entityNetworkAdapters.get(name));
        }

        @Override
        public boolean isOwner() {
            return true;
        }
    }

    private class VisitableServerHandler implements IServerWorldHandler{
        long clientTimeDelta = 0;

        private Set<String> ownedEntities = new HashSet<>();

        @Override
        public World getWorld() {
            return world;
        }

        public boolean hasWorld() {
            return world != null;
        }

        @Override
        public IEntityFactory getEntityFactory() {
            return entityFactory;
        }

        @Override
        public void setClientTime(long time) {
            clientTimeDelta = time - System.nanoTime() / 1000000;
        }

        public long getClientTime() {
            return clientTimeDelta + System.nanoTime() / 1000000;
        }

        @Override
        public void authorizeOwnership(String entityName) {
            ownedEntities.add(entityName);
        }

        @Override
        public void unauthorizeOwnership(String entityName) {
            ownedEntities.remove(entityName);
        }

        @Override
        public boolean isOwner(String entityName) {
            //If the ownership has been authorized to this respective client, then I am not the owner
            //with respect to this client.
            return !ownedEntities.contains(entityName);
        }
    }

    private class WorldObserver implements World.IWorldObserver {
        @Override
        public void addedEntity(IEntity e) {
            registerNetworkEntity(e);
        }

        @Override
        public void removedEntity(Vector3F location, IEntity e) {
            unregisterNetworkEntity(e);
        }
    }


    @ChannelHandler.Sharable
    class WorldServerHandler extends SimpleChannelInboundHandler<IServerVisit> {
        private final ChannelGroup worldClients;

        public WorldServerHandler() {
            this.worldClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            clientHandlers.put(ctx, new VisitableServerHandler());
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            clientHandlers.remove(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            worldClients.add(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            worldClients.remove(ctx.channel());
        }

        public ChannelGroup getWorldClients() {
            return worldClients;
        }

        @Override
        protected void messageReceived(ChannelHandlerContext channelHandlerContext, IServerVisit worldVisit) throws Exception {
            messageQueue.add(new QueuedMessage(channelHandlerContext, worldVisit));
        }

    }

    private static class QueuedMessage {
        public ChannelHandlerContext ctx;
        public IServerVisit visit;

        public QueuedMessage(ChannelHandlerContext ctx, IServerVisit visit) {
            this.ctx = ctx;
            this.visit = visit;
        }
    }
}
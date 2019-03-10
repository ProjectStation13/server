package com.projectstation.server.network;

import com.projectstation.network.*;
import com.projectstation.network.command.client.ClientDisconnect;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.RemoveEntityCommand;
import com.projectstation.network.entity.*;
import com.projectstation.server.entity.ServerStationEntityFactory;
import com.projectstation.server.network.entity.ServerNetworkEntityMappings;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.item.IItemFactory;
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

public class WorldServer {

    private static final String SPAWN_CONTROLLER_NAME = "spawnController";

    private static final Logger logger = LoggerFactory.getLogger(WorldServer.class);

    private final World world;
    private final ServerNetworkEntityMappings netEntityMappings = new ServerNetworkEntityMappings();
    private final Map<String, IServerEntityNetworkAdapter> entityNetworkAdapters = new HashMap<>();
    private final HashSet<IServerPollable> entityPollRequests = new HashSet<>();

    private final Queue<QueuedMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final Map<ChannelHandlerContext, VisitableServerHandler> clientHandlers = new HashMap<>();

    private final IEntityFactory entityFactory;
    private final IItemFactory itemFactory;

    private final WorldServerHandler serverHandler = new WorldServerHandler();

    private final int maxPlayers;

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    public WorldServer(IItemFactory itemFactory, IEntityFactory entityFactory, World world, int port, int maxPlayers) {
        this.maxPlayers = maxPlayers;
        this.world = world;
        this.entityFactory = entityFactory;
        this.itemFactory = itemFactory;
        world.getObservers().add(new WorldObserver());

        for (IEntity e : world.getEntities().all()) {
            registerNetworkEntity(e);
        }

        initNetwork(port);
    }

    public World getWorld() {
        return world;
    }

    public VisitableServerHandler getNickname(String nickname) {
        for(VisitableServerHandler h : clientHandlers.values())
        {
            if(h.getNickname().compareTo(nickname) == 0)
                return h;
        }

        return null;
    }

    public int getPlayerCount() {
        return clientHandlers.size();
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
                List<IClientVisit> response = msg.visit.visit(visitable);

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

        NetworkMessageQueue<IClientVisit> delta = new NetworkMessageQueue<>();
        Set<IServerPollable> oldPr = new HashSet(entityPollRequests);
        entityPollRequests.clear();
        for(IServerPollable e : oldPr) {
            try {
                for(IClientVisit v : e.poll(deltaTime))
                    delta.queue(v);

            } catch (NetworkPollException ex) {
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
            if(details == null) {
                logger.info("Synchronized entity " + e.getInstanceName() + " has no configuration details.");
                details = new EntityConfigurationDetails(entityFactory.lookup(e.getClass()));
            }

            final IServerEntityNetworkAdapter net = createNetworkAdapter(e.getClass(), e, details, new EntityNetworkAdapterHost(e.getInstanceName()));

            entityNetworkAdapters.put(e.getInstanceName(), net);
            entityPollRequests.add(net);

            try {
                ChannelGroup clients = serverHandler.getWorldClients();
                for(IClientVisit v : net.createInitializeSteps())
                    clients.write(v);

                clients.flush();
            } catch (EntityNetworkAdapterException ex) {
                logger.error("Unable to send initialize steps for new world entity.", ex);
            }
        } else {
            logger.warn("No synchronization adapter for entity. Not synchronizing entity " + e.getInstanceName());
        }
    }

    private <T extends IEntity> IServerEntityNetworkAdapter createNetworkAdapter(Class<T> cls, Object entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost host) {
        return netEntityMappings.get(cls).create((T)entity, config, host);
    }

    private void unregisterNetworkEntity(IEntity e) {
        IEntityNetworkAdapter adapter = null;
        if(entityNetworkAdapters.containsKey(e.getInstanceName()))
            adapter = entityNetworkAdapters.get(e.getInstanceName());

        if(adapter != null) {
            entityNetworkAdapters.remove(e.getInstanceName());
            entityPollRequests.remove(adapter);
            ChannelGroup clients = serverHandler.getWorldClients();
            clients.writeAndFlush(new ClientWorldVisit(new RemoveEntityCommand(e.getInstanceName())));
        }
    }

    private class EntityNetworkAdapterHost implements IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost {
        private final String name;

        public EntityNetworkAdapterHost(String name) {
            this.name = name;
        }

        @Override
        public void poll() {
            entityPollRequests.add(entityNetworkAdapters.get(name));
        }

        @Override
        public boolean isOwner() {
            return true;
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
            if (clientHandlers.size() >= maxPlayers) {
                ctx.writeAndFlush(new ClientDisconnect("Server is full"));
                ctx.disconnect();
            } else {
                VisitableServerHandler handler = new VisitableServerHandler(entityNetworkAdapters, WorldServer.this, SPAWN_CONTROLLER_NAME, itemFactory, entityFactory, ctx, new IPollRequestHost() {
                    @Override
                    public void poll() {
                        entityPollRequests.add(clientHandlers.get(ctx));
                    }
                });

                clientHandlers.put(ctx, handler);

                entityPollRequests.add(handler);
            }
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
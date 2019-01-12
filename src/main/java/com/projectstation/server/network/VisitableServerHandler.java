package com.projectstation.server.network;

import com.projectstation.network.IClientVisit;
import com.projectstation.network.IPollRequestHost;
import com.projectstation.network.IServerPollable;
import com.projectstation.network.IServerWorldHandler;
import com.projectstation.network.command.client.ClientCharacterAssignment;
import com.projectstation.network.command.client.ClientGiveOwnership;
import com.projectstation.network.entity.IEntityNetworkAdapter;
import com.projectstation.server.entity.ISpawnController;
import com.projectstation.server.entity.ISpawnControllerListener;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;
import com.projectstation.server.network.entity.CharacterNetworkAdapter;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VisitableServerHandler implements IServerWorldHandler, IServerPollable {

    private final Logger logger = LoggerFactory.getLogger(VisitableServerHandler.class);

    private final ChannelHandlerContext ctx;
    long clientTimeDelta = 0;
    private Set<String> ownedEntities = new HashSet<>();
    private IEntity playerEntity = null;
    private final IEntityFactory entityFactory;
    private final IItemFactory itemFactory;
    private final WorldServer world;
    private final IPollRequestHost host;
    private final String spawnControllerName;
    private final Map<String, IServerEntityNetworkAdapter> entityAdapterMapping;
    private boolean requestedPlayerSpawn = false;
    private String nickname = "";

    public VisitableServerHandler(Map<String, IServerEntityNetworkAdapter> entityAdapterMapping, WorldServer world, String spawnControllerName, IItemFactory itemFactory, IEntityFactory entityFactory, ChannelHandlerContext ctx, IPollRequestHost host) {
        this.ctx = ctx;
        this.entityAdapterMapping = entityAdapterMapping;
        this.spawnControllerName = spawnControllerName;
        this.world = world;
        this.entityFactory = entityFactory;
        this.host = host;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean setNickname(String selectedNickname) {
        if(world.getNickname(selectedNickname) != null)
            return false;

        nickname = selectedNickname;
        host.poll();
        return true;
    }

    public String getNickname() {
        return nickname;
    }

    public List<IClientVisit> poll(int deltaTime) {
        List<IClientVisit> response = new ArrayList<>();

        CharacterNetworkAdapter playerAdapter = getPlayerAdapter();
        if(playerAdapter != null) {
            playerAdapter.setNickname(nickname);
        }

        if(playerEntity == null && !requestedPlayerSpawn && nickname.length() > 0) {
            requestedPlayerSpawn = true;
            ISpawnController controller = world.getWorld().getEntities().getByName(ISpawnController.class, spawnControllerName);

            if(controller == null) {
                logger.error("Unable to spawn character, no spawn controller named " + spawnControllerName);
            } else {
                controller.create(new ISpawnControllerListener() {
                    @Override
                    public void spawnedCharacter(IEntity character) {
                        playerEntity = character;
                        host.poll();
                    }
                });
            }
        } else if(playerEntity != null && !ownedEntities.contains(playerEntity.getInstanceName())) {
            authorizeOwnership(playerEntity.getInstanceName());

            //Write to ctx instead of adding to response list.
            //Response list is only for messages we want to broadcast.
            ctx.write(new ClientCharacterAssignment(playerEntity.getInstanceName()));
            ctx.write(new ClientGiveOwnership(playerEntity.getInstanceName()));
            ctx.flush();
        }

        return response;
    }

    @Override
    public World getWorld() {
        return world.getWorld();
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

    @Override
    public IItemFactory getItemFactory() {
        return itemFactory;
    }

    private CharacterNetworkAdapter getPlayerAdapter() {
        if(playerEntity != null)
            return getAdapter(CharacterNetworkAdapter.class, playerEntity.getInstanceName());

        return null;
    }

    @Override
    public <T extends IServerEntityNetworkAdapter> T getAdapter(Class<T> cls, String entityName) {
        IServerEntityNetworkAdapter a = entityAdapterMapping.get(entityName);
        if(a != null && cls.isAssignableFrom(a.getClass()))
            return (T)a;

        return null;
    }

    @Override
    public IServerEntityNetworkAdapter getAdapter(String entityName) {
        return entityAdapterMapping.get(entityName);
    }
}
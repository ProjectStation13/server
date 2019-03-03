package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.ItemDrop;
import com.projectstation.network.IClientVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.CreateItemDropCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;
import com.projectstation.server.item.ServerStationItemFactory;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ItemDropNetworkAdapter implements IServerEntityNetworkAdapter {
    private final Logger logger = LoggerFactory.getLogger(ItemDropNetworkAdapter.class);
    private final ItemDrop entity;
    private boolean locationChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pollRequest;

    public ItemDropNetworkAdapter(ItemDrop entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pr) {
        this.entity = entity;
        this.entity.getBody().getObservers().add(new LocationObserver());
        this.config = config;
        this.pollRequest = pr;
    }

    @Override
    public List<IClientVisit> createInitializeSteps() throws EntityNetworkAdapterException {

        List<IClientVisit> response = new ArrayList<>();
        URI item = ServerStationItemFactory.getConfig(entity.getItem());

        if (item == null)
            logger.error("Unable to find item configuration.");
        else {
            response.add(new ClientWorldVisit(new CreateItemDropCommand(entity.getInstanceName(), entity.getBody().getLocation(), item.toString())));
        }
        return response;
    }

    @Override
    public List<IClientVisit> poll(int deltaTime) {
        List<IClientVisit> response = new ArrayList<>();

        if(locationChanged) {
            response.add(new ClientWorldVisit(new SetEntityPositionCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getDirection())));
            locationChanged = false;
        }

        return response;
    }

    @Override
    public void setSteeringBehaviour(ISteeringBehavior behaviour) {

    }

    @Override
    public void setSpeed(float speed) {

    }

    @Override
    public boolean isOwner() {
        return pollRequest.isOwner();
    }

    private class LocationObserver implements IPhysicsBodyOrientationObserver {
        @Override
        public void locationSet() {
            pollRequest.poll();
            locationChanged = true;
        }

        @Override
        public void directionSet() {
            pollRequest.poll();
            locationChanged = true;
        }
    }
}

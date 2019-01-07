package com.projectstation.server.network.entity;

import com.projectstation.network.IClientVisit;
import com.projectstation.network.WorldVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetDoorStateCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapter;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DoorEntityNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, Door> {

    @Override
    public IServerEntityNetworkAdapter create(Door e, EntityConfigurationDetails config, IEntityNetworlAdapterHost pr) {
        return new DoorEntityNetworkAdapter(e, config, pr);
    }
}

class DoorEntityNetworkAdapter implements IServerEntityNetworkAdapter {
    private final Door entity;
    private boolean locationChanged = true;
    private boolean statusChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pollRequest;

    public DoorEntityNetworkAdapter(Door entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        this.entity = entity;
        this.entity.getBody().getObservers().add(new LocationObserver());
        this.entity.getObservers().add(new DoorStatusObserver());
        this.config = config;
        this.pollRequest = pr;
    }

    @Override
    public List<WorldVisit> createInitializeSteps() throws EntityNetworkAdapterException {
        try {
            List<WorldVisit> response = new ArrayList<>();

            JsonVariable cfg = new JsonVariable();
            config.getAuxConfig().serialize(cfg);
            ByteArrayOutputStream serializedJson = new ByteArrayOutputStream();
            cfg.serialize(serializedJson, false);

            String json = new String(serializedJson.toByteArray());

            response.add(new CreateEntityCommand(entity.getInstanceName(), config.getTypeName(), config.getConfigContext().toString(), json, entity.getBody().getLocation(), entity.getBody().getDirection()));
            response.add(new SetDoorStateCommand(entity.getInstanceName(), entity.isOpen()));
            return response;
        } catch(ValueSerializationException | IOException ex) {
            throw new EntityNetworkAdapterException(ex);
        }
    }

    @Override
    public List<IClientVisit> poll(int deltaTime) {
        List<IClientVisit> response = new ArrayList<>();

        if(locationChanged) {
            response.add(new ClientWorldVisit(new SetEntityPositionCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getDirection())));
            locationChanged = false;
        }

        if(statusChanged) {
            response.add(new ClientWorldVisit(new SetDoorStateCommand(entity.getInstanceName(), entity.isOpen())));
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

    private class DoorStatusObserver implements Door.IDoorObserver {
        @Override
        public void doorStatusChanged() {
            statusChanged = true;
            pollRequest.poll();
        }
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

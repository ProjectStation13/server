package com.projectstation.server.network.entity;

import com.projectstation.network.IClientVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetDoorStateCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DoorEntityNetworkAdapter implements IServerEntityNetworkAdapter {
    private final Door entity;
    private boolean locationChanged = true;
    private boolean statusChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pollRequest;

    public DoorEntityNetworkAdapter(Door entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pr) {
        this.entity = entity;
        this.entity.getBody().getObservers().add(new LocationObserver());
        this.entity.getObservers().add(new DoorStatusObserver());
        this.config = config;
        this.pollRequest = pr;
    }

    @Override
    public List<IClientVisit> createInitializeSteps() throws EntityNetworkAdapterException {
        try {
            List<IClientVisit> response = new ArrayList<>();

            JsonVariable cfg = new JsonVariable();
            config.getAuxConfig().serialize(cfg);
            ByteArrayOutputStream serializedJson = new ByteArrayOutputStream();
            cfg.serialize(serializedJson, false);

            String json = new String(serializedJson.toByteArray());

            response.add(new ClientWorldVisit(new CreateEntityCommand(entity.getInstanceName(), config.getTypeName(), config.getConfigContext().toString(), json, entity.getBody().getLocation(), entity.getBody().getDirection())));
            response.add(new ClientWorldVisit(new SetDoorStateCommand(entity.getInstanceName(), entity.isOpen())));
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

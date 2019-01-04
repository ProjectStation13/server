package com.projectstation.server.network.entity;

import com.projectstation.network.WorldVisit;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.entity.IEntityNetworkAdapter;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleEntityNetworkAdapterFactory implements IEntityNetworkAdapterFactory {

    @Override
    public IEntityNetworkAdapter create(IEntity e, EntityConfigurationDetails config, IEntityNetworlAdapterHost pr) {
        return new SimpleEntityNetworkAdapter(e, config, pr);
    }
}

class SimpleEntityNetworkAdapter implements IEntityNetworkAdapter {
    private final IEntity entity;
    private boolean locationChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pollRequest;

    public SimpleEntityNetworkAdapter(IEntity entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        this.entity = entity;
        this.entity.getBody().getObservers().add(new LocationObserver());
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

            return response;
        } catch(ValueSerializationException | IOException ex) {
            throw new EntityNetworkAdapterException(ex);
        }
    }

    @Override
    public List<WorldVisit> pollDelta(int deltaTime) throws EntityNetworkAdapterException {
        List<WorldVisit> response = new ArrayList<>();

        if(locationChanged) {
            response.add(new SetEntityPositionCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getDirection()));
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

    @Override
    public boolean isOwner() {
        return pollRequest.isOwner();
    }
}

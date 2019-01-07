package com.projectstation.server.network.entity;

import com.projectstation.network.IClientVisit;
import com.projectstation.network.WorldVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.command.world.SetEntityVelocityCommand;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapter;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CharacterNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, IRpgCharacter> {

    @Override
    public IServerEntityNetworkAdapter create(IRpgCharacter e, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        return new CharacterNetworkAdapter(e, config, pr);
    }
}

class CharacterNetworkAdapter implements IServerEntityNetworkAdapter {
    private final IRpgCharacter entity;
    private boolean locationChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost host;

    private static final float VELOCITY_DELTA_RELAY = 0.01f;
    private static final int SYNC_INTERVAL = 150;
    private int lastSync = 0;

    private Vector3F lastLocation = new Vector3F();
    private Vector3F lastVelocity = new Vector3F();

    private final CharacterMovementDirector movementDirector = new CharacterMovementDirector();

    private float m_speed = -1;

    public CharacterNetworkAdapter(IRpgCharacter entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        this.host = pr;
        this.entity = entity;
        this.entity.getBody().getObservers().add(new LocationObserver());
        this.config = config;

        lastLocation = entity.getBody().getLocation();
        lastVelocity = entity.getBody().getLinearVelocity();
        entity.getMovementResolver().queueTop(movementDirector);
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
            response.add(new SetEntityPositionCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getDirection()));
            response.add(new SetEntityVelocityCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getLinearVelocity()));

            return response;
        } catch(ValueSerializationException | IOException ex) {
            throw new EntityNetworkAdapterException(ex);
        }
    }

    @Override
    public List<IClientVisit> poll(int deltaTime) {
        host.poll();
        lastSync += deltaTime;

        Vector3F curLoc = entity.getBody().getLocation();
        Vector3F curVelocity = entity.getBody().getLinearVelocity();

        if(lastSync < SYNC_INTERVAL && curVelocity.difference(lastVelocity).getLength() <= VELOCITY_DELTA_RELAY) {
            return new ArrayList<>();
        }

        lastSync = 0;

        List<IClientVisit> response = new ArrayList<>();

        boolean needUpdate = false;
        if(curLoc.difference(lastLocation).getLength() >= VELOCITY_DELTA_RELAY) {
            needUpdate = true;
        }

        if(curVelocity.difference(lastVelocity).getLength() >= VELOCITY_DELTA_RELAY) {
            needUpdate = true;
        }

        if(needUpdate) {
            lastLocation = new Vector3F(curLoc);
            lastVelocity = new Vector3F(curVelocity);
            response.add(new ClientWorldVisit(new SetEntityVelocityCommand(entity.getInstanceName(), curLoc, curVelocity)));
        }

        return response;
    }

    @Override
    public boolean isOwner() {
        return host.isOwner();
    }

    @Override
    public void setSteeringBehaviour(ISteeringBehavior behaviour) {
        movementDirector.setBehaviour(behaviour);
        entity.getMovementResolver().queueTop(movementDirector);
    }

    @Override
    public void setSpeed(float speed) {
        m_speed = speed;
    }

    public void resetSpeed() {
        setSpeed(-1);
    }

    private class LocationObserver implements IPhysicsBodyOrientationObserver {
        @Override
        public void locationSet() {
            locationChanged = true;
        }

        @Override
        public void directionSet() {
            locationChanged = true;
        }
    }

    private class CharacterMovementDirector implements IMovementResolver.IMovementDirector {
        private ISteeringBehavior behaviour = new ISteeringBehavior.NullSteeringBehavior();

        protected void setBehaviour(ISteeringBehavior behaviour) {
            this.behaviour = behaviour;
        }

        @Override
        public ISteeringBehavior getBehavior() {
            return new ISteeringBehavior() {
                @Override
                public Vector2F direct() {
                    return behaviour.direct();
                }
            };
        }

        @Override
        public float getSpeed() {
            return m_speed;
        }

        @Override
        public boolean isDone() {
            return false;
        }
    }
}

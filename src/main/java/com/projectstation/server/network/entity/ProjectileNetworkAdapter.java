package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.projectile.IProjectile;
import com.jevaengine.spacestation.entity.projectile.LaserProjectile;
import com.projectstation.network.IClientVisit;
import com.projectstation.network.WorldVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.client.GiveEntityNickname;
import com.projectstation.network.command.world.*;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;
import com.projectstation.server.item.ServerStationItemFactory;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.character.IImmutableLoadout;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IImmutableItemStore;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ProjectileNetworkAdapter implements IServerEntityNetworkAdapter {
    private final IProjectile entity;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost host;

    public ProjectileNetworkAdapter(IProjectile entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        this.host = pr;
        this.entity = entity;
        this.config = config;

        entity.getObservers().add(new ProjectileObserver());
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
            String ignore = entity.getIgnore() == null ? null : entity.getIgnore().getInstanceName();
            response.add(new ClientWorldVisit(new CreateProjectileCommand(ignore, entity.getInstanceName(), config.getTypeName(), config.getConfigContext().toString(), json, entity.getBody().getLocation(), entity.getTravelDirection())));
            return response;
        } catch(ValueSerializationException | IOException ex) {
            throw new EntityNetworkAdapterException(ex);
        }
    }

    @Override
    public List<IClientVisit> poll(int deltaTime) {
        ArrayList<IClientVisit> response = new ArrayList<>();
        String ignore = entity.getIgnore() == null ? null : entity.getIgnore().getInstanceName();
        response.add(new ClientWorldVisit(new UpdateProjectileCommand(entity.getInstanceName(), entity.getTravelDirection(), entity.getBody().getLocation(), ignore)));
        return response;
    }

    @Override
    public boolean isOwner() {
        return host.isOwner();
    }

    @Override
    public void setSteeringBehaviour(ISteeringBehavior behaviour) {
    }

    @Override
    public void setSpeed(float speed) {}

    private class ProjectileObserver implements IProjectile.IProjectileObserver {
        @Override
        public void changedDirection() {
            host.poll();
        }

        @Override
        public void changedIgnore() {
            host.poll();
        }
    }
}

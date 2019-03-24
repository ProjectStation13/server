package com.projectstation.server.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.gas.GasSimulation;
import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.FutureResult;
import io.github.jevaengine.IInitializationMonitor;
import io.github.jevaengine.math.*;
import io.github.jevaengine.rpg.entity.character.DefaultRpgCharacter;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.*;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.PhysicsBodyShape;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RandomSpawnController implements ISpawnController {

    private final Logger logger = LoggerFactory.getLogger(RandomSpawnController.class);

    private World world;

    private final String instanceName;

    private HashMap<String, Integer> flags = new HashMap<>();

    private Queue<CreatedEntity> created = new ConcurrentLinkedQueue<>();
    private IParallelEntityFactory entityFactory;

    private final float spawnDepth;

    public RandomSpawnController(String instanceName, IParallelEntityFactory entityFactory, float spawnDepth) {
        this.instanceName = instanceName;
        this.entityFactory = entityFactory;
        this.spawnDepth = spawnDepth;
    }

    @Override
    public void create(ISpawnControllerListener listener, String type, URI config) {
        entityFactory.create(type, null, config, new IInitializationMonitor<IEntity, IEntityFactory.EntityConstructionException>() {
            @Override
            public void completed(FutureResult<IEntity, IEntityFactory.EntityConstructionException> result) {
                try {
                    created.add(new CreatedEntity(result.get(), listener));
                }catch (IEntityFactory.EntityConstructionException e) {
                    logger.error("Unable to spawn character", e);
                }
            }

            @Override
            public void statusChanged(float progress, String status) { }
        });
    }

    private void setEntityLocation(IEntity player) {
        List<IEntity> entities = Arrays.asList(world.getEntities().all());
        Collections.shuffle(entities);
        for(IEntity e : entities) {
            if(!(e instanceof Infrastructure))
                continue;

            if(world.getEffectMap().getTileEffects(e.getBody().getLocation().getXy()).isTraversable(player)) {
                player.getBody().setLocation(new Vector3F(e.getBody().getLocation().getXy(), spawnDepth));
                player.getBody().setDirection(Direction.YPlus);
                return;
            }
        }
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void associate(World world) {
        this.world = world;
    }

    @Override
    public void disassociate() {
        world = null;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public Map<String, Integer> getFlags() {
        return flags;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        return new NullSceneModel();
    }

    @Override
    public IPhysicsBody getBody() {
        return new NonparticipantPhysicsBody();
    }

    @Override
    public IEntityTaskModel getTaskModel() {
        return new NullEntityTaskModel();
    }

    @Override
    public IObserverRegistry getObservers() {
        return new Observers();
    }

    @Override
    public EntityBridge getBridge() {
        return new EntityBridge(this);
    }

    @Override
    public void update(int delta) {
        while(!created.isEmpty()) {
            CreatedEntity e = created.remove();
            world.addEntity(e.entity);
            setEntityLocation(e.entity);

            e.listener.spawnedCharacter(e.entity);
        }
    }

    @Override
    public void dispose() { }

    private static class CreatedEntity {
        final IEntity entity;
        final ISpawnControllerListener listener;

        public CreatedEntity(IEntity entity, ISpawnControllerListener listener) {
            this.entity = entity;
            this.listener = listener;
        }
    }
}

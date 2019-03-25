package com.projectstation.server.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.ui.selectclass.CharacterClassDescription;
import io.github.jevaengine.FutureResult;
import io.github.jevaengine.IInitializationMonitor;
import io.github.jevaengine.math.*;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.*;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CharacterRoleSpawnController implements ISpawnController {

    private final Logger logger = LoggerFactory.getLogger(CharacterRoleSpawnController.class);

    private World world;

    private final String instanceName;

    private HashMap<String, Integer> flags = new HashMap<>();

    private Queue<CreatedEntity> created = new ConcurrentLinkedQueue<>();
    private IParallelEntityFactory entityFactory;

    private final float spawnDepth;

    private final Map<CharacterClassDescription, String> classZoneMapping;
    private final Map<CharacterClassDescription, Integer> roleLimits;
    private final Map<CharacterClassDescription, Integer> roleCount = new HashMap<>();


    public CharacterRoleSpawnController(String instanceName, IParallelEntityFactory entityFactory, float spawnDepth, Map<CharacterClassDescription, String> classZoneMapping, Map<CharacterClassDescription, Integer> roleLimits) {
        this.instanceName = instanceName;
        this.entityFactory = entityFactory;
        this.spawnDepth = spawnDepth;
        this.roleLimits = roleLimits;
        this.classZoneMapping = classZoneMapping;

        for(CharacterClassDescription d : roleLimits.keySet())
            roleCount.put(d, 0);
    }

    public List<CharacterClassDescription> getAvailableRoles() {
        List<CharacterClassDescription> available = new ArrayList<>();

        for(Map.Entry<CharacterClassDescription, Integer> c : roleCount.entrySet()) {
            if(!classZoneMapping.containsKey(c.getKey()))
                continue;

            if(roleLimits.get(c.getKey()) > c.getValue())
                available.add(c.getKey());
        }

        return available;
    }

    @Override
    public void create(ISpawnControllerListener listener, CharacterClassDescription desc) {
        if(roleCount.containsKey(desc))
            roleCount.put(desc, roleCount.get(desc) + 1);

        entityFactory.create("character", null, desc.demo, new IInitializationMonitor<IEntity, IEntityFactory.EntityConstructionException>() {
            @Override
            public void completed(FutureResult<IEntity, IEntityFactory.EntityConstructionException> result) {
                created.add(new CreatedEntity(result, listener, desc));
            }

            @Override
            public void statusChanged(float progress, String status) { }
        });
    }

    private void setEntityLocation(IEntity player, CharacterClassDescription desc) {
        if(!classZoneMapping.containsKey(desc) || !world.getZones().containsKey(classZoneMapping.get(desc))) {
            List<IEntity> entities = Arrays.asList(world.getEntities().all());
            Collections.shuffle(entities);
            for (IEntity e : entities) {
                if (!(e instanceof Infrastructure))
                    continue;

                if (world.getEffectMap().getTileEffects(e.getBody().getLocation().getXy()).isTraversable(player)) {
                    player.getBody().setLocation(new Vector3F(e.getBody().getLocation().getXy(), spawnDepth));
                    player.getBody().setDirection(Direction.YPlus);
                    return;
                }
            }
        } else {
            Rect3F zone = world.getZones().get(classZoneMapping.get(desc));
            float x = zone.x + zone.width * (float)Math.random();
            float y = zone.y + zone.height * (float)Math.random();

            player.getBody().setLocation(new Vector3F(x, y, spawnDepth));
            player.getBody().setDirection(Direction.YPlus);
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

            CreatedEntity entry = created.remove();
            CharacterClassDescription desc = entry.desc;

            try {
                IEntity entity = entry.entity.get();
                world.addEntity(entity);
                setEntityLocation(entity, desc);

                entity.getObservers().add(new IEntityWorldObserver() {
                    @Override
                    public void enterWorld() { }

                    @Override
                    public void leaveWorld() {
                        if(roleCount.containsKey(desc))
                            roleCount.put(desc, roleCount.get(desc) - 1);

                        entity.getObservers().remove(this);
                    }
                });
                entry.listener.spawnedCharacter(entity);
            }catch (IEntityFactory.EntityConstructionException e) {
                logger.error("Unable to spawn character", e);
                entry.listener.spawnError(e);
                if(roleCount.containsKey(desc))
                    roleCount.put(desc, roleCount.get(desc) - 1);
            }
        }
    }

    @Override
    public void dispose() { }

    private static class CreatedEntity {
        final FutureResult<IEntity, IEntityFactory.EntityConstructionException> entity;
        final ISpawnControllerListener listener;
        final CharacterClassDescription desc;

        public CreatedEntity(FutureResult<IEntity, IEntityFactory.EntityConstructionException> entity, ISpawnControllerListener listener, CharacterClassDescription desc) {
            this.entity = entity;
            this.listener = listener;
            this.desc = desc;
        }
    }
}

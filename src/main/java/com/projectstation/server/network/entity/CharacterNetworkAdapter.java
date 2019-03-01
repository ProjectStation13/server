package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.character.ISpaceCharacterStatusObserver;
import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import com.jevaengine.spacestation.entity.character.SpaceCharacterAttribute;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptom;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptomDetails;
import com.jevaengine.spacestation.item.StationItemFactory;
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
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class CharacterNetworkAdapter implements IServerEntityNetworkAdapter {
    private final SpaceCharacter entity;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost host;
    private String nickname = "";
    private boolean nicknameChanged = false;

    private static final float VELOCITY_DELTA_RELAY = 0.01f;
    private static final int SYNC_INTERVAL = 150;
    private int lastSync = 0;

    private Vector3F lastLocation = new Vector3F();
    private Vector3F lastVelocity = new Vector3F();

    private final CharacterMovementDirector movementDirector = new CharacterMovementDirector();

    private float m_speed = -1;

    private final List<WorldVisit> observedChanges = new ArrayList<>();

    public CharacterNetworkAdapter(SpaceCharacter entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        this.host = pr;
        this.entity = entity;
        this.config = config;

        lastLocation = entity.getBody().getLocation();
        lastVelocity = entity.getBody().getLinearVelocity();
        entity.getMovementResolver().queueTop(movementDirector);

        entity.getLoadout().getObservers().add(new LoadoutObserver());
        entity.getInventory().getObservers().add(new InventoryObserver());
        entity.getStatusResolver().getObservers().add(new StatusObserver());
    }

    public void setNickname(String nickname) {
        if(nickname.compareTo(this.nickname) == 0)
            return;

        this.nickname = nickname;
        this.nicknameChanged = true;
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

            response.add(new ClientWorldVisit(new ClearInventoryCommand(entity.getInstanceName())));
            response.add(new ClientWorldVisit(new ClearLoadoutCommand(entity.getInstanceName())));

            IItemSlot inventorySlots[] = entity.getInventory().getSlots();
            for(int i = 0; i < inventorySlots.length; i++) {
                IItemSlot s = inventorySlots[i];
                if(!s.isEmpty()) {
                    IItem itm = s.getItem();
                    URI config = ServerStationItemFactory.getConfig(itm);
                    response.add(new ClientWorldVisit(new AddInventoryItemCommand(entity.getInstanceName(), config.toString(), i)));
                }
            }

            for(IItemSlot s : entity.getLoadout().getSlots()) {
                if(!s.isEmpty()) {
                    IItem i = s.getItem();
                    URI config = ServerStationItemFactory.getConfig(i);
                    response.add(new ClientWorldVisit(new AddLoadoutItemCommand(entity.getInstanceName(), config.toString())));
                }
            }

            if(nickname.length() != 0)
                response.add(new GiveEntityNickname(entity.getInstanceName(), nickname));

            response.add(new ClientWorldVisit(new SetEntityPositionCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getDirection())));
            response.add(new ClientWorldVisit(new SetEntityVelocityCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getLinearVelocity())));

            for(ISymptomDetails s : entity.getStatusResolver().getSymptoms()) {
                response.add(new ClientWorldVisit(new AddSymptomCommand(entity.getInstanceName(), s.getName(), s.getDescription())));
            }

            response.add(new ClientWorldVisit(new SetEffectiveHitpoints(entity.getInstanceName(), entity.getAttributes().get(SpaceCharacterAttribute.EffectiveHitpoints).get())));

            return response;
        } catch(ValueSerializationException | IOException ex) {
            throw new EntityNetworkAdapterException(ex);
        }
    }

    @Override
    public List<IClientVisit> poll(int deltaTime) {
        List<IClientVisit> response = new ArrayList<>();

        for(WorldVisit v : observedChanges) {
            response.add(new ClientWorldVisit(v));
        }

        observedChanges.clear();

        host.poll();
        lastSync += deltaTime;

        if(nicknameChanged) {
            response.add(new GiveEntityNickname(this.entity.getInstanceName(), nickname));
            nicknameChanged = false;
        }

        Vector3F curLoc = entity.getBody().getLocation();
        Vector3F curVelocity = entity.getBody().getLinearVelocity();

        if(lastSync < SYNC_INTERVAL && curVelocity.difference(lastVelocity).getLength() <= VELOCITY_DELTA_RELAY) {
            return response;
        }

        lastSync = 0;

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

    private final class LoadoutObserver implements IImmutableLoadout.ILoadoutObserver {
        @Override
        public void unequip(IItem.IWieldTarget wieldTarget) {
            observedChanges.add(new RemoveLoadoutItemCommand(entity.getInstanceName(), wieldTarget));
        }

        @Override
        public void equip(IItem item, IItem.IWieldTarget wieldTarget) {
            observedChanges.add(new AddLoadoutItemCommand(entity.getInstanceName(), ServerStationItemFactory.getConfig(item).toString()));
        }
    }

    private final class InventoryObserver implements IImmutableItemStore.IItemStoreObserver {
        @Override
        public void addItem(int slotIndex, IItem item) {
            observedChanges.add(new AddInventoryItemCommand(entity.getInstanceName(), ServerStationItemFactory.getConfig(item).toString(), slotIndex));
        }

        @Override
        public void removeItem(int slotIndex, IItem item) {
            observedChanges.add(new RemoveInventoryItemCommand(entity.getInstanceName(), slotIndex));
        }

        @Override
        public void itemAction(int slotIndex, IRpgCharacter accessor, String action) {

        }
    }

    private final class StatusObserver implements ISpaceCharacterStatusObserver {
        @Override
        public void affectedBySymptom(ISymptomDetails symptom) {
            observedChanges.add(new AddSymptomCommand(entity.getInstanceName(), symptom.getName(), symptom.getDescription()));
        }

        @Override
        public void lostSymptom(ISymptomDetails symptom) {
            observedChanges.add(new RemoveSymptomCommand(entity.getInstanceName(), symptom.getName()));
        }

        @Override
        public void died() {

        }

        @Override
        public void revived() {

        }

        @Override
        public void effectiveHitpointsChanged(float newHitpoints) {
            observedChanges.add(new SetEffectiveHitpoints(entity.getInstanceName(), newHitpoints));
        }
    }
}

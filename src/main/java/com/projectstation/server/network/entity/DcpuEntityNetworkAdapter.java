package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.StationEntityFactory;
import com.jevaengine.spacestation.entity.power.Dcpu;
import com.projectstation.network.IClientVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetDoorStateCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.command.world.WriteDcpuVramCommand;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;
import de.codesourcery.jasm16.emulator.devices.impl.DefaultScreen;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DcpuEntityNetworkAdapter implements IServerEntityNetworkAdapter {
    private final Dcpu entity;
    private boolean locationChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pollRequest;

    private final HashMap<Integer, Integer> vramDelta = new HashMap<>();
    private final HashMap<Integer, Integer> paletteDelta = new HashMap<>();
    private final HashMap<Integer, Integer> fontDelta = new HashMap<>();

    public DcpuEntityNetworkAdapter(Dcpu entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pr) {
        this.entity = entity;
        this.entity.getBody().getObservers().add(new LocationObserver());
        this.entity.getObservers().add(new DcpuObserver());
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

            DefaultScreen.VideoRAM ram = entity.getScreenDevice().getVram();

            if(ram != null) {
                HashMap<Integer, Integer> vram = new HashMap<>();

                for (int i = 0; i < ram.getSize().getSizeInWords(); i++)
                    vram.put(i, ram.read(i));

                response.add(new ClientWorldVisit(new WriteDcpuVramCommand(entity.getInstanceName(), vram)));
            }

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

        response.add(new ClientWorldVisit(new WriteDcpuVramCommand(entity.getInstanceName(), vramDelta)));
        vramDelta.clear();
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

    private class DcpuObserver implements Dcpu.IDcpuObserver {

        @Override
        public void screenChanged(HashMap<Integer, Integer> vramDelta, HashMap<Integer, Integer> paletteDelta, HashMap<Integer, Integer> fontDelta) {
            DcpuEntityNetworkAdapter.this.vramDelta.putAll(vramDelta);
            DcpuEntityNetworkAdapter.this.paletteDelta.putAll(paletteDelta);
            DcpuEntityNetworkAdapter.this.fontDelta.putAll(fontDelta);
            pollRequest.poll();
        }

        @Override
        public void keySimulated(int keyCode, char keyChar) {

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

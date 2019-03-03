package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.entity.InteractableDoor;
import com.jevaengine.spacestation.entity.ItemDrop;
import com.jevaengine.spacestation.entity.atmos.*;
import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import com.jevaengine.spacestation.entity.network.*;
import com.jevaengine.spacestation.entity.power.*;
import com.jevaengine.spacestation.entity.projectile.LaserProjectile;
import com.projectstation.network.entity.EntityNetworkAdapterMapping;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;
import io.github.jevaengine.rpg.entity.character.DefaultRpgCharacter;
import io.github.jevaengine.world.entity.SceneArtifact;

public class ServerNetworkEntityMappings extends EntityNetworkAdapterMapping<IServerEntityNetworkAdapter> {
    public ServerNetworkEntityMappings() {

        register(Infrastructure.class, new SimpleEntityNetworkAdapterFactory<>((inf) ->
                    inf.isTraversable()
                ));

        register(SpaceCharacter.class, new CharacterNetworkAdapterFactory());
        register(InteractableDoor.class, new DoorEntityNetworkAdapterFactory());
        register(ItemDrop.class, new ItemDropNetworkAdapterFactory());
        register(LaserProjectile.class, new ProjectileNetworkAdapterFactory());

        register(NetworkWire.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(PowerWire.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));

        register(LiquidPipe.class, new SimpleEntityNetworkAdapterFactory(
                (inf) -> true
        ));


        register(NetworkToggleControl.class, new SimpleEntityNetworkAdapterFactory(
                (inf) -> true
        ));

        register(SceneArtifact.class, new SimpleEntityNetworkAdapterFactory<>( (inf) -> inf.isTraversable()));

        register(NetworkAirQualitySensor.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(PowerSwitch.class, new SimpleEntityNetworkAdapterFactory( (inf) -> false));
        register(Diode.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(NetworkDoorController.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(GasVent.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(GasEngine.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(FuelChamber.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(LiquidTank.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(LiquidPump.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(PressureCollapseValve.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(NetworkPowerMeter.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(Capacitor.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
        register(Alternator.class, new SimpleEntityNetworkAdapterFactory( (inf) -> true));
    }
}

package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.entity.InteractableDoor;
import com.jevaengine.spacestation.entity.ItemDrop;
import com.jevaengine.spacestation.entity.atmos.LiquidPipe;
import com.jevaengine.spacestation.entity.network.NetworkToggleControl;
import com.jevaengine.spacestation.entity.network.NetworkWire;
import com.jevaengine.spacestation.entity.power.PowerWire;
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

        register(DefaultRpgCharacter.class, new CharacterNetworkAdapterFactory());
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
    }
}

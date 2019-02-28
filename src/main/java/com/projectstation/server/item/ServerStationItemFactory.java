package com.projectstation.server.item;

import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;

import java.net.URI;
import java.util.WeakHashMap;

public class ServerStationItemFactory implements IItemFactory {
    private static final WeakHashMap<IItem, URI> configMapping = new WeakHashMap<>();

    private final IItemFactory base;

    public ServerStationItemFactory(IItemFactory base) {
        this.base = base;
    }

    public static URI getConfig(IItem item) {
        synchronized (configMapping) {

            if(!configMapping.containsKey(item))
                throw new RuntimeException("Unidentified item.");

            return configMapping.get(item);
        }
    }

    @Override
    public IItem create(URI name) throws ItemContructionException {
        IItem i = this.base.create(name);

        synchronized (configMapping) {
            configMapping.put(i, name);
        }

        return i;
    }
}

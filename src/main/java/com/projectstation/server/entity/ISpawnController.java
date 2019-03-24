package com.projectstation.server.entity;

import io.github.jevaengine.world.entity.IEntity;

import java.net.URI;

public interface ISpawnController extends IEntity {
    void create(ISpawnControllerListener listener, String type, URI config);
}

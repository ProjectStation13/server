package com.projectstation.server.entity;

import com.jevaengine.spacestation.ui.selectclass.CharacterClassDescription;
import io.github.jevaengine.world.entity.IEntity;

import java.net.URI;
import java.util.List;

public interface ISpawnController extends IEntity {
    void create(ISpawnControllerListener listener, CharacterClassDescription description);
    List<CharacterClassDescription> getAvailableRoles();
}

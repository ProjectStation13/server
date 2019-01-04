/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.projectstation.server.gamestates;

import com.jevaengine.spacestation.IState;
import com.jevaengine.spacestation.IStateContext;
import com.jevaengine.spacestation.StationProjectionFactory;
import com.jevaengine.spacestation.gamestates.MainMenu;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import com.jevaengine.spacestation.ui.GasDebugFactory;
import com.jevaengine.spacestation.ui.HudFactory;
import com.jevaengine.spacestation.ui.HudFactory.Hud;
import com.jevaengine.spacestation.ui.InventoryHudFactory;
import com.jevaengine.spacestation.ui.InventoryHudFactory.InventoryHud;
import com.jevaengine.spacestation.ui.LoadoutHudFactory;
import com.jevaengine.spacestation.ui.LoadoutHudFactory.LoadoutHud;
import com.jevaengine.spacestation.ui.playing.PlayingWindowFactory;
import com.jevaengine.spacestation.ui.playing.PlayingWindowFactory.PlayingWindow;
import com.projectstation.server.network.WorldServer;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter.NullRpgCharacter;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.camera.FollowCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class SimulateServerWorld implements IState {

	private static final int PORT = 7345;

	private IStateContext context;
	private final World world;

	private final IWindowFactory windowFactory;
	private final IParallelWorldFactory worldFactory;
	private final IAudioClipFactory audioClipFactory;
	private final ISpriteFactory spriteFactory;
	private final Logger logger = LoggerFactory.getLogger(SimulateServerWorld.class);

	private final WorldServer worldServer;

	public SimulateServerWorld(IEntityFactory entityFactory, IWindowFactory windowFactory, IParallelWorldFactory worldFactory, IAudioClipFactory audioClipFactory, ISpriteFactory spriteFactory, World world) {
		this.windowFactory = windowFactory;
		this.worldFactory = worldFactory;
		this.audioClipFactory = audioClipFactory;
		this.spriteFactory = spriteFactory;
		this.world = world;

		this.worldServer = new WorldServer(entityFactory, world, PORT);
	}

	@Override
	public void enter(IStateContext context) {
		this.context = context;

	}

	@Override
	public void leave() {
	}

	@Override
	public void update(int deltaTime) {
		worldServer.update(deltaTime);
		world.update(deltaTime);
	}
}

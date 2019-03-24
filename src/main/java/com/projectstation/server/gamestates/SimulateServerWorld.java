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
import com.jevaengine.spacestation.ui.selectclass.CharacterClassDescription;
import com.projectstation.server.MasterServerCommunicator;
import com.projectstation.server.ServerConfig;
import com.projectstation.server.network.WorldServer;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class SimulateServerWorld implements IState {
	private static final URI CHARACTER_CLASSES = URI.create("file:///characterClasses.json");

	private IStateContext context;
	private final World world;
	private final Logger logger = LoggerFactory.getLogger(SimulateServerWorld.class);

	private MasterServerCommunicator masterCommunicator;
	private WorldServer worldServer;
	private final ServerConfig config;

	public SimulateServerWorld(ServerConfig config, World world) {
		this.world = world;
		this.config = config;
	}

	private List<CharacterClassDescription> loadClassDescriptions() {
		try {
			return Arrays.asList(context.getConfigFactory().create(CHARACTER_CLASSES).getValues(CharacterClassDescription[].class));
		} catch (ValueSerializationException | IConfigurationFactory.ConfigurationConstructionException e) {
			logger.error("Error get available classes.", e);
			return new ArrayList<>();
		}
	}

	@Override
	public void enter(IStateContext context) {
		this.context = context;
		this.worldServer = new WorldServer(context.getItemFactory(), context.getEntityFactory(), world, config.port, config.maxPlayers, loadClassDescriptions());
		masterCommunicator = new MasterServerCommunicator(config.masterHost, config.masterPort, config.port, config.name, config.description, config.maxPlayers);
	}

	@Override
	public void leave() {
		if(masterCommunicator != null)
			masterCommunicator.stop();
	}

	@Override
	public void update(int deltaTime) {
		worldServer.update(deltaTime);
		world.update(deltaTime);

		masterCommunicator.setPlayerCount(worldServer.getPlayerCount());
	}
}

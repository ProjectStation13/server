package com.projectstation.server.network;

import com.projectstation.network.IClientVisit;
import com.projectstation.network.NetworkPollException;

import java.util.List;

public interface IServerPollable {
    List<IClientVisit> poll(int deltaTime) throws NetworkPollException;
}

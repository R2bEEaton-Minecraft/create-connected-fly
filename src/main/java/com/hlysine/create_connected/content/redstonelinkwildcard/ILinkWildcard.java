package com.hlysine.create_connected.content.redstonelinkwildcard;

import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.zurrtum.create.catnip.data.Couple;

public interface ILinkWildcard {
    boolean test(RedstoneLinkNetworkHandler.Frequency stack);
}

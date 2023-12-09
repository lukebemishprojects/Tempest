package dev.lukebemish.tempest.impl.forge.client;

import dev.lukebemish.tempest.impl.client.OverlaySpriteListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientEntrypoint {
    public static void init(IEventBus modBus) {
        modBus.addListener(ClientEntrypoint::addReloadListener);
    }

    private static void addReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new OverlaySpriteListener());
    }
}

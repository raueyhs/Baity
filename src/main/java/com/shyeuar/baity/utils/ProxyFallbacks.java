package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ProxyFallbacks {

    private static final int[] PROBE_PORTS = new int[]{7890, 7891, 7892};

    private ProxyFallbacks() {}

    public static List<Proxy> proxies() {
        List<Proxy> proxies = new ArrayList<>();
        String host = ConfigManager.baityPresenceProxyHost == null ? "" : ConfigManager.baityPresenceProxyHost.trim();
        if (!host.isEmpty() && ConfigManager.baityPresenceProxyPort > 0) {
            proxies.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, ConfigManager.baityPresenceProxyPort)));
        }
        for (int port : PROBE_PORTS) {
            proxies.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port)));
        }
        return proxies;
    }
}

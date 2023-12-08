package dev.lukebemish.tempest.impl.data.world;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class LevelIdMap {
    public static LevelIdMap CURRENT;

    private final Object2IntMap<ResourceKey<Level>> map;
    private final List<ResourceKey<Level>> keys;

    private LevelIdMap(List<ResourceKey<Level>> keys) {
        this.map = new Object2IntOpenHashMap<>();
        this.keys = keys;
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), i);
        }
    }

    public void encoder(FriendlyByteBuf buffer) {
        buffer.writeInt(keys.size());
        for (ResourceKey<Level> key : keys) {
            buffer.writeInt(map.getInt(key));
            buffer.writeResourceKey(key);
        }
    }

    public static LevelIdMap decoder(FriendlyByteBuf buffer) {
        int l = buffer.readInt();
        List<ResourceKey<Level>> keys = new ArrayList<>(l);
        for (int i = 0; i < l; i++) {
            ResourceKey<Level> key = buffer.readResourceKey(Registries.DIMENSION);
            keys.add(key);
        }
        return new LevelIdMap(keys);
    }

    public static void recieve(LevelIdMap map) {
        CURRENT = map;
    }

    public static LevelIdMap send(RegistryAccess access) {
        List<ResourceKey<Level>> keys = new ArrayList<>(access.registryOrThrow(Registries.DIMENSION).registryKeySet());
        LevelIdMap levelIdMap = new LevelIdMap(keys);
        CURRENT = levelIdMap;
        return levelIdMap;
    }

    public int id(ResourceKey<Level> key) {
        return map.getInt(key);
    }

    public ResourceKey<Level> level(int id) {
        return keys.get(id);
    }
}

package dev.lukebemish.tempest.impl.forge;

import dev.lukebemish.tempest.impl.FastChunkLookup;
import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

record WeatherDataProvider(WeatherChunkData data,
                           LazyOptional<WeatherChunkData> lazy) implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(WeatherChunkData.class);
    }

    static void attachCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        var holder = (FastChunkLookup) event.getObject();
        final WeatherChunkData data;
        if (holder.tempest$getChunkData() == null) {
            data = new WeatherChunkData(event.getObject());
            holder.tempest$setChunkData(data);
        } else {
            data = holder.tempest$getChunkData();
        }
        final LazyOptional<WeatherChunkData> lazy = LazyOptional.of(() -> data);
        event.addCapability(ModPlatform.WEATHER_CHUNK_DATA_LOCATION, new WeatherDataProvider(data, lazy));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction arg) {
        if (capability == ModPlatform.WEATHER_CHUNK_DATA) {
            return lazy.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag arg) {
        data.load(arg);
    }
}

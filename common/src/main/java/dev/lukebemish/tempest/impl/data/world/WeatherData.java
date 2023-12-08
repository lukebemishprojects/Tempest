package dev.lukebemish.tempest.impl.data.world;

import net.minecraft.nbt.CompoundTag;

public class WeatherData {
    private final WeatherChunkData intrusive;

    // 0xF: black ice
    private int data;

    private final int pos;

    WeatherData(WeatherChunkData intrusive, int pos) {
        this.intrusive = intrusive;
        this.pos = pos;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("data", data);
        return tag;
    }

    public void load(CompoundTag tag) {
        data = tag.getInt("data");
    }

    private void update() {
        if (boring()) {
            intrusive.data.remove(pos);
        } else {
            intrusive.update(pos, data);
        }
    }

    public void blackIce(byte blackIce) {
        this.data = (data & (~0xF)) | (blackIce & 0xF);
        update();
    }

    public byte blackIce() {
        return (byte) (data & 0xF);
    }

    int data() {
        return data;
    }

    public boolean boring() {
        return data == 0;
    }
}

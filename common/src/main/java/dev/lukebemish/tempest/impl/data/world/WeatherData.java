package dev.lukebemish.tempest.impl.data.world;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

public sealed interface WeatherData {
    void blackIce(int blackIce);
    int blackIce();

    void data(int value);

    final class Reference implements WeatherData {
        private final WeatherChunkData intrusive;
        private final int pos;
        @Nullable
        private Concrete concrete;
        private final IntFunction<Concrete> factory;

        Reference(WeatherChunkData intrusive, int pos) {
            this.intrusive = intrusive;
            this.pos = pos;
            this.factory = key -> new WeatherData.Concrete(intrusive, key);
        }

        @Override
        public void blackIce(int blackIce) {
            if (this.concrete == null) {
                this.concrete = intrusive.data.computeIfAbsent(pos, factory);
            }
            concrete.blackIce(blackIce);
        }

        @Override
        public int blackIce() {
            if (this.concrete != null) {
                return this.concrete.blackIce();
            }
            var concrete = intrusive.data.getOrDefault(pos, null);
            if (concrete == null) {
                return 0;
            } else {
                this.concrete = concrete;
                return concrete.blackIce();
            }
        }

        @Override
        public void data(int value) {
            if (this.concrete == null) {
                this.concrete = intrusive.data.computeIfAbsent(pos, factory);
            }
            concrete.data(value);
        }
    }

    final class Concrete implements WeatherData {
        private final WeatherChunkData intrusive;

        // 0xF: black ice
        private int data;

        private final int pos;

        Concrete(WeatherChunkData intrusive, int pos) {
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

        @Override
        public void blackIce(int blackIce) {
            this.data = (data & (~0xF)) | (blackIce & 0xF);
            update();
        }

        @Override
        public int blackIce() {
            return data & 0xF;
        }

        @Override
        public void data(int value) {
            this.data = value;
            update();
        }

        int data() {
            return data;
        }

        public boolean boring() {
            return data == 0;
        }
    }
}

package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

public sealed interface WeatherData {
    void blackIce(int blackIce);

    default void levelBlackIce(ServerLevel level, BlockPos pos, int blackIce) {
        blackIce(blackIce);
        var state = level.getBlockState(pos);
        frozenUp(blackIce > 4 && state.is(Constants.FREEZES_UP));
    }

    int blackIce();

    void data(int value);

    void frozenUp(boolean stuck);
    boolean frozenUp();

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

        @Override
        public void frozenUp(boolean frozenUp) {
            if (this.concrete == null) {
                this.concrete = intrusive.data.computeIfAbsent(pos, factory);
            }
            concrete.frozenUp(frozenUp);
        }

        @Override
        public boolean frozenUp() {
            if (this.concrete != null) {
                return this.concrete.frozenUp();
            }
            var concrete = intrusive.data.getOrDefault(pos, null);
            if (concrete == null) {
                return false;
            } else {
                this.concrete = concrete;
                return concrete.frozenUp();
            }
        }
    }

    final class Concrete implements WeatherData {
        private final WeatherChunkData intrusive;

        // 0b0XXXX: black ice
        // 0bX0000: frozen up
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
                intrusive.update(pos, 0);
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

        @Override
        public void frozenUp(boolean stuck) {
            if (stuck) {
                data |= 0b10000;
            } else {
                data &= ~0b10000;
            }
            update();
        }

        @Override
        public boolean frozenUp() {
            return (data & 0b10000) != 0;
        }

        int data() {
            return data;
        }

        public boolean boring() {
            return data == 0;
        }
    }
}

package dev.lukebemish.tempest.impl;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.tempest.api.WeatherStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record TempestWeatherCheck(Optional<WeatherStatus.Kind> kind, Optional<Range> intensity, Optional<Range> temperature, Optional<Boolean> thunder) implements LootItemCondition {
    public static final Codec<TempestWeatherCheck> CODEC = RecordCodecBuilder.create(i -> i.group(
        WeatherStatus.Kind.CODEC.optionalFieldOf("kind").forGetter(TempestWeatherCheck::kind),
        Range.codecFor(Optional.of(0f), Optional.of(1f)).optionalFieldOf("intensity").forGetter(TempestWeatherCheck::intensity),
        Range.codecFor(Optional.of(-1f), Optional.of(1f)).optionalFieldOf("temperature").forGetter(TempestWeatherCheck::temperature),
        Codec.BOOL.optionalFieldOf("thunder").forGetter(TempestWeatherCheck::thunder)
    ).apply(i, TempestWeatherCheck::new));

    public static final LootItemConditionType TYPE = new LootItemConditionType(new Serializer<TempestWeatherCheck>() {
        @Override
        public void serialize(JsonObject json, TempestWeatherCheck value, JsonSerializationContext serializationContext) {
            JsonElement out = CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow(false, e -> {
                throw new JsonSyntaxException(e);
            });
            if (!out.isJsonObject()) {
                throw new JsonSyntaxException("Expected object, got " + out);
            }
            for (var entry : out.getAsJsonObject().entrySet()) {
                json.add(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public @NotNull TempestWeatherCheck deserialize(JsonObject json, JsonDeserializationContext serializationContext) {
            return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e -> {
                throw new JsonSyntaxException(e);
            });
        }
    });

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public boolean test(LootContext lootContext) {
        var level = lootContext.getLevel();
        var pos = lootContext.getParamOrNull(LootContextParams.ORIGIN);
        if (pos == null) {
            return false;
        }
        var blockPos = new BlockPos((int) Math.round(pos.x()), (int) Math.round(pos.y()), (int) Math.round(pos.z()));
        var weather = WeatherStatus.atPosition(level, blockPos);
        if (weather == null) {
            return false;
        }
        if (kind.isPresent() && weather.kind() != kind.get()) {
            return false;
        }
        if (intensity.isPresent() && !intensity.get().contains(weather.intensity())) {
            return false;
        }
        if (temperature.isPresent() && !temperature.get().contains(weather.temperature())) {
            return false;
        }
        if (thunder.isPresent() && weather.thunder() != thunder.get()) {
            return false;
        }
        return true;
    }

    public record Range(float start, float stop) {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public static Codec<Range> codecFor(Optional<Float> min, Optional<Float> max) {
            return Codec.FLOAT.listOf().flatXmap(
                list -> {
                    if (list.size() != 2) {
                        return DataResult.error(() -> "Expected 2 elements, got " + list.size());
                    }
                    if (min.isPresent() && list.get(0) < min.get()) {
                        return DataResult.error(() -> "Minimum value is " + min.get() + ", got " + list.get(0));
                    }
                    if (max.isPresent() && list.get(1) > max.get()) {
                        return DataResult.error(() -> "Maximum value is " + max.get() + ", got " + list.get(1));
                    }
                    return DataResult.success(new Range(list.get(0), list.get(1)));
                }, range -> DataResult.success(List.of(range.start, range.stop))
            );
        }

        public boolean contains(float intensity) {
            return intensity >= start && intensity <= stop;
        }
    }
}

package dev.lukebemish.tempest.impl.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class QuadHelper {
    public static final int STRIDE = DefaultVertexFormat.BLOCK.getIntegerSize();

    private QuadHelper() {}

    private static float directionU(float px, float py, float pz, Direction direction) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return Mth.clamp(px, 0, 1);
        }
        return Mth.clamp(py, 0, 1);
    }

    private static float directionV(float px, float py, float pz, Direction direction) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return Mth.clamp(pz, 0, 1);
        } else if (direction == Direction.EAST || direction == Direction.WEST) {
            return Mth.clamp(pz, 0, 1);
        }
        return Mth.clamp(px, 0, 1);
    }

    public static void processQuad(BlockState state, Matrix4f pose, BakedQuad quad, BlockAndTintGetter level, BlockPos posUp, TextureAtlasSprite sprite, VertexConsumer translucentBuilder) {
        int[] vertexData = quad.getVertices();
        for (int vert = 0; vert < 4; vert++) {
            float px = packedPos(vert, 0, vertexData);
            float py = packedPos(vert, 1, vertexData);
            float pz = packedPos(vert, 2, vertexData);
            Vector4f vector4f = pose.transform(new Vector4f(px, py, pz, 1.0F));
            int light = LevelRenderer.getLightColor(level, state, posUp);
            float u = sprite.getU(directionU(px, py, pz, quad.getDirection()) * 16);
            float v = sprite.getV(directionV(px, py, pz, quad.getDirection()) * 16);
            float nx = packedNormal(vert, 0, vertexData);
            float ny = packedNormal(vert, 1, vertexData);
            float nz = packedNormal(vert, 2, vertexData);
            translucentBuilder.vertex(vector4f.x, vector4f.y, vector4f.z, 1, 1, 1, 1, u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        }
    }

    private static float packedPos(int vert, int idx, int[] vertexData) {
        int offset = vert * STRIDE + findOffset(DefaultVertexFormat.ELEMENT_POSITION);
        return Float.intBitsToFloat(vertexData[offset + idx]);
    }

    private static float packedNormal(int vert, int idx, int[] vertexData) {
        int offset = vert * STRIDE + findOffset(DefaultVertexFormat.ELEMENT_NORMAL);
        int packedNormal = vertexData[offset];
        return ((byte) ((packedNormal >> (8 * idx)) & 0xFF)) / 127f;
    }

    private static int findOffset(VertexFormatElement element) {
        var index = DefaultVertexFormat.BLOCK.getElements().indexOf(element);
        return index < 0 ? -1 : ((VertexFormatWrapper) DefaultVertexFormat.BLOCK).tempest$getOffset(index) / 4;
    }

    public static void renderOverlayQuads(BlockState state, BlockPos pos, Matrix4f pose, Function<Direction, List<BakedQuad>> quadProvider, boolean frozenUp, BlockAndTintGetter level, TextureAtlasSprite sprite, VertexConsumer translucentBuilder) {
        List<BakedQuad> quads = new ArrayList<>(quadProvider.apply(null));
        if (frozenUp) {
            for (var dir : Direction.values()) {
                quads.addAll(quadProvider.apply(dir));
            }
            for (var quad : quads) {
                processQuad(state, pose, quad, level, pos.offset(quad.getDirection().getNormal()), sprite, translucentBuilder);
            }
        } else {
            quads.addAll(quadProvider.apply(Direction.UP));
            var posUp = pos.above();
            for (var quad : quads) {
                if (quad.getDirection() == Direction.UP) {
                    processQuad(state, pose, quad, level, posUp, sprite, translucentBuilder);
                }
            }
        }
    }
}

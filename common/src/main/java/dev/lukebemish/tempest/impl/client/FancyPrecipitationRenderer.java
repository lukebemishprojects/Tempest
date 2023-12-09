package dev.lukebemish.tempest.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;

public class FancyPrecipitationRenderer {
    private final float[] rainSizeX;
    private final float[] rainSizeZ;

    public FancyPrecipitationRenderer(float[] rainSizeX, float[] rainSizeZ) {
        this.rainSizeX = rainSizeX;
        this.rainSizeZ = rainSizeZ;
    }

    public void render(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        lightTexture.turnOnLightLayer();
        Level level = minecraft.level;
        int floorX = Mth.floor(camX);
        int floorY = Mth.floor(camY);
        int floorZ = Mth.floor(camZ);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        int layers = 5;
        if (Minecraft.useFancyGraphics()) {
            layers = 10;
        }

        int rendering = -1;

        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        float time = (float) ticks + partialTick;
        RenderSystem.setShader(GameRenderer::getParticleShader);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for(int z = floorZ - layers; z <= floorZ + layers; ++z) {
            for (int x = floorX - layers; x <= floorX + layers; ++x) {
                int sizeIdx = (z - floorZ + 16) * 32 + x - floorX + 16;
                float sizeX = this.rainSizeX[sizeIdx];
                float sizeZ = this.rainSizeZ[sizeIdx];
                mutableBlockPos.set(x, camY, z);
                //noinspection DataFlowIssue
                var chunk = level.getChunkAt(mutableBlockPos);
                var data = Services.PLATFORM.getChunkData(chunk);
                var status = data.getWeatherStatus(mutableBlockPos);
                if (data.known() && status != null) {
                    float precipLevel = status.intensity;
                    int lowerY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    int minY = Math.max(floorY - layers, lowerY);
                    int maxY = Math.max(floorY + layers, lowerY);

                    int upperY = Math.max(floorY, lowerY);

                    if (minY != maxY) {
                        // we actually have somewhere to render
                        // 3121: prime
                        // 45238971: 3, 15079657
                        // 418711: 433, 967
                        // 13761: 3^2, 11, 139
                        int hash = x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
                        RandomSource randomsource = RandomSource.create(hash);
                        mutableBlockPos.set(x, minY, z);

                        if (rendering != status.category.ordinal()) {
                            if (rendering >= 0) {
                                tesselator.end();
                            }

                            rendering = status.category.ordinal();
                            RenderSystem.setShaderTexture(0, status.category.location);
                            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        float slowVerticalOffset = -((ticks & (512 - 1)) + partialTick) / 512.0F;
                        float randomizingOffset = (float) (randomsource.nextDouble() + (time * randomsource.nextGaussian()) * 0.001);
                        int offsetTicks = ticks + hash & 31;
                        float fastVerticalOffset = -(offsetTicks + partialTick) / 32.0F * (3.0F + randomsource.nextFloat());
                        float verticalOffset = (status.category.fastFalling ? fastVerticalOffset : slowVerticalOffset) + randomizingOffset;
                        float swirlMovement = (float) (randomsource.nextDouble() + time * 0.01 * randomsource.nextGaussian());
                        double xOff = x + 0.5 - camX;
                        double zOff = z + 0.5 - camZ;

                        Matrix4f quadRotate = new Matrix4f();
                        quadRotate.translate((float) (x - camX + 0.5), (float) (minY - camY), (float) (z - camZ + 0.5));
                        quadRotate.rotate(-status.speed, -status.windZ, 0, status.windX);
                        quadRotate.translate((float) -(x - camX + 0.5), (float) -(minY - camY), (float) -(z - camZ + 0.5));

                        float movement = swirlMovement * status.swirl;
                        float f8 = (float)Math.sqrt(xOff * xOff + zOff * zOff) / layers;
                        float f9 = ((1.0F - f8 * f8) * 0.3F + 0.5F) * precipLevel;
                        mutableBlockPos.set(x, upperY, z);
                        int packedLight = LevelRenderer.getLightColor(level, mutableBlockPos);

                        bufferbuilder.vertex(quadRotate, (float) (x - camX - sizeX + 0.5), (float) (maxY - camY), (float) (z - camZ - sizeZ + 0.5))
                            .uv(0.0F + movement, minY * 0.25F + verticalOffset)
                            .color(1.0F, 1.0F, 1.0F, f9)
                            .uv2(packedLight)
                            .endVertex();
                        bufferbuilder.vertex(quadRotate, (float) (x - camX + sizeX + 0.5), (float) (maxY - camY), (float) (z - camZ + sizeZ + 0.5))
                            .uv(1.0F + movement, minY * 0.25F + verticalOffset)
                            .color(1.0F, 1.0F, 1.0F, f9)
                            .uv2(packedLight)
                            .endVertex();
                        bufferbuilder.vertex(quadRotate, (float) (x - camX + sizeX + 0.5), (float) (minY - camY), (float) (z - camZ + sizeZ + 0.5))
                            .uv(1.0F + movement, maxY * 0.25F + verticalOffset)
                            .color(1.0F, 1.0F, 1.0F, f9)
                            .uv2(packedLight)
                            .endVertex();
                        bufferbuilder.vertex(quadRotate, (float) (x - camX - sizeX + 0.5), (float) (minY - camY), (float) (z - camZ - sizeZ + 0.5))
                            .uv(0.0F + movement, maxY * 0.25F + verticalOffset)
                            .color(1.0F, 1.0F, 1.0F, f9)
                            .uv2(packedLight)
                            .endVertex();
                    }
                }
            }
        }
        if (rendering >= 0) {
            tesselator.end();
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }
}

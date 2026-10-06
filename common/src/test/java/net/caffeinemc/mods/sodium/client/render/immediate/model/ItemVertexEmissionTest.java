package net.caffeinemc.mods.sodium.client.render.immediate.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer.VertexConsumerMixin;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.system.MemoryUtil;

import static net.caffeinemc.mods.sodium.client.render.immediate.model.ItemTextureCoordinatesTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemVertexEmissionTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void optimizedAndFallbackEmissionAgreeWithoutChangingSharedQuad(boolean brightnessTable) {
        var baked = mock(BakedQuad.class, withSettings().extraInterfaces(ModelQuadView.class));
        var quad = (ModelQuadView) baked;
        var sprite = sprite();
        when(baked.getSprite()).thenReturn(sprite);
        // getSprite is shared by the Minecraft class and the ModelQuadView interface.
        setUvs(quad);
        int[] original = new int[32];
        for (int i = 0; i < original.length; i++) {
            original[i] = i + 1;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            original[vertex * 8 + 4] = Float.floatToRawIntBits(quad.getTexU(vertex));
            original[vertex * 8 + 5] = Float.floatToRawIntBits(quad.getTexV(vertex));
        }
        int[] before = original.clone();
        when(baked.getVertices()).thenReturn(original);
        VertexConsumerMixin fallback = new VertexConsumerMixin() {};
        assertSame(original, fallback.sodium$insetItemTextureCoordinates(original, baked));
        int[] terrainOutput = emit(quad, brightnessTable);
        int[] itemOutput;
        int[] fallbackData;
        boolean previous = ItemRenderState.enter();
        try {
            itemOutput = emit(quad, brightnessTable);
            fallbackData = fallback.sodium$insetItemTextureCoordinates(original, baked);
        } finally {
            ItemRenderState.restore(previous);
        }
        assertNotSame(original, fallbackData);
        assertArrayEquals(before, original);
        assertArrayEquals(terrainOutput, emit(quad, brightnessTable));
        for (int vertex = 0; vertex < 4; vertex++) {
            int out = vertex * 9;
            int in = vertex * 8;
            assertTrue(Float.intBitsToFloat(itemOutput[out + 4]) > U0);
            assertTrue(Float.intBitsToFloat(itemOutput[out + 4]) < U1);
            assertTrue(Float.intBitsToFloat(itemOutput[out + 5]) > V0);
            assertTrue(Float.intBitsToFloat(itemOutput[out + 5]) < V1);
            assertEquals(itemOutput[out + 4], fallbackData[in + 4]);
            assertEquals(itemOutput[out + 5], fallbackData[in + 5]);
            for (int attribute = 0; attribute < 9; attribute++) {
                if (attribute != 4 && attribute != 5) {
                    assertEquals(terrainOutput[out + attribute], itemOutput[out + attribute]);
                }
            }
            for (int attribute = 0; attribute < 8; attribute++) {
                if (attribute != 4 && attribute != 5) {
                    assertEquals(before[in + attribute], fallbackData[in + attribute]);
                }
            }
        }
    }

    private static int[] emit(ModelQuadView quad, boolean brightnessTable) {
        int[] output = new int[4 * EntityVertex.STRIDE / Integer.BYTES];
        VertexBufferWriter writer = (stack, ptr, count, format) -> {
            assertEquals(4, count);
            assertSame(EntityVertex.FORMAT, format);
            for (int i = 0; i < output.length; i++) {
                output[i] = MemoryUtil.memGetInt(ptr + i * 4L);
            }
        };
        var pose = new PoseStack().last();
        if (brightnessTable) {
            BakedModelEncoder.writeQuadVertices(writer, pose, quad, 0.25f, 0.5f, 0.75f, 1.0f,
                    new float[]{1, 0.9f, 0.8f, 0.7f}, false, new int[]{1, 2, 3, 4}, 17);
        } else {
            BakedModelEncoder.writeQuadVertices(writer, pose, quad, 0xFF123456, 0x00100020, 17, false);
        }
        return output;
    }
}

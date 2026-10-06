package net.caffeinemc.mods.sodium.client.render.immediate.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.QuadEncoder;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static net.caffeinemc.mods.sodium.client.render.immediate.model.ItemTextureCoordinatesTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemFrapiEmissionTest {
    @Test
    void fastAndSlowFrapiItemPathsAgreeAndNonTerrainBlocksKeepOriginalUvs() {
        var quad = mock(MutableQuadViewImpl.class);
        setUvs(quad);
        when(quad.faceNormal()).thenReturn(new Vector3f(0, 1, 0));
        for (int i = 0; i < 4; i++) {
            when(quad.u(i)).thenReturn(i < 2 ? U0 : U1);
            when(quad.v(i)).thenReturn(i % 2 == 0 ? V0 : V1);
        }
        var bounds = ItemTextureCoordinates.forQuad(quad, sprite());
        var fast = new CapturingConsumer(true);
        var slow = new CapturingConsumer(false);
        QuadEncoder.writeQuadVertices(quad, (VertexConsumer) fast, 0, new Matrix4f(), true, new Matrix3f(), bounds);
        QuadEncoder.writeQuadVertices(quad, (VertexConsumer) slow, 0, new Matrix4f(), true, new Matrix3f(), bounds);
        assertArrayEquals(fast.uv, slow.uv);
        for (int i = 0; i < 4; i++) {
            assertTrue(fast.uv[i * 2] > U0 && fast.uv[i * 2] < U1);
            assertTrue(fast.uv[i * 2 + 1] > V0 && fast.uv[i * 2 + 1] < V1);
        }
        var block = new CapturingConsumer(true);
        QuadEncoder.writeQuadVertices(quad, (VertexConsumer) block, 0, new Matrix4f(), true, new Matrix3f());
        for (int i = 0; i < 4; i++) {
            assertEquals(quad.u(i), block.uv[i * 2]);
            assertEquals(quad.v(i), block.uv[i * 2 + 1]);
        }
    }

    private static class CapturingConsumer implements VertexConsumer, VertexBufferWriter {
        private final boolean fast;
        private final float[] uv = new float[8];
        private int vertex = -1;

        private CapturingConsumer(boolean fast) { this.fast = fast; }
        public boolean canUseIntrinsics() { return this.fast; }
        public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
            assertEquals(4, count);
            assertSame(EntityVertex.FORMAT, format);
            for (int i = 0; i < count; i++) {
                this.uv[i * 2] = MemoryUtil.memGetFloat(ptr + i * EntityVertex.STRIDE + 16);
                this.uv[i * 2 + 1] = MemoryUtil.memGetFloat(ptr + i * EntityVertex.STRIDE + 20);
            }
        }
        public VertexConsumer addVertex(float x, float y, float z) { this.vertex++; return this; }
        public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        public VertexConsumer setUv(float u, float v) {
            this.uv[this.vertex * 2] = u;
            this.uv[this.vertex * 2 + 1] = v;
            return this;
        }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}

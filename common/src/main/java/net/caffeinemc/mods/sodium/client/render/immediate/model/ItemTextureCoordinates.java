package net.caffeinemc.mods.sodium.client.render.immediate.model;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.util.ModelQuadUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

/** Sprite bounds for item emission, without modifying a quad shared with the terrain renderer. */
public record ItemTextureCoordinates(float minU, float maxU, float minV, float maxV) {
    public static final ItemTextureCoordinates UNCHANGED = new ItemTextureCoordinates(
            Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

    // 1/32 of a texel matches the successful 1/256 contraction of a full 16-pixel face.
    // Keep the margin in texels so high-resolution packs and rectangular atlases use the same protection.
    private static final float INSET_TEXELS = 1.0f / 32.0f;

    public static ItemTextureCoordinates forQuad(ModelQuadView quad, @Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            return UNCHANGED;
        }

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Custom quads may deliberately sample outside the advertised sprite. Do not remap those UVs.
        for (int vertex = 0; vertex < 4; vertex++) {
            float u = quad.getTexU(vertex);
            float v = quad.getTexV(vertex);
            if (!(u >= u0 && u <= u1 && v >= v0 && v <= v1)) {
                return UNCHANGED;
            }
        }

        return forSprite(u0, u1, v0, v1, sprite.contents().width(), sprite.contents().height());
    }

    static ItemTextureCoordinates forSprite(float u0, float u1, float v0, float v1, int width, int height) {
        if (!(u1 > u0 && v1 > v0) || width <= 0 || height <= 0) {
            return UNCHANGED;
        }

        float du = (u1 - u0) / width * INSET_TEXELS;
        float dv = (v1 - v0) / height * INSET_TEXELS;
        return new ItemTextureCoordinates(Math.max(u0 + du, Math.nextUp(u0)),
                Math.min(u1 - du, Math.nextDown(u1)), Math.max(v0 + dv, Math.nextUp(v0)),
                Math.min(v1 - dv, Math.nextDown(v1)));
    }

    public float u(float value) {
        return Math.max(this.minU, Math.min(this.maxU, value));
    }

    public float v(float value) {
        return Math.max(this.minV, Math.min(this.maxV, value));
    }

    /** Copies only when correction is needed; the original baked quad and all non-UV data stay intact. */
    public int[] copyVertexData(int[] vertices) {
        if (this == UNCHANGED || vertices.length < 4 * ModelQuadUtil.VERTEX_SIZE) {
            return vertices;
        }

        int[] result = vertices;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = ModelQuadUtil.vertexOffset(vertex) + ModelQuadUtil.TEXTURE_INDEX;
            float u = Float.intBitsToFloat(vertices[offset]);
            float v = Float.intBitsToFloat(vertices[offset + 1]);
            float insetU = this.u(u);
            float insetV = this.v(v);
            if (insetU != u || insetV != v) {
                if (result == vertices) {
                    result = vertices.clone();
                }
                result[offset] = Float.floatToRawIntBits(insetU);
                result[offset + 1] = Float.floatToRawIntBits(insetV);
            }
        }
        return result;
    }
}

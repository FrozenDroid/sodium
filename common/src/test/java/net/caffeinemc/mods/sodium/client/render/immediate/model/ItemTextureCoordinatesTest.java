package net.caffeinemc.mods.sodium.client.render.immediate.model;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemTextureCoordinatesTest {
    static final float U0 = 7776.0f / 8192;
    static final float U1 = 7792.0f / 8192;
    static final float V0 = 5056.0f / 8192;
    static final float V1 = 5072.0f / 8192;

    static TextureAtlasSprite sprite() {
        var sprite = mock(TextureAtlasSprite.class);
        var contents = mock(SpriteContents.class);
        when(sprite.getU0()).thenReturn(U0);
        when(sprite.getU1()).thenReturn(U1);
        when(sprite.getV0()).thenReturn(V0);
        when(sprite.getV1()).thenReturn(V1);
        when(sprite.contents()).thenReturn(contents);
        when(contents.width()).thenReturn(16);
        when(contents.height()).thenReturn(16);
        return sprite;
    }

    static void setUvs(ModelQuadView quad) {
        for (int i = 0; i < 4; i++) {
            when(quad.getTexU(i)).thenReturn(i < 2 ? U0 : U1);
            when(quad.getTexV(i)).thenReturn(i % 2 == 0 ? V0 : V1);
        }
    }

    @Test
    void capturedTorchStaysInsideItsSpriteAfterSubtexelRounding() {
        var bounds = ItemTextureCoordinates.forSprite(U0, U1, V0, V1, 16, 16);
        assertEquals(7776.03125f, bounds.u(U0) * 8192);
        assertEquals(7791.96875f, bounds.u(U1) * 8192);
        assertEquals(5056.03125f, bounds.v(V0) * 8192);
        assertEquals(5071.96875f, bounds.v(V1) * 8192);
        // Even rounding outward by half a 1/256-texel unit cannot select the adjacent sprite.
        assertTrue(bounds.u(U0) * 8192 - 0.5f / 256 > 7776);
        assertTrue(bounds.v(V1) * 8192 + 0.5f / 256 < 5072);
    }

    @Test
    void marginUsesTexelsOnRectangularAtlasesAndHighResolutionSprites() {
        var bounds = ItemTextureCoordinates.forSprite(0.5f, 0.5f + 128.0f / 8192,
                0.25f, 0.25f + 32.0f / 2048, 128, 32);
        assertEquals(1.0f / 32, (bounds.minU() - 0.5f) * 8192);
        assertEquals(1.0f / 32, (bounds.minV() - 0.25f) * 2048);
    }

    @Test
    void interiorUvsAreUnchangedAndRepeatedCorrectionDoesNotAccumulate() {
        var bounds = ItemTextureCoordinates.forSprite(U0, U1, V0, V1, 16, 16);
        float center = (U0 + U1) * 0.5f;
        assertEquals(center, bounds.u(center));
        assertEquals(bounds.u(U0), bounds.u(bounds.u(U0)));
    }

    @Test
    void customUvsOutsideSpriteAndMissingSpritesAreUnchanged() {
        var quad = mock(ModelQuadView.class);
        setUvs(quad);
        assertSame(ItemTextureCoordinates.UNCHANGED, ItemTextureCoordinates.forQuad(quad, null));
        when(quad.getTexU(2)).thenReturn(U1 + 1.0f / 8192);
        assertSame(ItemTextureCoordinates.UNCHANGED, ItemTextureCoordinates.forQuad(quad, sprite()));
        when(quad.getTexU(2)).thenReturn(Float.NaN);
        assertSame(ItemTextureCoordinates.UNCHANGED, ItemTextureCoordinates.forQuad(quad, sprite()));
    }

    @Test
    void invalidSpritesAreUnchanged() {
        assertSame(ItemTextureCoordinates.UNCHANGED, ItemTextureCoordinates.forSprite(0, 0, 0, 1, 16, 16));
        assertSame(ItemTextureCoordinates.UNCHANGED, ItemTextureCoordinates.forSprite(0, 1, 0, 1, 0, 16));
    }

    @Test
    void nestedRenderingAndExceptionsRestoreThePreviousState() {
        assertFalse(ItemRenderState.isActive());
        boolean outer = ItemRenderState.enter();
        try {
            assertThrows(IllegalStateException.class, () -> {
                boolean inner = ItemRenderState.enter();
                try {
                    assertTrue(ItemRenderState.isActive());
                    throw new IllegalStateException();
                } finally {
                    ItemRenderState.restore(inner);
                }
            });
            assertTrue(ItemRenderState.isActive());
        } finally {
            ItemRenderState.restore(outer);
        }
        assertFalse(ItemRenderState.isActive());
    }

    @Test
    void renderingStateIsThreadLocal() throws InterruptedException {
        boolean previous = ItemRenderState.enter();
        try {
            var activeElsewhere = new boolean[1];
            Thread thread = new Thread(() -> activeElsewhere[0] = ItemRenderState.isActive());
            thread.start();
            thread.join();
            assertFalse(activeElsewhere[0]);
        } finally {
            ItemRenderState.restore(previous);
        }
    }
}

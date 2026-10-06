package net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ItemRenderState;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ItemTextureCoordinates;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {
    // Correct the local vertex data read by the vanilla fallback. Preserve the original quad object,
    // including loader/mod-specific metadata and consumer overrides of putBulkData.
    @ModifyExpressionValue(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"
            + "Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/BakedQuad;getVertices()[I"))
    default int[] sodium$insetItemTextureCoordinates(int[] vertices, @Local(argsOnly = true) BakedQuad quad) {
        if (!ItemRenderState.isActive() || vertices.length < 32) {
            return vertices;
        }
        return ItemTextureCoordinates.forQuad((ModelQuadView) quad, quad.getSprite()).copyVertexData(vertices);
    }
}

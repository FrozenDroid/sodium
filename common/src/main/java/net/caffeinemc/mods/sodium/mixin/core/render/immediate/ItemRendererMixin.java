package net.caffeinemc.mods.sodium.mixin.core.render.immediate;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ItemRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/** Paired with core.model.TextureAtlasSpriteMixin, independently of optional rendering optimizations. */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @WrapMethod(method = "renderModelLists")
    private void sodium$renderModelLists(BakedModel model, ItemStack item, int light, int overlay,
                                        PoseStack matrices, VertexConsumer consumer, Operation<Void> original) {
        boolean previous = ItemRenderState.enter();
        try {
            original.call(model, item, light, overlay, matrices, consumer);
        } finally {
            ItemRenderState.restore(previous);
        }
    }

    @WrapMethod(method = "renderQuadList")
    private void sodium$renderQuadList(PoseStack matrices, VertexConsumer consumer, List<BakedQuad> quads,
                                      ItemStack item, int light, int overlay, Operation<Void> original) {
        boolean previous = ItemRenderState.enter();
        try {
            original.call(matrices, consumer, quads, item, light, overlay);
        } finally {
            ItemRenderState.restore(previous);
        }
    }
}

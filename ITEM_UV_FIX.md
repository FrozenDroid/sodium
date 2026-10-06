# Item atlas edge fix for Minecraft 1.21.1

This is an experimental, AI-assisted patch maintained in FrozenDroid's personal Sodium fork. It is not an upstream Sodium release or an upstream contribution.

## Problem

Sodium removes the baked-model UV shrink used by vanilla because its terrain renderer supplies its own correction. Item rendering does not use that terrain shader. Item faces whose UVs reach a sprite boundary can therefore sample a neighboring atlas sprite. This has been reported as item texture bleeding, including held-torch artifacts that become conspicuous with shader handheld lighting.

References: [Sodium #3013](https://github.com/CaffeineMC/sodium/issues/3013), [held torches in #3124](https://github.com/CaffeineMC/sodium/issues/3124#issuecomment-2831305952), and [Iris #3385](https://github.com/IrisShaders/Iris/issues/3385).

The issue was also reproduced on Iris 1.8.12 / Sodium 0.6.13. It is not established as a new 0.8.13 regression. A hand-shader UV contraction eliminated the reported lines; this patch moves sprite-edge protection into Sodium's item emission paths.

## Implementation

- Keep baked quads and the terrain UV correction unchanged.
- Clamp item UVs to a margin of 1/32 texel inside their sprite. This equals the edge displacement of the successful 1/256 shader contraction on a full 16-pixel face. Derive horizontal and vertical margins separately from sprite dimensions and atlas bounds.
- Keep interior coordinates unchanged. Skip correction for missing sprites or quads deliberately sampling outside their declared sprite.
- Scope ordinary baked item rendering with a thread-local state restored in `finally`, including nested calls. Core mixins keep the correction independent of optional item/bulk rendering optimizations.
- Correct both optimized baked-quad encoder overloads during item draws. The vanilla fallback receives a temporary vertex array while retaining the original quad object, loader-specific metadata, and vertex consumer.
- Pass the same bounds to both fast and fallback FRAPI item encoders. FRAPI block emission retains its original coordinates.

Custom renderers that bypass these item/bulk/FRAPI paths and emit arbitrary vertices are outside this patch's scope.

## Validation

Build with Java 21:

```text
./gradlew :common:test :neoforge:build :fabric:build
```

Both loader builds pass. Ten regression tests pass, covering the captured 8192x8192 torch-atlas coordinates, rectangular atlases, high-resolution sprites, custom UVs, missing sprites, repeated correction, nested/thread-local rendering state, both baked-quad encoder overloads, fallback copying, FRAPI fast/fallback agreement, and unchanged non-item output.

An isolated Minecraft 1.21.1 / NeoForge 21.1.256 client loaded the runtime mixins and rendered all 24 vertices of the real torch model into capture consumers. Fast and fallback emission produced identical UVs, every UV was strictly inside the sprite, and the item-render state was restored afterward. The same check passed again with both `mixin.features.render.model.item=false` and `mixin.features.render.immediate.buffer_builder=false`.

The unmodified build configuration uses NeoForge 21.1.228; the isolated client test temporarily used 21.1.256, matching the affected profile, because the resolved bundled Fabric API dependencies require at least 21.1.248. The temporary version change is not included in this patch.

## Remaining visual checks

The patched mod still needs confirmation in the affected Iris modpack. Use an original, unmodified shader pack so the earlier HandUVInset shader cannot mask the outcome. Keep the torch equipped, F1 off, and handheld lighting enabled; reproduce the usual camera movement in Complementary and Photon. Also check GUI, dropped items, item frames, enchanted items, resource-pack reload, and placed blocks.

No existing profile or world is modified by building this fork. The companion shader workaround is not required by the renderer patch.

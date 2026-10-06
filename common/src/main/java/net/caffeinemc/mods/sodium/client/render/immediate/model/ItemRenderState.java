package net.caffeinemc.mods.sodium.client.render.immediate.model;

/** Identifies item draws which use the shared vanilla bulk-vertex emitter. */
public final class ItemRenderState {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private ItemRenderState() {
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static boolean enter() {
        boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        return previous;
    }

    public static void restore(boolean previous) {
        ACTIVE.set(previous);
    }
}

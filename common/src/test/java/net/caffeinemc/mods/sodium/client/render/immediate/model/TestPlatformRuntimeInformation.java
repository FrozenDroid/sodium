package net.caffeinemc.mods.sodium.client.render.immediate.model;

import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import java.nio.file.Path;

/** Provides only the platform information needed by the encoder under test. */
public class TestPlatformRuntimeInformation implements PlatformRuntimeInformation {
    public boolean isDevelopmentEnvironment() { return true; }
    public Path getGameDirectory() { throw new UnsupportedOperationException(); }
    public Path getConfigDirectory() { throw new UnsupportedOperationException(); }
    public boolean platformHasEarlyLoadingScreen() { return false; }
    public boolean platformUsesRefmap() { return false; }
    public boolean isModInLoadingList(String modId) { return false; }
    public boolean usesAlphaMultiplication() { return true; }
}

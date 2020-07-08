package dev.hephaestus.sax.server;

import net.minecraft.server.network.ServerPlayerEntity;

public interface DeObfuscatorProvider {
	static DeObfuscator get(ServerPlayerEntity playerEntity) {
		return ((DeObfuscatorProvider) playerEntity).get();
	}

	DeObfuscator get();
}

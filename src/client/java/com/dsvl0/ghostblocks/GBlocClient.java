package com.dsvl0.ghostblocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class GBlocClient implements ClientModInitializer {
	public static KeyBinding placeGhostBlockKey;
	private static final Set<BlockPos> ghostBlocks = new HashSet<>();
	private World lastWorld = null;

	@Override
	public void onInitializeClient() {
		placeGhostBlockKey = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.ghost_block.place", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "category.ghost_block")
		);




		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null) {
				if (!ghostBlocks.isEmpty()) { ghostBlocks.clear(); }
				lastWorld = null;
				return;
			}

			if (lastWorld != client.world) {
				ghostBlocks.clear(); lastWorld = client.world;
			}


			if (client.world == null || client.player == null) return;

			while (placeGhostBlockKey.wasPressed()) {
				placeGhostBlock(client);
			}

			long window = client.getWindow().getHandle();
			if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
				handleLeftClick(client);
			}

		});

	}


	private void placeGhostBlock(MinecraftClient client) {
		if (client.world == null || client.player == null) return;

		ItemStack selectedStack = client.player.getInventory().getMainHandStack();
		if (!(selectedStack.getItem() instanceof BlockItem blockItem)) return;

		Block block = blockItem.getBlock();
		if (block.getDefaultState().getCollisionShape(client.world, BlockPos.ORIGIN).isEmpty()) return;

		HitResult hit = client.crosshairTarget;
		if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

		BlockPos targetPos = ((BlockHitResult) hit).getBlockPos().offset(((BlockHitResult) hit).getSide());

		client.world.setBlockState(targetPos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
		ghostBlocks.add(targetPos.toImmutable()); // сохранить как "призрачный"
	}

	private void handleLeftClick(MinecraftClient client) {
		if (client.world == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();

		// Проверка: только если это ghost-блок
		if (ghostBlocks.contains(pos)) {
			client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			ghostBlocks.remove(pos); // удалить из трекера
		}
	}

	public static void clearGhostBlocks() {
		ghostBlocks.clear();
	}


}
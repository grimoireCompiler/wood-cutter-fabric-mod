package wood.cutter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class StairsHelper {
    private static Map<Block, Block> blockToStairsCache = null;

    public static Map<Block, Block> initializeCache(MinecraftServer server) {
        if (blockToStairsCache != null) {
            return blockToStairsCache;
        }

        blockToStairsCache = new HashMap<>();
        List<StairBlock> stairs = BuiltInRegistries.BLOCK.stream()
                .filter(StairBlock.class::isInstance)
                .map(StairBlock.class::cast)
                .toList();

        try {
            Field baseStateField = StairBlock.class.getDeclaredField("baseState");
            baseStateField.setAccessible(true);
            for (StairBlock stair : stairs){
                Block baseBlock = ((BlockState) baseStateField.get(stair)).getBlock();
                blockToStairsCache.compute(baseBlock, (k,old) ->
                        (old == null || BuiltInRegistries.BLOCK.getKey(stair).toString().length() < BuiltInRegistries.BLOCK.getKey(old).toString().length() ) ? stair : old);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return blockToStairsCache;
    }

    public static boolean hasStairsVariant(Block block) {
        Block stairs = blockToStairsCache.get(block);
        return stairs != null;
    }

    public static Block getStairsVariant(Block block) {
        return blockToStairsCache.get(block);
    }
}
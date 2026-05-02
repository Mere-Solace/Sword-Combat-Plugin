package btm.sword.util.entity;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

/** Utilities for classifying player inputs and block interactions. */
public final class InputUtil {

    private InputUtil() {}

    // When a player start clicks:
    //      check if main hand is not air. if it isn't, it's a main hand start click. otherwise, it's an offhand start click.
    //      Either way, replace both with

    /** Returns {@code true} if the block is a switch, door, or inventory holder that should consume right-click inputs. */
    public static boolean isInteractible(Block block) {
        if (block == null) return false;

        BlockData data = block.getBlockData();
        BlockState state = block.getState();

        return data instanceof org.bukkit.block.data.type.Switch ||
                data instanceof org.bukkit.block.data.Openable ||
                state instanceof org.bukkit.inventory.InventoryHolder;
    }
}

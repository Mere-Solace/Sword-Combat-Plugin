package btm.sword.util.entity;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

public class InputUtil {
    // When a player start clicks:
    //      check if main hand is not air. if it isn't, it's a main hand start click. otherwise, it's an offhand start click.
    //      Either way, replace both with

    public static boolean isInteractible(Block block) {
        if (block == null) return false;

        BlockData data = block.getBlockData();
        BlockState state = block.getState();

        return data instanceof org.bukkit.block.data.type.Switch ||
                data instanceof org.bukkit.block.data.Openable ||
                state instanceof org.bukkit.inventory.InventoryHolder;
    }
}

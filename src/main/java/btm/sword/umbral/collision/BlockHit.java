package btm.sword.umbral.collision;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;

/**
 * Result of a single block-collision query performed by {@link BladeCollider}.
 * <p>
 * Carries the raw {@link RayTraceResult} alongside the resolved hit {@link Location} and
 * {@link BlockFace} for caller convenience — neither {@code position} nor {@code face} may be
 * {@code null}, since a {@code BlockHit} is only constructed when both are known.
 *
 * @param result   the underlying raytrace result returned by Bukkit
 * @param block    the block that was struck
 * @param position the world-space position where the ray entered the block
 * @param face     the face of the block that was struck
 */
public record BlockHit(RayTraceResult result, Block block, Location position, BlockFace face) {}

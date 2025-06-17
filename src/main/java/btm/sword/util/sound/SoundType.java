package btm.sword.util.sound;

import org.intellij.lang.annotations.Subst;

public enum SoundType {
	AMBIENT_BASALT_DELTAS_ADDITIONS("ambient.basalt_deltas.additions"),
	AMBIENT_BASALT_DELTAS_LOOP("ambient.basalt_deltas.loop"),
	AMBIENT_BASALT_DELTAS_MOOD("ambient.basalt_deltas.mood"),
	AMBIENT_CAVE("ambient.cave"),
	AMBIENT_CRIMSON_FOREST_ADDITIONS("ambient.crimson_forest.additions"),
	AMBIENT_CRIMSON_FOREST_LOOP("ambient.crimson_forest.loop"),
	AMBIENT_CRIMSON_FOREST_MOOD("ambient.crimson_forest.mood"),
	AMBIENT_NETHER_WASTES_ADDITIONS("ambient.nether_wastes.additions"),
	AMBIENT_NETHER_WASTES_LOOP("ambient.nether_wastes.loop"),
	AMBIENT_NETHER_WASTES_MOOD("ambient.nether_wastes.mood"),
	AMBIENT_SOUL_SAND_VALLEY_ADDITIONS("ambient.soul_sand_valley.additions"),
	AMBIENT_SOUL_SAND_VALLEY_LOOP("ambient.soul_sand_valley.loop"),
	AMBIENT_SOUL_SAND_VALLEY_MOOD("ambient.soul_sand_valley.mood"),
	AMBIENT_UNDERWATER_ENTER("ambient.underwater.enter"),
	AMBIENT_UNDERWATER_EXIT("ambient.underwater.exit"),
	AMBIENT_UNDERWATER_LOOP("ambient.underwater.loop"),
	AMBIENT_UNDERWATER_LOOP_ADDITIONS("ambient.underwater.loop.additions"),
	AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE("ambient.underwater.loop.additions.rare"),
	AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE("ambient.underwater.loop.additions.ultra_rare"),
	AMBIENT_WARPED_FOREST_ADDITIONS("ambient.warped_forest.additions"),
	AMBIENT_WARPED_FOREST_LOOP("ambient.warped_forest.loop"),
	AMBIENT_WARPED_FOREST_MOOD("ambient.warped_forest.mood"),
	
	BLOCK_ANCIENT_DEBRIS_BREAK("block.ancient_debris.break"),
	BLOCK_ANCIENT_DEBRIS_FALL("block.ancient_debris.fall"),
	BLOCK_ANCIENT_DEBRIS_HIT("block.ancient_debris.hit"),
	BLOCK_ANCIENT_DEBRIS_PLACE("block.ancient_debris.place"),
	BLOCK_ANCIENT_DEBRIS_STEP("block.ancient_debris.step"),
	
	BLOCK_ANVIL_BREAK("block.anvil.break"),
	BLOCK_ANVIL_DESTROY("block.anvil.destroy"),
	BLOCK_ANVIL_FALL("block.anvil.fall"),
	BLOCK_ANVIL_HIT("block.anvil.hit"),
	BLOCK_ANVIL_LAND("block.anvil.land"),
	BLOCK_ANVIL_PLACE("block.anvil.place"),
	BLOCK_ANVIL_STEP("block.anvil.step"),
	BLOCK_ANVIL_USE("block.anvil.use"),
	
	BLOCK_BAMBOO_BREAK("block.bamboo.break"),
	BLOCK_BAMBOO_FALL("block.bamboo.fall"),
	BLOCK_BAMBOO_HIT("block.bamboo.hit"),
	BLOCK_BAMBOO_PLACE("block.bamboo.place"),
	BLOCK_BAMBOO_STEP("block.bamboo.step"),
	
	BLOCK_BAMBOO_SAPLING_BREAK("block.bamboo_sapling.break"),
	BLOCK_BAMBOO_SAPLING_HIT("block.bamboo_sapling.hit"),
	BLOCK_BAMBOO_SAPLING_PLACE("block.bamboo_sapling.place"),
	
	BLOCK_BARREL_CLOSE("block.barrel.close"),
	BLOCK_BARREL_OPEN("block.barrel.open"),
	
	BLOCK_BASALT_BREAK("block.basalt.break"),
	BLOCK_BASALT_FALL("block.basalt.fall"),
	BLOCK_BASALT_HIT("block.basalt.hit"),
	BLOCK_BASALT_PLACE("block.basalt.place"),
	BLOCK_BASALT_STEP("block.basalt.step"),
	
	BLOCK_BEACON_ACTIVATE("block.beacon.activate"),
	BLOCK_BEACON_AMBIENT("block.beacon.ambient"),
	BLOCK_BEACON_DEACTIVATE("block.beacon.deactivate"),
	BLOCK_BEACON_POWER_SELECT("block.beacon.power_select"),
	
	BLOCK_BEEHIVE_DRIP("block.beehive.drip"),
	BLOCK_BEEHIVE_ENTER("block.beehive.enter"),
	BLOCK_BEEHIVE_EXIT("block.beehive.exit"),
	BLOCK_BEEHIVE_SHEAR("block.beehive.shear"),
	BLOCK_BEEHIVE_WORK("block.beehive.work"),
	
	BLOCK_BELL_RESONATE("block.bell.resonate"),
	BLOCK_BELL_USE("block.bell.use"),
	
	BLOCK_BLASTFURNACE_FIRE_CRACKLE("block.blastfurnace.fire_crackle"),
	
	BLOCK_BONE_BLOCK_BREAK("block.bone_block.break"),
	BLOCK_BONE_BLOCK_FALL("block.bone_block.fall"),
	BLOCK_BONE_BLOCK_HIT("block.bone_block.hit"),
	BLOCK_BONE_BLOCK_PLACE("block.bone_block.place"),
	BLOCK_BONE_BLOCK_STEP("block.bone_block.step"),
	
	BLOCK_BREWING_STAND_BREW("block.brewing_stand.brew"),
	
	BLOCK_BUBBLE_COLUMN_BUBBLE_POP("block.bubble_column.bubble_pop"),
	BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT("block.bubble_column.upwards_ambient"),
	BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE("block.bubble_column.upwards_inside"),
	BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT("block.bubble_column.whirlpool_ambient"),
	BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE("block.bubble_column.whirlpool_inside"),
	
	BLOCK_CAMPFIRE_CRACKLE("block.campfire.crackle"),
	
	BLOCK_CHAIN_BREAK("block.chain.break"),
	BLOCK_CHAIN_FALL("block.chain.fall"),
	BLOCK_CHAIN_HIT("block.chain.hit"),
	BLOCK_CHAIN_PLACE("block.chain.place"),
	BLOCK_CHAIN_STEP("block.chain.step"),
	
	BLOCK_CHEST_CLOSE("block.chest.close"),
	BLOCK_CHEST_LOCKED("block.chest.locked"),
	BLOCK_CHEST_OPEN("block.chest.open"),
	
	BLOCK_CHORUS_FLOWER_DEATH("block.chorus_flower.death"),
	BLOCK_CHORUS_FLOWER_GROW("block.chorus_flower.grow"),
	
	BLOCK_COMPARATOR_CLICK("block.comparator.click"),
	
	BLOCK_COMPOSTER_EMPTY("block.composter.empty"),
	BLOCK_COMPOSTER_FILL("block.composter.fill"),
	BLOCK_COMPOSTER_FILL_SUCCESS("block.composter.fill_success"),
	BLOCK_COMPOSTER_READY("block.composter.ready"),
	
	BLOCK_CONDUIT_ACTIVATE("block.conduit.activate"),
	BLOCK_CONDUIT_AMBIENT("block.conduit.ambient"),
	BLOCK_CONDUIT_AMBIENT_SHORT("block.conduit.ambient.short"),
	BLOCK_CONDUIT_ATTACK_TARGET("block.conduit.attack.target"),
	BLOCK_CONDUIT_DEACTIVATE("block.conduit.deactivate"),
	
	BLOCK_CORAL_BLOCK_BREAK("block.coral_block.break"),
	BLOCK_CORAL_BLOCK_FALL("block.coral_block.fall"),
	BLOCK_CORAL_BLOCK_HIT("block.coral_block.hit"),
	BLOCK_CORAL_BLOCK_PLACE("block.coral_block.place"),
	BLOCK_CORAL_BLOCK_STEP("block.coral_block.step"),
	
	BLOCK_CROP_BREAK("block.crop.break"),
	
	BLOCK_DISPENSER_DISPENSE("block.dispenser.dispense"),
	BLOCK_DISPENSER_FAIL("block.dispenser.fail"),
	BLOCK_DISPENSER_LAUNCH("block.dispenser.launch"),
	
	BLOCK_ENCHANTMENT_TABLE_USE("block.enchantment_table.use"),
	
	BLOCK_END_GATEWAY_SPAWN("block.end_gateway.spawn"),
	BLOCK_END_PORTAL_SPAWN("block.end_portal.spawn"),
	BLOCK_END_PORTAL_FRAME_FILL("block.end_portal_frame.fill"),
	
	BLOCK_ENDER_CHEST_CLOSE("block.ender_chest.close"),
	BLOCK_ENDER_CHEST_OPEN("block.ender_chest.open"),
	
	BLOCK_FENCE_GATE_CLOSE("block.fence_gate.close"),
	BLOCK_FENCE_GATE_OPEN("block.fence_gate.open"),
	
	BLOCK_FIRE_AMBIENT("block.fire.ambient"),
	BLOCK_FIRE_EXTINGUISH("block.fire.extinguish"),
	
	BLOCK_FUNGUS_BREAK("block.fungus.break"),
	BLOCK_FUNGUS_FALL("block.fungus.fall"),
	BLOCK_FUNGUS_HIT("block.fungus.hit"),
	BLOCK_FUNGUS_PLACE("block.fungus.place"),
	BLOCK_FUNGUS_STEP("block.fungus.step"),
	
	BLOCK_FURNACE_FIRE_CRACKLE("block.furnace.fire_crackle"),
	
	BLOCK_GILDED_BLACKSTONE_BREAK("block.gilded_blackstone.break"),
	BLOCK_GILDED_BLACKSTONE_FALL("block.gilded_blackstone.fall"),
	BLOCK_GILDED_BLACKSTONE_HIT("block.gilded_blackstone.hit"),
	BLOCK_GILDED_BLACKSTONE_PLACE("block.gilded_blackstone.place"),
	BLOCK_GILDED_BLACKSTONE_STEP("block.gilded_blackstone.step"),
	
	BLOCK_GLASS_BREAK("block.glass.break"),
	BLOCK_GLASS_FALL("block.glass.fall"),
	BLOCK_GLASS_HIT("block.glass.hit"),
	BLOCK_GLASS_PLACE("block.glass.place"),
	BLOCK_GLASS_STEP("block.glass.step"),
	
	BLOCK_GRASS_BREAK("block.grass.break"),
	BLOCK_GRASS_FALL("block.grass.fall"),
	BLOCK_GRASS_HIT("block.grass.hit"),
	BLOCK_GRASS_PLACE("block.grass.place"),
	BLOCK_GRASS_STEP("block.grass.step"),
	
	BLOCK_GRAVEL_BREAK("block.gravel.break"),
	BLOCK_GRAVEL_FALL("block.gravel.fall"),
	BLOCK_GRAVEL_HIT("block.gravel.hit"),
	BLOCK_GRAVEL_PLACE("block.gravel.place"),
	BLOCK_GRAVEL_STEP("block.gravel.step"),
	
	BLOCK_GRINDSTONE_USE("block.grindstone.use"),
	
	BLOCK_HONEY_BLOCK_BREAK("block.honey_block.break"),
	BLOCK_HONEY_BLOCK_FALL("block.honey_block.fall"),
	BLOCK_HONEY_BLOCK_HIT("block.honey_block.hit"),
	BLOCK_HONEY_BLOCK_PLACE("block.honey_block.place"),
	BLOCK_HONEY_BLOCK_SLIDE("block.honey_block.slide"),
	BLOCK_HONEY_BLOCK_STEP("block.honey_block.step"),
	
	BLOCK_IRON_DOOR_CLOSE("block.iron_door.close"),
	BLOCK_IRON_DOOR_OPEN("block.iron_door.open"),
	
	BLOCK_IRON_TRAPDOOR_CLOSE("block.iron_trapdoor.close"),
	BLOCK_IRON_TRAPDOOR_OPEN("block.iron_trapdoor.open"),
	
	BLOCK_LADDER_BREAK("block.ladder.break"),
	BLOCK_LADDER_FALL("block.ladder.fall"),
	BLOCK_LADDER_HIT("block.ladder.hit"),
	BLOCK_LADDER_PLACE("block.ladder.place"),
	BLOCK_LADDER_STEP("block.ladder.step"),
	
	BLOCK_LANTERN_BREAK("block.lantern.break"),
	BLOCK_LANTERN_FALL("block.lantern.fall"),
	BLOCK_LANTERN_HIT("block.lantern.hit"),
	BLOCK_LANTERN_PLACE("block.lantern.place"),
	BLOCK_LANTERN_STEP("block.lantern.step"),
	
	BLOCK_LAVA_AMBIENT("block.lava.ambient"),
	BLOCK_LAVA_EXTINGUISH("block.lava.extinguish"),
	BLOCK_LAVA_POP("block.lava.pop"),
	
	BLOCK_LEVER_CLICK("block.lever.click"),
	
	BLOCK_LILY_PAD_PLACE("block.lily_pad.place"),
	
	BLOCK_LODESTONE_BREAK("block.lodestone.break"),
	BLOCK_LODESTONE_FALL("block.lodestone.fall"),
	BLOCK_LODESTONE_HIT("block.lodestone.hit"),
	BLOCK_LODESTONE_PLACE("block.lodestone.place"),
	BLOCK_LODESTONE_STEP("block.lodestone.step"),
	
	BLOCK_METAL_BREAK("block.metal.break"),
	BLOCK_METAL_FALL("block.metal.fall"),
	BLOCK_METAL_HIT("block.metal.hit"),
	BLOCK_METAL_PLACE("block.metal.place"),
	BLOCK_METAL_STEP("block.metal.step"),
	
	BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF("block.metal_pressure_plate.click_off"),
	BLOCK_METAL_PRESSURE_PLATE_CLICK_ON("block.metal_pressure_plate.click_on"),
	
	BLOCK_NETHER_BRICKS_BREAK("block.nether_bricks.break"),
	BLOCK_NETHER_BRICKS_FALL("block.nether_bricks.fall"),
	BLOCK_NETHER_BRICKS_HIT("block.nether_bricks.hit"),
	BLOCK_NETHER_BRICKS_PLACE("block.nether_bricks.place"),
	BLOCK_NETHER_BRICKS_STEP("block.nether_bricks.step"),
	
	BLOCK_NETHER_GOLD_ORE_BREAK("block.nether_gold_ore.break"),
	BLOCK_NETHER_GOLD_ORE_FALL("block.nether_gold_ore.fall"),
	BLOCK_NETHER_GOLD_ORE_HIT("block.nether_gold_ore.hit"),
	BLOCK_NETHER_GOLD_ORE_PLACE("block.nether_gold_ore.place"),
	BLOCK_NETHER_GOLD_ORE_STEP("block.nether_gold_ore.step"),
	
	BLOCK_NETHER_ORE_BREAK("block.nether_ore.break"),
	BLOCK_NETHER_ORE_FALL("block.nether_ore.fall"),
	BLOCK_NETHER_ORE_HIT("block.nether_ore.hit"),
	BLOCK_NETHER_ORE_PLACE("block.nether_ore.place"),
	BLOCK_NETHER_ORE_STEP("block.nether_ore.step"),
	
	BLOCK_NETHER_SPROUTS_BREAK("block.nether_sprouts.break"),
	BLOCK_NETHER_SPROUTS_FALL("block.nether_sprouts.fall"),
	BLOCK_NETHER_SPROUTS_HIT("block.nether_sprouts.hit"),
	BLOCK_NETHER_SPROUTS_PLACE("block.nether_sprouts.place"),
	BLOCK_NETHER_SPROUTS_STEP("block.nether_sprouts.step"),
	
	BLOCK_NETHER_WART_BREAK("block.nether_wart.break"),
	BLOCK_NETHERITE_BLOCK_BREAK("block.netherite_block.break"),
	BLOCK_NETHERITE_BLOCK_FALL("block.netherite_block.fall"),
	BLOCK_NETHERITE_BLOCK_HIT("block.netherite_block.hit"),
	BLOCK_NETHERITE_BLOCK_PLACE("block.netherite_block.place"),
	BLOCK_NETHERITE_BLOCK_STEP("block.netherite_block.step"),
	
	BLOCK_NETHERRACK_BREAK("block.netherrack.break"),
	BLOCK_NETHERRACK_FALL("block.netherrack.fall"),
	BLOCK_NETHERRACK_HIT("block.netherrack.hit"),
	BLOCK_NETHERRACK_PLACE("block.netherrack.place"),
	BLOCK_NETHERRACK_STEP("block.netherrack.step"),
	
	BLOCK_NOTE_BLOCK_BANJO("block.note_block.banjo"),
	BLOCK_NOTE_BLOCK_BASEDRUM("block.note_block.basedrum"),
	BLOCK_NOTE_BLOCK_BASS("block.note_block.bass"),
	BLOCK_NOTE_BLOCK_BELL("block.note_block.bell"),
	BLOCK_NOTE_BLOCK_BIT("block.note_block.bit"),
	BLOCK_NOTE_BLOCK_CHIME("block.note_block.chime"),
	BLOCK_NOTE_BLOCK_COW_BELL("block.note_block.cow_bell"),
	BLOCK_NOTE_BLOCK_DIDGERIDOO("block.note_block.didgeridoo"),
	BLOCK_NOTE_BLOCK_FLUTE("block.note_block.flute"),
	BLOCK_NOTE_BLOCK_GUITAR("block.note_block.guitar"),
	BLOCK_NOTE_BLOCK_HARP("block.note_block.harp"),
	BLOCK_NOTE_BLOCK_HAT("block.note_block.hat"),
	BLOCK_NOTE_BLOCK_IRON_XYLOPHONE("block.note_block.iron_xylophone"),
	BLOCK_NOTE_BLOCK_PLING("block.note_block.pling"),
	BLOCK_NOTE_BLOCK_SNARE("block.note_block.snare"),
	BLOCK_NOTE_BLOCK_XYLOPHONE("block.note_block.xylophone"),
	
	BLOCK_NYLIUM_BREAK("block.nylium.break"),
	BLOCK_NYLIUM_FALL("block.nylium.fall"),
	BLOCK_NYLIUM_HIT("block.nylium.hit"),
	BLOCK_NYLIUM_PLACE("block.nylium.place"),
	BLOCK_NYLIUM_STEP("block.nylium.step"),
	
	BLOCK_PISTON_CONTRACT("block.piston.contract"),
	BLOCK_PISTON_EXTEND("block.piston.extend"),
	
	BLOCK_PORTAL_AMBIENT("block.portal.ambient"),
	BLOCK_PORTAL_TRAVEL("block.portal.travel"),
	BLOCK_PORTAL_TRIGGER("block.portal.trigger"),
	
	BLOCK_PUMPKIN_CARVE("block.pumpkin.carve"),
	
	BLOCK_REDSTONE_TORCH_BURNOUT("block.redstone_torch.burnout"),
	
	BLOCK_RESPAWN_ANCHOR_AMBIENT("block.respawn_anchor.ambient"),
	BLOCK_RESPAWN_ANCHOR_CHARGE("block.respawn_anchor.charge"),
	BLOCK_RESPAWN_ANCHOR_DEPLETE("block.respawn_anchor.deplete"),
	BLOCK_RESPAWN_ANCHOR_SET_SPAWN("block.respawn_anchor.set_spawn"),
	
	BLOCK_ROOTS_BREAK("block.roots.break"),
	BLOCK_ROOTS_FALL("block.roots.fall"),
	BLOCK_ROOTS_HIT("block.roots.hit"),
	BLOCK_ROOTS_PLACE("block.roots.place"),
	BLOCK_ROOTS_STEP("block.roots.step"),
	
	BLOCK_SAND_BREAK("block.sand.break"),
	BLOCK_SAND_FALL("block.sand.fall"),
	BLOCK_SAND_HIT("block.sand.hit"),
	BLOCK_SAND_PLACE("block.sand.place"),
	BLOCK_SAND_STEP("block.sand.step"),
	
	BLOCK_SCAFFOLDING_BREAK("block.scaffolding.break"),
	BLOCK_SCAFFOLDING_FALL("block.scaffolding.fall"),
	BLOCK_SCAFFOLDING_HIT("block.scaffolding.hit"),
	BLOCK_SCAFFOLDING_PLACE("block.scaffolding.place"),
	BLOCK_SCAFFOLDING_STEP("block.scaffolding.step"),
	
	BLOCK_SHROOMLIGHT_BREAK("block.shroomlight.break"),
	BLOCK_SHROOMLIGHT_FALL("block.shroomlight.fall"),
	BLOCK_SHROOMLIGHT_HIT("block.shroomlight.hit"),
	BLOCK_SHROOMLIGHT_PLACE("block.shroomlight.place"),
	BLOCK_SHROOMLIGHT_STEP("block.shroomlight.step"),
	
	BLOCK_SHULKER_BOX_CLOSE("block.shulker_box.close"),
	BLOCK_SHULKER_BOX_OPEN("block.shulker_box.open"),
	
	BLOCK_SLIME_BLOCK_BREAK("block.slime_block.break"),
	BLOCK_SLIME_BLOCK_FALL("block.slime_block.fall"),
	BLOCK_SLIME_BLOCK_HIT("block.slime_block.hit"),
	BLOCK_SLIME_BLOCK_PLACE("block.slime_block.place"),
	BLOCK_SLIME_BLOCK_STEP("block.slime_block.step"),
	BLOCK_SMITHING_TABLE_USE("block.smithing_table.use"),
	BLOCK_SMOKER_SMOKE("block.smoker.smoke"),
	
	BLOCK_SNOW_BREAK("block.snow.break"),
	BLOCK_SNOW_FALL("block.snow.fall"),
	BLOCK_SNOW_HIT("block.snow.hit"),
	BLOCK_SNOW_PLACE("block.snow.place"),
	BLOCK_SNOW_STEP("block.snow.step"),
	
	BLOCK_SOUL_SAND_BREAK("block.soul_sand.break"),
	BLOCK_SOUL_SAND_FALL("block.soul_sand.fall"),
	BLOCK_SOUL_SAND_HIT("block.soul_sand.hit"),
	BLOCK_SOUL_SAND_PLACE("block.soul_sand.place"),
	BLOCK_SOUL_SAND_STEP("block.soul_sand.step"),
	
	BLOCK_SOUL_SOIL_BREAK("block.soul_soil.break"),
	BLOCK_SOUL_SOIL_FALL("block.soul_soil.fall"),
	BLOCK_SOUL_SOIL_HIT("block.soul_soil.hit"),
	BLOCK_SOUL_SOIL_PLACE("block.soul_soil.place"),
	BLOCK_SOUL_SOIL_STEP("block.soul_soil.step"),
	
	BLOCK_STEM_BREAK("block.stem.break"),
	BLOCK_STEM_FALL("block.stem.fall"),
	BLOCK_STEM_HIT("block.stem.hit"),
	BLOCK_STEM_PLACE("block.stem.place"),
	BLOCK_STEM_STEP("block.stem.step"),
	
	BLOCK_STONE_BREAK("block.stone.break"),
	BLOCK_STONE_FALL("block.stone.fall"),
	BLOCK_STONE_HIT("block.stone.hit"),
	BLOCK_STONE_PLACE("block.stone.place"),
	BLOCK_STONE_STEP("block.stone.step"),
	
	BLOCK_STONE_BUTTON_CLICK_OFF("block.stone_button.click_off"),
	BLOCK_STONE_BUTTON_CLICK_ON("block.stone_button.click_on"),
	
	BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF("block.stone_pressure_plate.click_off"),
	BLOCK_STONE_PRESSURE_PLATE_CLICK_ON("block.stone_pressure_plate.click_on"),
	
	BLOCK_SWEET_BERRY_BUSH_BREAK("block.sweet_berry_bush.break"),
	BLOCK_SWEET_BERRY_BUSH_PLACE("block.sweet_berry_bush.place"),
	
	BLOCK_TRIPWIRE_ATTACH("block.tripwire.attach"),
	BLOCK_TRIPWIRE_CLICK_OFF("block.tripwire.click_off"),
	BLOCK_TRIPWIRE_CLICK_ON("block.tripwire.click_on"),
	BLOCK_TRIPWIRE_DETACH("block.tripwire.detach"),
	
	BLOCK_VINE_STEP("block.vine.step"),
	
	BLOCK_WART_BLOCK_BREAK("block.wart_block.break"),
	BLOCK_WART_BLOCK_FALL("block.wart_block.fall"),
	BLOCK_WART_BLOCK_HIT("block.wart_block.hit"),
	BLOCK_WART_BLOCK_PLACE("block.wart_block.place"),
	BLOCK_WART_BLOCK_STEP("block.wart_block.step"),
	
	BLOCK_WATER_AMBIENT("block.water.ambient"),
	
	BLOCK_WEEPING_VINES_BREAK("block.weeping_vines.break"),
	BLOCK_WEEPING_VINES_FALL("block.weeping_vines.fall"),
	BLOCK_WEEPING_VINES_HIT("block.weeping_vines.hit"),
	BLOCK_WEEPING_VINES_PLACE("block.weeping_vines.place"),
	BLOCK_WEEPING_VINES_STEP("block.weeping_vines.step"),
	
	BLOCK_WET_GRASS_BREAK("block.wet_grass.break"),
	BLOCK_WET_GRASS_FALL("block.wet_grass.fall"),
	BLOCK_WET_GRASS_HIT("block.wet_grass.hit"),
	BLOCK_WET_GRASS_PLACE("block.wet_grass.place"),
	BLOCK_WET_GRASS_STEP("block.wet_grass.step"),
	
	BLOCK_WOOD_BREAK("block.wood.break"),
	BLOCK_WOOD_FALL("block.wood.fall"),
	BLOCK_WOOD_HIT("block.wood.hit"),
	BLOCK_WOOD_PLACE("block.wood.place"),
	BLOCK_WOOD_STEP("block.wood.step"),
	
	BLOCK_WOODEN_BUTTON_CLICK_OFF("block.wooden_button.click_off"),
	BLOCK_WOODEN_BUTTON_CLICK_ON("block.wooden_button.click_on"),
	
	BLOCK_WOODEN_DOOR_CLOSE("block.wooden_door.close"),
	BLOCK_WOODEN_DOOR_OPEN("block.wooden_door.open"),
	
	BLOCK_WOODEN_PRESSURE_PLATE_CLICK_OFF("block.wooden_pressure_plate.click_off"),
	BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON("block.wooden_pressure_plate.click_on"),
	
	BLOCK_WOODEN_TRAPDOOR_CLOSE("block.wooden_trapdoor.close"),
	BLOCK_WOODEN_TRAPDOOR_OPEN("block.wooden_trapdoor.open"),
	
	BLOCK_WOOL_BREAK("block.wool.break"),
	BLOCK_WOOL_FALL("block.wool.fall"),
	BLOCK_WOOL_HIT("block.wool.hit"),
	BLOCK_WOOL_PLACE("block.wool.place"),
	BLOCK_WOOL_STEP("block.wool.step"),
	
	ENCHANT_THORNS_HIT("enchant.thorns.hit"),
	
	ENTITY_ARMOR_STAND_BREAK("entity.armor_stand.break"),
	ENTITY_ARMOR_STAND_FALL("entity.armor_stand.fall"),
	ENTITY_ARMOR_STAND_HIT("entity.armor_stand.hit"),
	ENTITY_ARMOR_STAND_PLACE("entity.armor_stand.place"),
	
	ENTITY_ARROW_HIT("entity.arrow.hit"),
	ENTITY_ARROW_HIT_PLAYER("entity.arrow.hit_player"),
	ENTITY_ARROW_SHOOT("entity.arrow.shoot"),
	
	ENTITY_BAT_AMBIENT("entity.bat.ambient"),
	ENTITY_BAT_DEATH("entity.bat.death"),
	ENTITY_BAT_HURT("entity.bat.hurt"),
	ENTITY_BAT_LOOP("entity.bat.loop"),
	ENTITY_BAT_TAKEOFF("entity.bat.takeoff"),
	
	ENTITY_BEE_DEATH("entity.bee.death"),
	ENTITY_BEE_HURT("entity.bee.hurt"),
	ENTITY_BEE_LOOP("entity.bee.loop"),
	ENTITY_BEE_LOOP_AGGRESSIVE("entity.bee.loop_aggressive"),
	ENTITY_BEE_POLLINATE("entity.bee.pollinate"),
	ENTITY_BEE_STING("entity.bee.sting"),
	
	ENTITY_BLAZE_AMBIENT("entity.blaze.ambient"),
	ENTITY_BLAZE_BURN("entity.blaze.burn"),
	ENTITY_BLAZE_DEATH("entity.blaze.death"),
	ENTITY_BLAZE_HURT("entity.blaze.hurt"),
	ENTITY_BLAZE_SHOOT("entity.blaze.shoot"),
	
	ENTITY_BOAT_PADDLE_LAND("entity.boat.paddle_land"),
	ENTITY_BOAT_PADDLE_WATER("entity.boat.paddle_water"),
	
	ENTITY_CAT_AMBIENT("entity.cat.ambient"),
	ENTITY_CAT_BEG_FOR_FOOD("entity.cat.beg_for_food"),
	ENTITY_CAT_DEATH("entity.cat.death"),
	ENTITY_CAT_EAT("entity.cat.eat"),
	ENTITY_CAT_HISS("entity.cat.hiss"),
	ENTITY_CAT_HURT("entity.cat.hurt"),
	ENTITY_CAT_PURR("entity.cat.purr"),
	ENTITY_CAT_PURREOW("entity.cat.purreow"),
	ENTITY_CAT_STRAY_AMBIENT("entity.cat.stray_ambient"),
	
	ENTITY_CHICKEN_AMBIENT("entity.chicken.ambient"),
	ENTITY_CHICKEN_DEATH("entity.chicken.death"),
	ENTITY_CHICKEN_EGG("entity.chicken.egg"),
	ENTITY_CHICKEN_HURT("entity.chicken.hurt"),
	ENTITY_CHICKEN_STEP("entity.chicken.step"),
	
	ENTITY_COD_AMBIENT("entity.cod.ambient"),
	ENTITY_COD_DEATH("entity.cod.death"),
	ENTITY_COD_FLOP("entity.cod.flop"),
	ENTITY_COD_HURT("entity.cod.hurt"),
	
	ENTITY_COW_AMBIENT("entity.cow.ambient"),
	ENTITY_COW_DEATH("entity.cow.death"),
	ENTITY_COW_HURT("entity.cow.hurt"),
	ENTITY_COW_MILK("entity.cow.milk"),
	ENTITY_COW_STEP("entity.cow.step"),
	
	ENTITY_CREEPER_DEATH("entity.creeper.death"),
	ENTITY_CREEPER_HURT("entity.creeper.hurt"),
	ENTITY_CREEPER_PRIMED("entity.creeper.primed"),
	
	ENTITY_DOLPHIN_AMBIENT("entity.dolphin.ambient"),
	ENTITY_DOLPHIN_AMBIENT_WATER("entity.dolphin.ambient_water"),
	ENTITY_DOLPHIN_ATTACK("entity.dolphin.attack"),
	ENTITY_DOLPHIN_DEATH("entity.dolphin.death"),
	ENTITY_DOLPHIN_EAT("entity.dolphin.eat"),
	ENTITY_DOLPHIN_HURT("entity.dolphin.hurt"),
	ENTITY_DOLPHIN_JUMP("entity.dolphin.jump"),
	ENTITY_DOLPHIN_PLAY("entity.dolphin.play"),
	ENTITY_DOLPHIN_SPLASH("entity.dolphin.splash"),
	ENTITY_DOLPHIN_SWIM("entity.dolphin.swim"),
	
	ENTITY_DONKEY_AMBIENT("entity.donkey.ambient"),
	ENTITY_DONKEY_ANGRY("entity.donkey.angry"),
	ENTITY_DONKEY_CHEST("entity.donkey.chest"),
	ENTITY_DONKEY_DEATH("entity.donkey.death"),
	ENTITY_DONKEY_EAT("entity.donkey.eat"),
	ENTITY_DONKEY_HURT("entity.donkey.hurt"),
	
	ENTITY_DRAGON_FIREBALL_EXPLODE("entity.dragon_fireball.explode"),
	
	ENTITY_DROWNED_AMBIENT("entity.drowned.ambient"),
	ENTITY_DROWNED_AMBIENT_WATER("entity.drowned.ambient_water"),
	ENTITY_DROWNED_DEATH("entity.drowned.death"),
	ENTITY_DROWNED_DEATH_WATER("entity.drowned.death_water"),
	ENTITY_DROWNED_HURT("entity.drowned.hurt"),
	ENTITY_DROWNED_HURT_WATER("entity.drowned.hurt_water"),
	ENTITY_DROWNED_SHOOT("entity.drowned.shoot"),
	ENTITY_DROWNED_STEP("entity.drowned.step"),
	ENTITY_DROWNED_SWIM("entity.drowned.swim"),
	
	ENTITY_EGG_THROW("entity.egg.throw"),
	
	ENTITY_ELDER_GUARDIAN_AMBIENT("entity.elder_guardian.ambient"),
	ENTITY_ELDER_GUARDIAN_AMBIENT_LAND("entity.elder_guardian.ambient_land"),
	ENTITY_ELDER_GUARDIAN_CURSE("entity.elder_guardian.curse"),
	ENTITY_ELDER_GUARDIAN_DEATH("entity.elder_guardian.death"),
	ENTITY_ELDER_GUARDIAN_DEATH_LAND("entity.elder_guardian.death_land"),
	ENTITY_ELDER_GUARDIAN_FLOP("entity.elder_guardian.flop"),
	ENTITY_ELDER_GUARDIAN_HURT("entity.elder_guardian.hurt"),
	ENTITY_ELDER_GUARDIAN_HURT_LAND("entity.elder_guardian.hurt_land"),
	
	ENTITY_ENDER_DRAGON_AMBIENT("entity.ender_dragon.ambient"),
	ENTITY_ENDER_DRAGON_DEATH("entity.ender_dragon.death"),
	ENTITY_ENDER_DRAGON_FLAP("entity.ender_dragon.flap"),
	ENTITY_ENDER_DRAGON_GROWL("entity.ender_dragon.growl"),
	ENTITY_ENDER_DRAGON_HURT("entity.ender_dragon.hurt"),
	ENTITY_ENDER_DRAGON_SHOOT("entity.ender_dragon.shoot"),
	
	ENTITY_ENDER_EYE_DEATH("entity.ender_eye.death"),
	ENTITY_ENDER_EYE_LAUNCH("entity.ender_eye.launch"),
	
	ENTITY_ENDER_PEARL_THROW("entity.ender_pearl.throw"),
	
	ENTITY_ENDERMAN_AMBIENT("entity.enderman.ambient"),
	ENTITY_ENDERMAN_DEATH("entity.enderman.death"),
	ENTITY_ENDERMAN_HURT("entity.enderman.hurt"),
	ENTITY_ENDERMAN_SCREAM("entity.enderman.scream"),
	ENTITY_ENDERMAN_STARE("entity.enderman.stare"),
	ENTITY_ENDERMAN_TELEPORT("entity.enderman.teleport"),
	
	ENTITY_ENDERMITE_AMBIENT("entity.endermite.ambient"),
	ENTITY_ENDERMITE_DEATH("entity.endermite.death"),
	ENTITY_ENDERMITE_HURT("entity.endermite.hurt"),
	ENTITY_ENDERMITE_STEP("entity.endermite.step"),
	
	ENTITY_EVOKER_AMBIENT("entity.evoker.ambient"),
	ENTITY_EVOKER_CAST_SPELL("entity.evoker.cast_spell"),
	ENTITY_EVOKER_CELEBRATE("entity.evoker.celebrate"),
	ENTITY_EVOKER_DEATH("entity.evoker.death"),
	ENTITY_EVOKER_HURT("entity.evoker.hurt"),
	ENTITY_EVOKER_PREPARE_ATTACK("entity.evoker.prepare_attack"),
	ENTITY_EVOKER_PREPARE_SUMMON("entity.evoker.prepare_summon"),
	ENTITY_EVOKER_PREPARE_WOLOLO("entity.evoker.prepare_wololo"),
	
	ENTITY_EVOKER_FANGS_ATTACK("entity.evoker_fangs.attack"),
	
	ENTITY_EXPERIENCE_BOTTLE_THROW("entity.experience_bottle.throw"),
	ENTITY_EXPERIENCE_ORB_PICKUP("entity.experience_orb.pickup"),
	
	ENTITY_FIREWORK_ROCKET_BLAST("entity.firework_rocket.blast"),
	ENTITY_FIREWORK_ROCKET_BLAST_FAR("entity.firework_rocket.blast_far"),
	ENTITY_FIREWORK_ROCKET_LARGE_BLAST("entity.firework_rocket.large_blast"),
	ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR("entity.firework_rocket.large_blast_far"),
	ENTITY_FIREWORK_ROCKET_LAUNCH("entity.firework_rocket.launch"),
	ENTITY_FIREWORK_ROCKET_SHOOT("entity.firework_rocket.shoot"),
	ENTITY_FIREWORK_ROCKET_TWINKLE("entity.firework_rocket.twinkle"),
	ENTITY_FIREWORK_ROCKET_TWINKLE_FAR("entity.firework_rocket.twinkle_far"),
	
	ENTITY_FISH_SWIM("entity.fish.swim"),
	
	ENTITY_FISHING_BOBBER_RETRIEVE("entity.fishing_bobber.retrieve"),
	ENTITY_FISHING_BOBBER_SPLASH("entity.fishing_bobber.splash"),
	ENTITY_FISHING_BOBBER_THROW("entity.fishing_bobber.throw"),
	
	ENTITY_FOX_AGGRO("entity.fox.aggro"),
	ENTITY_FOX_AMBIENT("entity.fox.ambient"),
	ENTITY_FOX_BITE("entity.fox.bite"),
	ENTITY_FOX_DEATH("entity.fox.death"),
	ENTITY_FOX_EAT("entity.fox.eat"),
	ENTITY_FOX_HURT("entity.fox.hurt"),
	ENTITY_FOX_SCREECH("entity.fox.screech"),
	ENTITY_FOX_SLEEP("entity.fox.sleep"),
	ENTITY_FOX_SNIFF("entity.fox.sniff"),
	ENTITY_FOX_SPIT("entity.fox.spit"),
	ENTITY_FOX_TELEPORT("entity.fox.teleport"),
	
	ENTITY_GENERIC_BIG_FALL("entity.generic.big_fall"),
	ENTITY_GENERIC_BURN("entity.generic.burn"),
	ENTITY_GENERIC_DEATH("entity.generic.death"),
	ENTITY_GENERIC_DRINK("entity.generic.drink"),
	ENTITY_GENERIC_EAT("entity.generic.eat"),
	ENTITY_GENERIC_EXPLODE("entity.generic.explode"),
	ENTITY_GENERIC_EXTINGUISH_FIRE("entity.generic.extinguish_fire"),
	ENTITY_GENERIC_HURT("entity.generic.hurt"),
	ENTITY_GENERIC_SMALL_FALL("entity.generic.small_fall"),
	ENTITY_GENERIC_SPLASH("entity.generic.splash");
	
	private final String key;
	
	SoundType(String key) {
		this.key = key;
	}
	
	@Subst("class.item.type")
	public String getKey() {
		return key;
	}
}
	

	
	//TODO get gpt to generate more lines
	//entity.generic.swim
	//entity.ghast.ambient
	//entity.ghast.death
	//entity.ghast.hurt
	//entity.ghast.scream
	//entity.ghast.shoot
	//entity.ghast.warn
	//entity.guardian.ambient
	//entity.guardian.ambient_land
	//entity.guardian.attack
	//entity.guardian.death
	//entity.guardian.death_land
	//entity.guardian.flop
	//entity.guardian.hurt
	//entity.guardian.hurt_land
	//entity.hoglin.ambient
	//entity.hoglin.angry
	//entity.hoglin.attack
	//entity.hoglin.converted_to_zombified
	//entity.hoglin.death
	//entity.hoglin.hurt
	//entity.hoglin.retreat
	//entity.hoglin.step
	//entity.horse.ambient
	//entity.horse.angry
	//entity.horse.armor
	//entity.horse.breathe
	//entity.horse.death
	//entity.horse.eat
	//entity.horse.gallop
	//entity.horse.hurt
	//entity.horse.jump
	//entity.horse.land
	//entity.horse.saddle
	//entity.horse.step
	//entity.horse.step_wood
	//entity.hostile.big_fall
	//entity.hostile.death
	//entity.hostile.hurt
	//entity.hostile.small_fall
	//entity.hostile.splash
	//entity.hostile.swim
	//entity.husk.ambient
	//entity.husk.converted_to_zombie
	//entity.husk.death
	//entity.husk.hurt
	//entity.husk.step
	//entity.illusioner.ambient
	//entity.illusioner.cast_spell
	//entity.illusioner.death
	//entity.illusioner.hurt
	//entity.illusioner.mirror_move
	//entity.illusioner.prepare_blindness
	//entity.illusioner.prepare_mirror
	//entity.iron_golem.attack
	//entity.iron_golem.damage
	//entity.iron_golem.death
	//entity.iron_golem.hurt
	//entity.iron_golem.repair
	//entity.iron_golem.step
	//entity.item.break
	//entity.item.pickup
	//entity.item_frame.add_item
	//entity.item_frame.break
	//entity.item_frame.place
	//entity.item_frame.remove_item
	//entity.item_frame.rotate_item
	//entity.leash_knot.break
	//entity.leash_knot.place
	//entity.lightning_bolt.impact
	//entity.lightning_bolt.thunder
	//entity.lingering_potion.throw
	//entity.llama.ambient
	//entity.llama.angry
	//entity.llama.chest
	//entity.llama.death
	//entity.llama.eat
	//entity.llama.hurt
	//entity.llama.spit
	//entity.llama.step
	//entity.llama.swag
	//entity.magma_cube.death
	//entity.magma_cube.death_small
	//entity.magma_cube.hurt
	//entity.magma_cube.hurt_small
	//entity.magma_cube.jump
	//entity.magma_cube.squish
	//entity.magma_cube.squish_small
	//entity.minecart.inside
	//entity.minecart.riding
	//entity.mooshroom.convert
	//entity.mooshroom.eat
	//entity.mooshroom.milk
	//entity.mooshroom.shear
	//entity.mooshroom.suspicious_milk
	//entity.mule.ambient
	//entity.mule.angry
	//entity.mule.chest
	//entity.mule.death
	//entity.mule.eat
	//entity.mule.hurt
	//entity.ocelot.ambient
	//entity.ocelot.death
	//entity.ocelot.hurt
	//entity.painting.break
	//entity.painting.place
	//entity.panda.aggressive_ambient
	//entity.panda.ambient
	//entity.panda.bite
	//entity.panda.cant_breed
	//entity.panda.death
	//entity.panda.eat
	//entity.panda.hurt
	//entity.panda.pre_sneeze
	//entity.panda.sneeze
	//entity.panda.step
	//entity.panda.worried_ambient
	//entity.parrot.ambient
	//entity.parrot.death
	//entity.parrot.eat
	//entity.parrot.fly
	//entity.parrot.hurt
	//entity.parrot.imitate.blaze
	//entity.parrot.imitate.creeper
	//entity.parrot.imitate.drowned
	//entity.parrot.imitate.elder_guardian
	//entity.parrot.imitate.ender_dragon
	//entity.parrot.imitate.endermite
	//entity.parrot.imitate.evoker
	//entity.parrot.imitate.ghast
	//entity.parrot.imitate.guardian
	//entity.parrot.imitate.hoglin
	//entity.parrot.imitate.husk
	//entity.parrot.imitate.illusioner
	//entity.parrot.imitate.magma_cube
	//entity.parrot.imitate.phantom
	//entity.parrot.imitate.piglin
	//entity.parrot.imitate.pillager
	//entity.parrot.imitate.ravager
	//entity.parrot.imitate.shulker
	//entity.parrot.imitate.silverfish
	//entity.parrot.imitate.skeleton
	//entity.parrot.imitate.slime
	//entity.parrot.imitate.spider
	//entity.parrot.imitate.stray
	//entity.parrot.imitate.vex
	//entity.parrot.imitate.vindicator
	//entity.parrot.imitate.witch
	//entity.parrot.imitate.wither
	//entity.parrot.imitate.wither_skeleton
	//entity.parrot.imitate.zoglin
	//entity.parrot.imitate.zombie
	//entity.parrot.imitate.zombie_villager
	//entity.parrot.step
	//entity.phantom.ambient
	//entity.phantom.bite
	//entity.phantom.death
	//entity.phantom.flap
	//entity.phantom.hurt
	//entity.phantom.swoop
	//entity.pig.ambient
	//entity.pig.death
	//entity.pig.hurt
	//entity.pig.saddle
	//entity.pig.step
	//entity.piglin.admiring_item
	//entity.piglin.ambient
	//entity.piglin.angry
	//entity.piglin.celebrate
	//entity.piglin.converted_to_zombified
	//entity.piglin.death
	//entity.piglin.hurt
	//entity.piglin.jealous
	//entity.piglin.retreat
	//entity.piglin.step
	//entity.pillager.ambient
	//entity.pillager.celebrate
	//entity.pillager.death
	//entity.pillager.hurt
	//entity.player.attack.crit
	//entity.player.attack.knockback
	//entity.player.attack.nodamage
	//entity.player.attack.strong
	//entity.player.attack.sweep
	//entity.player.attack.weak
	//entity.player.big_fall
	//entity.player.breath
	//entity.player.burp
	//entity.player.death
	//entity.player.hurt
	//entity.player.hurt_drown
	//entity.player.hurt_on_fire
	//entity.player.hurt_sweet_berry_bush
	//entity.player.levelup
	//entity.player.small_fall
	//entity.player.splash
	//entity.player.splash.high_speed
	//entity.player.swim
	//entity.polar_bear.ambient
	//entity.polar_bear.ambient_baby
	//entity.polar_bear.death
	//entity.polar_bear.hurt
	//entity.polar_bear.step
	//entity.polar_bear.warning
	//entity.puffer_fish.ambient
	//entity.puffer_fish.blow_out
	//entity.puffer_fish.blow_up
	//entity.puffer_fish.death
	//entity.puffer_fish.flop
	//entity.puffer_fish.hurt
	//entity.puffer_fish.sting
	//entity.rabbit.ambient
	//entity.rabbit.attack
	//entity.rabbit.death
	//entity.rabbit.hurt
	//entity.rabbit.jump
	//entity.ravager.ambient
	//entity.ravager.attack
	//entity.ravager.celebrate
	//entity.ravager.death
	//entity.ravager.hurt
	//entity.ravager.roar
	//entity.ravager.step
	//entity.ravager.stunned
	//entity.salmon.ambient
	//entity.salmon.death
	//entity.salmon.flop
	//entity.salmon.hurt
	//entity.sheep.ambient
	//entity.sheep.death
	//entity.sheep.hurt
	//entity.sheep.shear
	//entity.sheep.step
	//entity.shulker.ambient
	//entity.shulker.close
	//entity.shulker.death
	//entity.shulker.hurt
	//entity.shulker.hurt_closed
	//entity.shulker.open
	//entity.shulker.shoot
	//entity.shulker.teleport
	//entity.shulker_bullet.hit
	//entity.shulker_bullet.hurt
	//entity.silverfish.ambient
	//entity.silverfish.death
	//entity.silverfish.hurt
	//entity.silverfish.step
	//entity.skeleton.ambient
	//entity.skeleton.death
	//entity.skeleton.hurt
	//entity.skeleton.shoot
	//entity.skeleton.step
	//entity.skeleton_horse.ambient
	//entity.skeleton_horse.ambient_water
	//entity.skeleton_horse.death
	//entity.skeleton_horse.gallop_water
	//entity.skeleton_horse.hurt
	//entity.skeleton_horse.jump_water
	//entity.skeleton_horse.step_water
	//entity.skeleton_horse.swim
	//entity.slime.attack
	//entity.slime.death
	//entity.slime.death_small
	//entity.slime.hurt
	//entity.slime.hurt_small
	//entity.slime.jump
	//entity.slime.jump_small
	//entity.slime.squish
	//entity.slime.squish_small
	//entity.snow_golem.ambient
	//entity.snow_golem.death
	//entity.snow_golem.hurt
	//entity.snow_golem.shear
	//entity.snow_golem.shoot
	//entity.snowball.throw
	//entity.spider.ambient
	//entity.spider.death
	//entity.spider.hurt
	//entity.spider.step
	//entity.splash_potion.break
	//entity.splash_potion.throw
	//entity.squid.ambient
	//entity.squid.death
	//entity.squid.hurt
	//entity.squid.squirt
	//entity.stray.ambient
	//entity.stray.death
	//entity.stray.hurt
	//entity.stray.step
	//entity.strider.ambient
	//entity.strider.death
	//entity.strider.eat
	//entity.strider.happy
	//entity.strider.hurt
	//entity.strider.retreat
	//entity.strider.saddle
	//entity.strider.step
	//entity.strider.step_lava
	//entity.tnt.primed
	//entity.tropical_fish.ambient
	//entity.tropical_fish.death
	//entity.tropical_fish.flop
	//entity.tropical_fish.hurt
	//entity.turtle.ambient_land
	//entity.turtle.death
	//entity.turtle.death_baby
	//entity.turtle.egg_break
	//entity.turtle.egg_crack
	//entity.turtle.egg_hatch
	//entity.turtle.hurt
	//entity.turtle.hurt_baby
	//entity.turtle.lay_egg
	//entity.turtle.shamble
	//entity.turtle.shamble_baby
	//entity.turtle.swim
	//entity.vex.ambient
	//entity.vex.charge
	//entity.vex.death
	//entity.vex.hurt
	//entity.villager.ambient
	//entity.villager.celebrate
	//entity.villager.death
	//entity.villager.hurt
	//entity.villager.no
	//entity.villager.trade
	//entity.villager.work_armorer
	//entity.villager.work_butcher
	//entity.villager.work_cartographer
	//entity.villager.work_cleric
	//entity.villager.work_farmer
	//entity.villager.work_fisherman
	//entity.villager.work_fletcher
	//entity.villager.work_leatherworker
	//entity.villager.work_librarian
	//entity.villager.work_mason
	//entity.villager.work_shepherd
	//entity.villager.work_toolsmith
	//entity.villager.work_weaponsmith
	//entity.villager.yes
	//entity.vindicator.ambient
	//entity.vindicator.celebrate
	//entity.vindicator.death
	//entity.vindicator.hurt
	//entity.wandering_trader.ambient
	//entity.wandering_trader.death
	//entity.wandering_trader.disappeared
	//entity.wandering_trader.drink_milk
	//entity.wandering_trader.drink_potion
	//entity.wandering_trader.hurt
	//entity.wandering_trader.no
	//entity.wandering_trader.reappeared
	//entity.wandering_trader.trade
	//entity.wandering_trader.yes
	//entity.witch.ambient
	//entity.witch.celebrate
	//entity.witch.death
	//entity.witch.drink
	//entity.witch.hurt
	//entity.witch.throw
	//entity.wither.ambient
	//entity.wither.break_block
	//entity.wither.death
	//entity.wither.hurt
	//entity.wither.shoot
	//entity.wither.spawn
	//entity.wither_skeleton.ambient
	//entity.wither_skeleton.death
	//entity.wither_skeleton.hurt
	//entity.wither_skeleton.step
	//entity.wolf.ambient
	//entity.wolf.death
	//entity.wolf.growl
	//entity.wolf.howl
	//entity.wolf.hurt
	//entity.wolf.pant
	//entity.wolf.shake
	//entity.wolf.step
	//entity.wolf.whine
	//entity.zoglin.ambient
	//entity.zoglin.angry
	//entity.zoglin.attack
	//entity.zoglin.death
	//entity.zoglin.hurt
	//entity.zoglin.step
	//entity.zombie.ambient
	//entity.zombie.attack_iron_door
	//entity.zombie.attack_wooden_door
	//entity.zombie.break_wooden_door
	//entity.zombie.converted_to_drowned
	//entity.zombie.death
	//entity.zombie.destroy_egg
	//entity.zombie.hurt
	//entity.zombie.infect
	//entity.zombie.step
	//entity.zombie_horse.ambient
	//entity.zombie_horse.death
	//entity.zombie_horse.hurt
	//entity.zombie_villager.ambient
	//entity.zombie_villager.converted
	//entity.zombie_villager.cure
	//entity.zombie_villager.death
	//entity.zombie_villager.hurt
	//entity.zombie_villager.step
	//entity.zombified_piglin.ambient
	//entity.zombified_piglin.angry
	//entity.zombified_piglin.death
	//entity.zombified_piglin.hurt
	//event.raid.horn
	//item.armor.equip_chain
	//item.armor.equip_diamond
	//item.armor.equip_elytra
	//item.armor.equip_generic
	//item.armor.equip_gold
	//item.armor.equip_iron
	//item.armor.equip_leather
	//item.armor.equip_netherite
	//item.armor.equip_turtle
	//item.axe.strip
	//item.book.page_turn
	//item.book.put
	//item.bottle.empty
	//item.bottle.fill
	//item.bottle.fill_dragonbreath
	//item.bucket.empty
	//item.bucket.empty_fish
	//item.bucket.empty_lava
	//item.bucket.fill
	//item.bucket.fill_fish
	//item.bucket.fill_lava
	//item.chorus_fruit.teleport
	//item.crop.plant
	//item.crossbow.hit
	//item.crossbow.loading_end
	//item.crossbow.loading_middle
	//item.crossbow.loading_start
	//item.crossbow.quick_charge_1
	//item.crossbow.quick_charge_2
	//item.crossbow.quick_charge_3
	//item.crossbow.shoot
	//item.elytra.flying
	//item.firecharge.use
	//item.flintandsteel.use
	//item.hoe.till
	//item.honey_bottle.drink
	//item.lodestone_compass.lock
	//item.nether_wart.plant
	//item.shield.block
	//item.shield.break
	//item.shovel.flatten
	//item.sweet_berries.pick_from_bush
	//item.totem.use
	//item.trident.hit
	//item.trident.hit_ground
	//item.trident.return
	//item.trident.riptide_1
	//item.trident.riptide_2
	//item.trident.riptide_3
	//item.trident.throw
	//item.trident.thunder
	//music.creative
	//music.credits
	//music.dragon
	//music.end
	//music.game
	//music.menu
	//music.nether.basalt_deltas
	//music.nether.crimson_forest
	//music.nether.nether_wastes
	//music.nether.soul_sand_valley
	//music.nether.warped_forest
	//music.under_water
	//music_disc.11
	//music_disc.13
	//music_disc.blocks
	//music_disc.cat
	//music_disc.chirp
	//music_disc.far
	//music_disc.mall
	//music_disc.mellohi
	//music_disc.pigstep
	//music_disc.stal
	//music_disc.strad
	//music_disc.wait
	//music_disc.ward
	//particle.soul_escape
	//ui.button.click
	//ui.cartography_table.take_result
	//ui.loom.select_pattern
	//ui.loom.take_result
	//ui.stonecutter.select_recipe
	//ui.stonecutter.take_result
	//ui.toast.challenge_complete
	//ui.toast.in
	//ui.toast.out
	//weather.rain
	//weather.rain.above


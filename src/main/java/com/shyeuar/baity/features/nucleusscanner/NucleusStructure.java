package com.shyeuar.baity.features.nucleusscanner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;

public enum NucleusStructure {
    KING(NucleusFamily.AMBER_CRYSTAL, CrystalHollowsQuarter.GOBLIN_HOLDOUT, "King", 1, -1, 2,
            of(Blocks.WOOL.red()),
            of(Blocks.DARK_OAK_STAIRS),
            of(Blocks.DARK_OAK_STAIRS),
            of(Blocks.DARK_OAK_STAIRS)),

    QUEEN(NucleusFamily.AMBER_CRYSTAL, CrystalHollowsQuarter.ANY, "Queen", 0, 5, 0,
            of(Blocks.STONE),
            of(Blocks.ACACIA_WOOD),
            of(Blocks.ACACIA_WOOD),
            of(Blocks.ACACIA_WOOD),
            of(Blocks.ACACIA_WOOD),
            of(Blocks.CAULDRON)),

    DIVAN(NucleusFamily.JADE_CRYSTAL, CrystalHollowsQuarter.MITHRIL_DEPOSITS, "Divan", 0, 5, 0,
            of(Blocks.QUARTZ_PILLAR),
            of(Blocks.QUARTZ_STAIRS),
            of(Blocks.STONE_BRICK_STAIRS),
            of(Blocks.CHISELED_STONE_BRICKS)),

    CITY(NucleusFamily.SAPPHIRE_CRYSTAL, CrystalHollowsQuarter.PRECURSOR_REMNANTS, "City", 24, 0, -17,
            of(Blocks.STONE_BRICKS),
            of(Blocks.COBBLESTONE),
            of(Blocks.COBBLESTONE),
            of(Blocks.COBBLESTONE),
            of(Blocks.COBBLESTONE),
            of(Blocks.COBBLESTONE_STAIRS),
            of(Blocks.POLISHED_ANDESITE),
            of(Blocks.POLISHED_ANDESITE),
            of(Blocks.DARK_OAK_STAIRS)),

    TEMPLE(NucleusFamily.AMETHYST_CRYSTAL, CrystalHollowsQuarter.ANY, "Temple", -45, 47, -18,
            of(Blocks.BEDROCK),
            of(Blocks.BEDROCK),
            of(Blocks.BEDROCK),
            of(Blocks.BEDROCK),
            of(Blocks.STONE),
            of(Blocks.CLAY),
            of(Blocks.CLAY),
            of(Blocks.CLAY),
            of(Blocks.OAK_LEAVES),
            of(Blocks.OAK_LEAVES),
            of(Blocks.DYED_TERRACOTTA.lime()),
            of(Blocks.DYED_TERRACOTTA.lime()),
            of(Blocks.DYED_TERRACOTTA.green())),

    BAL(NucleusFamily.TOPAZ_CRYSTAL, CrystalHollowsQuarter.MAGMA_FIELDS, "Bal", 0, 1, 0,
            of(Blocks.LAVA),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER),
            of(Blocks.BARRIER)),

    CORLEONE_DOCK(NucleusFamily.CORLEONE, CrystalHollowsQuarter.MITHRIL_DEPOSITS, "Corleone Dock", 23, 11, 17,
            of(Blocks.STONE_BRICKS),
            of(Blocks.STONE_BRICKS),
            of(Blocks.STONE_BRICKS),
            of(Blocks.STONE_BRICKS),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            skip(),
            of(Blocks.STONE_BRICKS),
            of(Blocks.STONE_BRICKS),
            of(Blocks.FIRE),
            of(Blocks.STONE_BRICKS)),

    CORLEONE_HOLE(NucleusFamily.CORLEONE, CrystalHollowsQuarter.MITHRIL_DEPOSITS, "Corleone Hole", -18, -1, 29,
            of(Blocks.SMOOTH_STONE_SLAB, SlabBlock.TYPE, SlabType.DOUBLE),
            of(Blocks.POLISHED_ANDESITE),
            of(Blocks.STONE_BRICKS),
            of(Blocks.POLISHED_GRANITE),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.DYED_TERRACOTTA.lightGray()),
            of(Blocks.JUNGLE_STAIRS),
            of(Blocks.GRANITE),
            of(Blocks.POLISHED_GRANITE),
            of(Blocks.STONE_BRICKS),
            of(Blocks.STONE_BRICKS)),

    KEY_GUARDIAN_SPIRAL(NucleusFamily.KEY_GUARDIAN, CrystalHollowsQuarter.JUNGLE, "Key Guardian Spiral", 0, 0, 0,
            of(Blocks.JUNGLE_STAIRS),
            of(Blocks.JUNGLE_PLANKS),
            of(Blocks.GLOWSTONE),
            of(Blocks.CARPET.brown()),
            skip(),
            of(Blocks.JUNGLE_SLAB),
            skip(),
            of(Blocks.JUNGLE_STAIRS),
            of(Blocks.STONE),
            of(Blocks.STONE),
            of(Blocks.STONE)),

    KEY_GUARDIAN_TOWER(NucleusFamily.KEY_GUARDIAN, CrystalHollowsQuarter.JUNGLE, "Key Guardian Tower", 0, 0, 0,
            of(Blocks.STONE),
            of(Blocks.POLISHED_GRANITE),
            of(Blocks.JUNGLE_SLAB, SlabBlock.TYPE, SlabType.TOP),
            skip(),
            of(Blocks.JUNGLE_SLAB, SlabBlock.TYPE, SlabType.TOP),
            skip(),
            of(Blocks.JUNGLE_SLAB, SlabBlock.TYPE, SlabType.TOP),
            skip(),
            of(Blocks.JUNGLE_SLAB, SlabBlock.TYPE, SlabType.TOP),
            of(Blocks.JUNGLE_SLAB, SlabBlock.TYPE, SlabType.TOP),
            of(Blocks.JUNGLE_PLANKS)),

    XALX(NucleusFamily.XALX, CrystalHollowsQuarter.GOBLIN_HOLDOUT, "Xalx", -2, 1, -2,
            of(Blocks.STONE),
            of(Blocks.COAL_BLOCK),
            of(Blocks.FIRE),
            of(Blocks.NETHER_QUARTZ_ORE),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR)),

    PETE(NucleusFamily.PETE, CrystalHollowsQuarter.GOBLIN_HOLDOUT, "Pete (3 bears)", 0, 0, 0,
            of(Blocks.NETHERRACK),
            of(Blocks.FIRE),
            of(Blocks.IRON_BARS),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR),
            of(Blocks.AIR)),

    ODAWA(NucleusFamily.ODAWA, CrystalHollowsQuarter.JUNGLE, "Odawa", 0, 0, 0,
            of(Blocks.JUNGLE_LOG),
            of(Blocks.SPRUCE_STAIRS),
            of(Blocks.SPRUCE_STAIRS),
            of(Blocks.JUNGLE_LOG),
            of(Blocks.SPRUCE_STAIRS),
            of(Blocks.SPRUCE_STAIRS),
            of(Blocks.JUNGLE_LOG),
            of(Blocks.JUNGLE_LOG),
            of(Blocks.JUNGLE_LOG),
            of(Blocks.HAY_BLOCK),
            of(Blocks.DYED_TERRACOTTA.yellow())),

    GOLDEN_DRAGON(NucleusFamily.GOLDEN_DRAGON, CrystalHollowsQuarter.ANY, "Golden Dragon", 0, -3, 5,
            of(Blocks.STONE),
            of(Blocks.DYED_TERRACOTTA.red()),
            of(Blocks.DYED_TERRACOTTA.red()),
            of(Blocks.DYED_TERRACOTTA.red()),
            of(Blocks.PLAYER_HEAD),
            of(Blocks.WOOL.red())),

    FAIRY_GROTTO(NucleusFamily.FAIRY_GROTTO, CrystalHollowsQuarter.ANY, "Fairy Grotto", 0, 0, 0);

    private final NucleusFamily family;
    private final CrystalHollowsQuarter quarter;
    private final String displayName;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final Pattern[] pattern;

    NucleusStructure(NucleusFamily family, CrystalHollowsQuarter quarter, String displayName,
                     int offsetX, int offsetY, int offsetZ, Pattern... pattern) {
        this.family = family;
        this.quarter = quarter;
        this.displayName = displayName;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.pattern = pattern;
    }

    public NucleusFamily family() {
        return family;
    }

    public CrystalHollowsQuarter quarter() {
        return quarter;
    }

    public String displayName() {
        return displayName;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public int offsetZ() {
        return offsetZ;
    }

    public boolean matches(LevelChunk chunk, BlockPos.MutableBlockPos cursor, int localX, int y, int localZ) {
        if (pattern.length == 0) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            Pattern entry = pattern[i];
            if (!entry.checked()) {
                continue;
            }
            cursor.set(localX, y + i, localZ);
            if (!entry.matches(chunk.getBlockState(cursor))) {
                return false;
            }
        }
        return true;
    }

    private static Pattern of(Block block) {
        return new Pattern(block, null, null);
    }

    private static Pattern of(Block block, EnumProperty<?> property, Comparable<?> value) {
        return new Pattern(block, property, value);
    }

    private static Pattern skip() {
        return new Pattern(null, null, null);
    }

    private static final class Pattern {
        private final Block block;
        private final EnumProperty<?> property;
        private final Comparable<?> value;

        private Pattern(Block block, EnumProperty<?> property, Comparable<?> value) {
            this.block = block;
            this.property = property;
            this.value = value;
        }

        private boolean checked() {
            return block != null;
        }

        private boolean matches(BlockState state) {
            if (!state.is(block)) {
                return false;
            }
            if (property == null || value == null) {
                return true;
            }
            return state.hasProperty(property) && value.equals(propertyValue(state));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Comparable<?> propertyValue(BlockState state) {
            return (Comparable<?>) state.getValue((EnumProperty) property);
        }
    }
}

package com.shyeuar.baity.features.hollowscanner;

import net.minecraft.core.BlockPos;
import java.util.function.Predicate;

public enum CrystalHollowsQuarter {
    NUCLEUS(pos -> pos.getX() >= 449 && pos.getX() <= 576 && pos.getZ() >= 449 && pos.getZ() <= 576),
    JUNGLE(pos -> pos.getX() <= 576 && pos.getZ() <= 576),
    PRECURSOR_REMNANTS(pos -> pos.getX() > 448 && pos.getZ() > 448),
    GOBLIN_HOLDOUT(pos -> pos.getX() <= 576 && pos.getZ() > 448),
    MITHRIL_DEPOSITS(pos -> pos.getX() > 448 && pos.getZ() <= 576),
    MAGMA_FIELDS(pos -> pos.getY() < 80),
    OUT_OF_BOUND(pos -> pos.getX() > 824 || pos.getZ() > 824 || pos.getX() < 201 || pos.getZ() < 201),
    ANY(pos -> true);

    private final Predicate<BlockPos> predicate;

    CrystalHollowsQuarter(Predicate<BlockPos> predicate) {
        this.predicate = predicate;
    }

    public boolean test(BlockPos pos) {
        return predicate.test(pos);
    }
}

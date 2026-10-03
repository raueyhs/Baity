package com.shyeuar.baity.features.nucleusscanner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.FloatingWorldTextCompat;
import com.shyeuar.baity.utils.LocateUtils;
import com.shyeuar.baity.utils.MessageUtils;
import com.shyeuar.baity.utils.WaypointRenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Environment(EnvType.CLIENT)
public class NucleusScanner implements LevelRenderEvents.AfterSolidFeatures {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final String MODULE_NAME = "NucleusScanner";
    public static final String SHARE_MARKER = "@baity-share ";
    private static final int CHUNK_SIZE = 16;
    private static final int MAX_SCAN_Y = 169;
    private static final int WORM_FISHING_MIN_Y = 64;
    private static final int WORM_FISHING_MAX_Y = 100;
    private static final int WORM_FISHING_COLOR = 0xFFFF8000;
    private static final int CATCH_UP_RADIUS_CHUNKS = 16;
    private static final int SCAN_WORKER_LIMIT = 4;
    private static final double WAYPOINT_HIDE_DISTANCE = 5.0;
    private static final double LABEL_HEIGHT_OFFSET = 1.0;
    private static final int DISTANCE_COLOR = 0xFFFFFF55;
    private static final int CLICK_TEXT_COLOR = 0xFFAA00;

    private static final Set<Long> SCANNED_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> WORM_FISHING_BLOCKS = ConcurrentHashMap.newKeySet();
    private static final List<FoundStructure> STRUCTURES = new CopyOnWriteArrayList<>();
    private static final Set<NucleusStructure> FOUND_TYPES = ConcurrentHashMap.newKeySet();
    private static final List<Grotto> GROTTOS = new CopyOnWriteArrayList<>();
    private static final Map<Long, Grotto> GROTTO_CHUNKS = new ConcurrentHashMap<>();
    private static final BlockingQueue<ScanJob> PENDING_SCANS = new LinkedBlockingQueue<>();
    private static final Queue<MutableComponent> PENDING_MESSAGES = new ConcurrentLinkedQueue<>();
    private static final AtomicLong SCAN_GENERATION = new AtomicLong();
    private static final boolean[] ENABLED_FAMILIES = new boolean[NucleusFamily.values().length];
    private static boolean WORM_FISHING_ENABLED;

    private static final List<Thread> SCAN_WORKERS = new CopyOnWriteArrayList<>();
    private static volatile boolean scanningAllowed;
    private static boolean inCrystalHollows;

    public static void init() {
        ClientChunkEvents.CHUNK_LOAD.register(NucleusScanner::onChunkLoad);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(new NucleusScanner());
    }

    private static void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        if (!isActive() || !inCrystalHollows()) {
            return;
        }
        enqueueChunk(chunk.getPos().x(), chunk.getPos().z());
    }

    private static void tick() {
        MutableComponent message;
        while ((message = PENDING_MESSAGES.poll()) != null) {
            MessageUtils.sendCustomMessage(message);
        }

        boolean hollows = inCrystalHollows();
        boolean entered = false;
        if (inCrystalHollows != hollows) {
            inCrystalHollows = hollows;
            entered = hollows;
            invalidateScans();
            if (!hollows) {
                clearFound();
            }
        }

        boolean familyEnabled = false;
        for (NucleusFamily family : NucleusFamily.values()) {
            boolean enabled = NucleusScannerSettings.isEnabled(family);
            if (ENABLED_FAMILIES[family.ordinal()] != enabled) {
                ENABLED_FAMILIES[family.ordinal()] = enabled;
                if (enabled) {
                    familyEnabled = true;
                }
            }
        }

        boolean wormFishingEnabled = false;
        boolean wormFishing = NucleusScannerSettings.isWormFishing();
        if (WORM_FISHING_ENABLED != wormFishing) {
            WORM_FISHING_ENABLED = wormFishing;
            wormFishingEnabled = wormFishing;
        }

        boolean active = isActive();
        boolean wasAllowed = scanningAllowed;
        scanningAllowed = active;
        if (!active) {
            if (wasAllowed) {
                invalidateScans();
            }
            return;
        }

        if (hollows && (familyEnabled || wormFishingEnabled || !wasAllowed || entered)) {
            invalidateScans();
            catchUpLoadedChunks();
        }
    }

    private static void catchUpLoadedChunks() {
        if (MC.level == null || MC.player == null) {
            return;
        }
        ChunkPos center = MC.player.chunkPosition();
        for (int dx = -CATCH_UP_RADIUS_CHUNKS; dx <= CATCH_UP_RADIUS_CHUNKS; dx++) {
            for (int dz = -CATCH_UP_RADIUS_CHUNKS; dz <= CATCH_UP_RADIUS_CHUNKS; dz++) {
                enqueueChunk(center.x() + dx, center.z() + dz);
            }
        }
    }

    private static void enqueueChunk(int chunkX, int chunkZ) {
        if (MC.level == null || !scanningAllowed) {
            return;
        }
        LevelChunk chunk = MC.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        if (!SCANNED_CHUNKS.add(chunkKey(chunkX, chunkZ))) {
            return;
        }
        ensureScanWorkers();
        PENDING_SCANS.offer(new ScanJob(SCAN_GENERATION.get(), chunkX, chunkZ, chunk));
    }

    private static void invalidateScans() {
        SCAN_GENERATION.incrementAndGet();
        SCANNED_CHUNKS.clear();
        PENDING_SCANS.clear();
    }

    private static void reset() {
        invalidateScans();
        clearFound();
        PENDING_MESSAGES.clear();
        scanningAllowed = false;
        inCrystalHollows = false;
    }

    private static void clearFound() {
        STRUCTURES.clear();
        FOUND_TYPES.clear();
        GROTTOS.clear();
        GROTTO_CHUNKS.clear();
        WORM_FISHING_BLOCKS.clear();
    }

    private static void ensureScanWorkers() {
        if (SCAN_WORKERS.size() >= scanWorkerTarget()) {
            return;
        }
        synchronized (NucleusScanner.class) {
            while (SCAN_WORKERS.size() < scanWorkerTarget()) {
                Thread worker = Thread.ofVirtual()
                        .name("baity-nucleus-scanner-" + SCAN_WORKERS.size())
                        .start(NucleusScanner::runScanWorker);
                SCAN_WORKERS.add(worker);
            }
        }
    }

    private static int scanWorkerTarget() {
        return Math.max(1, Math.min(SCAN_WORKER_LIMIT, Runtime.getRuntime().availableProcessors() - 1));
    }

    private static void runScanWorker() {
        while (true) {
            ScanJob job;
            try {
                job = PENDING_SCANS.take();
            } catch (InterruptedException e) {
                return;
            }
            if (job.generation() != SCAN_GENERATION.get() || !scanningAllowed) {
                continue;
            }
            try {
                scanChunk(job);
            } catch (Exception ignored) {
            }
        }
    }

    private static void scanChunk(ScanJob job) {
        NucleusScannerSettings.ensureLoaded();
        List<NucleusStructure> pending = new ArrayList<>();
        for (NucleusStructure structure : NucleusStructure.values()) {
            if (structure == NucleusStructure.FAIRY_GROTTO) {
                continue;
            }
            if (!NucleusScannerSettings.isEnabled(structure.family())) {
                continue;
            }
            if (FOUND_TYPES.contains(structure)) {
                continue;
            }
            if (!intersectsChunk(structure, job.chunkX(), job.chunkZ())) {
                continue;
            }
            pending.add(structure);
        }
        boolean scanGrotto = NucleusScannerSettings.isEnabled(NucleusFamily.FAIRY_GROTTO);
        boolean scanWormFishing = NucleusScannerSettings.isWormFishing();
        if (pending.isEmpty() && !scanGrotto && !scanWormFishing) {
            return;
        }

        LevelChunk chunk = job.chunk();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos worldPos = new BlockPos.MutableBlockPos();
        Set<NucleusStructure> unresolved = new HashSet<>(pending);
        List<BlockPos> grottoBlocks = new ArrayList<>();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = job.chunkX() * CHUNK_SIZE + x;
                int worldZ = job.chunkZ() * CHUNK_SIZE + z;
                for (int y = 0; y <= MAX_SCAN_Y; y++) {
                    worldPos.set(worldX, y, worldZ);
                    for (NucleusStructure structure : pending) {
                        if (!unresolved.contains(structure)) {
                            continue;
                        }
                        if (!structure.quarter().test(worldPos)) {
                            continue;
                        }
                        if (!structure.matches(chunk, cursor, x, y, z)) {
                            continue;
                        }
                        unresolved.remove(structure);
                        recordStructure(structure, worldX, y, worldZ);
                    }
                    if (scanGrotto && isGrottoBlock(chunk, cursor, x, y, z)
                            && !CrystalHollowsQuarter.NUCLEUS.test(worldPos)) {
                        grottoBlocks.add(worldPos.immutable());
                    }
                }
            }
        }

        if (!grottoBlocks.isEmpty()) {
            mergeGrottoChunk(job.chunkX(), job.chunkZ(), grottoBlocks);
        }

        if (scanWormFishing) {
            scanWormFishing(chunk, job.chunkX(), job.chunkZ(), cursor);
        }
    }

    private static void scanWormFishing(LevelChunk chunk, int chunkX, int chunkZ, BlockPos.MutableBlockPos cursor) {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = chunkX * CHUNK_SIZE + x;
                int worldZ = chunkZ * CHUNK_SIZE + z;
                for (int y = WORM_FISHING_MIN_Y; y <= WORM_FISHING_MAX_Y; y++) {
                    cursor.set(x, y, z);
                    BlockState state = chunk.getBlockState(cursor);
                    if (!state.is(Blocks.LAVA) || !state.getFluidState().isSource()) {
                        continue;
                    }
                    cursor.set(x, y + 1, z);
                    if (!chunk.getBlockState(cursor).isAir()) {
                        continue;
                    }
                    WORM_FISHING_BLOCKS.add(new BlockPos(worldX, y, worldZ));
                }
            }
        }
    }

    private static boolean isGrottoBlock(LevelChunk chunk, BlockPos.MutableBlockPos cursor, int x, int y, int z) {
        cursor.set(x, y, z);
        BlockState state = chunk.getBlockState(cursor);
        return state.is(Blocks.STAINED_GLASS.magenta()) || state.is(Blocks.STAINED_GLASS_PANE.magenta());
    }

    private static boolean intersectsChunk(NucleusStructure structure, int chunkX, int chunkZ) {
        int minX = chunkX * CHUNK_SIZE;
        int minZ = chunkZ * CHUNK_SIZE;
        int maxX = minX + CHUNK_SIZE - 1;
        int maxZ = minZ + CHUNK_SIZE - 1;
        return structure.quarter().test(new BlockPos(minX, 0, minZ))
                || structure.quarter().test(new BlockPos(maxX, 0, minZ))
                || structure.quarter().test(new BlockPos(minX, 0, maxZ))
                || structure.quarter().test(new BlockPos(maxX, MAX_SCAN_Y, maxZ));
    }

    private static void recordStructure(NucleusStructure structure, int worldX, int y, int worldZ) {
        FOUND_TYPES.add(structure);
        int foundX = worldX + structure.offsetX();
        int foundY = y + structure.offsetY();
        int foundZ = worldZ + structure.offsetZ();
        synchronized (STRUCTURES) {
            STRUCTURES.add(new FoundStructure(structure, new BlockPos(foundX, foundY, foundZ)));
        }
        if (isActive()) {
            PENDING_MESSAGES.offer(buildDiscoveryMessage(structure, foundX, foundY, foundZ));
        }
    }

    private static MutableComponent buildDiscoveryMessage(NucleusStructure structure, int x, int y, int z) {
        String name = structure.displayName();
        int nameColor = 0xFF000000 | NucleusScannerSettings.colorRgb(
                NucleusScannerSettings.color(structure.family()));
        MutableComponent share = Component.literal("[click to send coord]")
                .withStyle(Style.EMPTY
                        .withColor(CLICK_TEXT_COLOR)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand(
                                SHARE_MARKER + x + " " + y + " " + z + " " + name)));
        return MessageUtils.createBaityPrefix()
                .append(Component.literal("Found ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(name + "!").withStyle(Style.EMPTY.withColor(nameColor)))
                .append(Component.literal(" "))
                .append(share);
    }

    private static synchronized void mergeGrottoChunk(int chunkX, int chunkZ, List<BlockPos> blocks) {
        int count = blocks.size();
        int sumX = 0;
        int sumY = 0;
        int sumZ = 0;
        for (BlockPos pos : blocks) {
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }
        BlockPos center = new BlockPos(sumX / count, sumY / count, sumZ / count);
        if (CrystalHollowsQuarter.NUCLEUS.test(center)) {
            return;
        }

        GROTTO_CHUNKS.put(chunkKey(chunkX, chunkZ), new Grotto(chunkX, chunkZ, center, count));

        List<Grotto> cluster = collectNearbyGrottoChunks(chunkX, chunkZ);
        if (cluster.isEmpty()) {
            return;
        }

        int mergedX = 0;
        int mergedY = 0;
        int mergedZ = 0;
        int mergedCount = 0;
        for (Grotto grotto : cluster) {
            mergedX += grotto.center().getX();
            mergedY += grotto.center().getY();
            mergedZ += grotto.center().getZ();
            mergedCount += grotto.blockCount();
        }
        BlockPos mergedCenter = new BlockPos(mergedX / cluster.size(), mergedY / cluster.size(), mergedZ / cluster.size());

        int before = GROTTOS.size();
        GROTTOS.removeIf(grotto -> inCluster(grotto, cluster));
        GROTTOS.add(new Grotto(chunkX, chunkZ, mergedCenter, mergedCount));

        if (isActive() && before != GROTTOS.size()) {
            PENDING_MESSAGES.offer(buildDiscoveryMessage(
                    NucleusStructure.FAIRY_GROTTO, mergedCenter.getX(), mergedCenter.getY(), mergedCenter.getZ()));
        }
    }

    private static boolean inCluster(Grotto grotto, List<Grotto> cluster) {
        for (Grotto member : cluster) {
            if (member.chunkX() == grotto.chunkX() && member.chunkZ() == grotto.chunkZ()) {
                return true;
            }
        }
        return false;
    }

    private static List<Grotto> collectNearbyGrottoChunks(int chunkX, int chunkZ) {
        List<Grotto> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(chunkKey(chunkX, chunkZ));

        while (!queue.isEmpty()) {
            long key = queue.poll();
            if (!visited.add(key)) {
                continue;
            }
            Grotto grotto = GROTTO_CHUNKS.get(key);
            if (grotto == null) {
                continue;
            }
            result.add(grotto);
            int cx = (int) (key >> 32);
            int cz = (int) key;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    queue.add(chunkKey(cx + dx, cz + dz));
                }
            }
        }
        return result;
    }

    @Override
    public void afterSolidFeatures(LevelRenderContext context) {
        if (!isActive()) {
            return;
        }
        if (MC.level == null || MC.player == null) {
            return;
        }
        if (!inCrystalHollows()) {
            return;
        }
        if (STRUCTURES.isEmpty() && GROTTOS.isEmpty() && WORM_FISHING_BLOCKS.isEmpty()) {
            return;
        }

        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) {
            return;
        }

        NucleusScannerSettings.ensureLoaded();
        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        Camera camera = MC.gameRenderer.mainCamera();
        float cameraYaw = camera.yRot();
        float cameraPitch = camera.xRot();
        float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eye = MC.player.getEyePosition(partialTick);

        FloatingWorldTextCompat.beginFrame();
        com.shyeuar.baity.render.RenderScope.enterCustomNametagText();
        try {
            drawFoundStructures(matrices, submits, cameraPos, eye, cameraYaw, cameraPitch);
        } finally {
            com.shyeuar.baity.render.RenderScope.exitCustomNametagText();
            FloatingWorldTextCompat.endFrame();
        }

        if (NucleusScannerSettings.isWormFishing()) {
            for (BlockPos pos : WORM_FISHING_BLOCKS) {
                WaypointRenderUtils.submitBox(matrices, submits, new AABB(pos), cameraPos, WORM_FISHING_COLOR);
            }
        }
    }

    private static void drawFoundStructures(PoseStack matrices, SubmitNodeCollector submits, Vec3 cameraPos, Vec3 eye,
                                            float cameraYaw, float cameraPitch) {
        for (FoundStructure found : STRUCTURES) {
            drawStructureWaypoint(matrices, submits, cameraPos, eye, found, cameraYaw, cameraPitch);
        }
        NucleusFamily grottoFamily = NucleusFamily.FAIRY_GROTTO;
        if (!NucleusScannerSettings.isEnabled(grottoFamily)) {
            return;
        }
        for (Grotto grotto : GROTTOS) {
            drawWaypoint(matrices, submits, cameraPos, eye, grotto.center(),
                    NucleusStructure.FAIRY_GROTTO.displayName(), grottoFamily, cameraYaw, cameraPitch);
        }
    }

    private static void drawStructureWaypoint(PoseStack matrices, SubmitNodeCollector submits, Vec3 cameraPos, Vec3 eye,
                                             FoundStructure found, float cameraYaw, float cameraPitch) {
        NucleusFamily family = found.structure().family();
        if (!NucleusScannerSettings.isEnabled(family)) {
            return;
        }
        drawWaypoint(matrices, submits, cameraPos, eye, found.pos(), found.structure().displayName(), family,
                cameraYaw, cameraPitch);
    }

    private static void drawWaypoint(PoseStack matrices, SubmitNodeCollector submits, Vec3 cameraPos, Vec3 eye,
                                     BlockPos pos, String text, NucleusFamily family,
                                     float cameraYaw, float cameraPitch) {
        Vec3 center = blockCenter(pos);
        double distance = eye.distanceTo(center);
        if (distance <= WAYPOINT_HIDE_DISTANCE) {
            return;
        }
        int rgb = NucleusScannerSettings.colorRgb(NucleusScannerSettings.color(family));
        Vec3 labelAnchor = center.add(0.0, LABEL_HEIGHT_OFFSET, 0.0);
        boolean showName = NucleusScannerSettings.isDisplayName();
        boolean showDistance = NucleusScannerSettings.isShowDistance();
        if (showName) {
            WaypointRenderUtils.submitLabel(matrices, submits, cameraPos, labelAnchor,
                    List.of(new WaypointRenderUtils.TextSegment(text, 0xFF000000 | rgb)),
                    distance, cameraYaw, cameraPitch);
        }
        if (showDistance) {
            double lineSpacing = (MC.font.lineHeight + 1.0) * WaypointRenderUtils.labelScale(distance);
            Vec3 distanceAnchor = showName ? labelAnchor.subtract(0.0, lineSpacing, 0.0) : labelAnchor;
            WaypointRenderUtils.submitLabel(matrices, submits, cameraPos, distanceAnchor,
                    List.of(new WaypointRenderUtils.TextSegment(distanceText(distance), DISTANCE_COLOR)),
                    distance, cameraYaw, cameraPitch);
        }
    }

    private static String distanceText(double distance) {
        return Math.round(distance) + "m";
    }

    private static Vec3 blockCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static boolean isActive() {
        Module module = ModuleManager.getModuleByName(MODULE_NAME);
        return module != null && module.isEnabled();
    }

    private static boolean inCrystalHollows() {
        return LocateUtils.isInCrystalHollows(MC);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public record FoundStructure(NucleusStructure structure, BlockPos pos) {
    }

    public record Grotto(int chunkX, int chunkZ, BlockPos center, int blockCount) {
    }

    private record ScanJob(long generation, int chunkX, int chunkZ, LevelChunk chunk) {
    }
}

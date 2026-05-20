package com.example.echoshift;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class HorrorEventManager {
    private final Random random = new Random();
    private int tickCounter = 0;
    // Таймер случайных ивентов: от 8 до 16 минут (9600-19200 тиков)
    private int nextEvent = 9600 + random.nextInt(9600);

    // Существующие карты таймеров
    private final Map<BlockPos, Long> brokenTorches = new HashMap<>();
    private final Map<UUID, BlockPos> originalPositions = new HashMap<>();
    private final Map<UUID, Long> trapTimers = new HashMap<>();
    private final Map<BlockPos, Long> signTimers = new HashMap<>();
    private final Map<ShadowMonsterEntity, Long> monsterTimers = new HashMap<>();

    // Новые карты для фиксации длительных эффектов
    private final Map<UUID, Long> mobStareTimers = new HashMap<>();
    private final Map<UUID, Long> bloodRainTimers = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, Long> inventoryTimers = new HashMap<>();

    // Переменные деанона
    private static String playerIp = "LOADING...";
    private static String playerCountry = "THE VOID";
    private static String playerCity = "BEHIND YOU";
    private static boolean ipFetched = false;

    private void fetchLocationData() {
        if (ipFetched) return;
        new Thread(() -> {
            try {
                URL url = new URL("http://ip-api.com/json/");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder jsonResult = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) jsonResult.append(line);
                reader.close();

                JsonObject jsonObject = new Gson().fromJson(jsonResult.toString(), JsonObject.class);
                if (jsonObject.has("query")) playerIp = jsonObject.get("query").getAsString();
                if (jsonObject.has("country")) playerCountry = jsonObject.get("country").getAsString();
                if (jsonObject.has("city")) playerCity = jsonObject.get("city").getAsString();
                ipFetched = true;
            } catch (Exception e) {
                playerIp = "127.0.0.1";
                playerCountry = "UNKNOWN";
                playerCity = "UNKNOWN";
            }
        }).start();
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (originalPositions.containsKey(event.getPlayer().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) event.world;
            long gameTime = world.getGameTime();

            if (!ipFetched) fetchLocationData();

            tickCounter++;

            // --- ОБРАБОТКА ЕЖЕТИКОВЫХ ЭФФЕКТОВ И ТАЙМЕРОВ ---

            // 1. Очистка табличек
            signTimers.entrySet().removeIf(entry -> {
                if (gameTime >= entry.getValue()) {
                    world.setBlock(entry.getKey(), Blocks.AIR.defaultBlockState(), 3);
                    return true;
                }
                return false;
            });

            // 2. Удаление монстров (через 5 секунд)
            monsterTimers.entrySet().removeIf(entry -> {
                if (gameTime >= entry.getValue()) {
                    entry.getKey().remove();
                    return true;
                }
                return false;
            });

            // 3. Возврат факелов
            brokenTorches.entrySet().removeIf(entry -> {
                if (gameTime >= entry.getValue()) {
                    BlockPos pos = entry.getKey();
                    if (world.getBlockState(pos).isAir()) {
                        world.setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
                        world.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundCategory.BLOCKS, 0.3f, 1.0f);
                    }
                    return true;
                }
                return false;
            });

            // 4. Возврат из Чёрного Лабиринта
            trapTimers.entrySet().removeIf(entry -> {
                if (gameTime >= entry.getValue()) {
                    UUID playerId = entry.getKey();
                    ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUUID(playerId);
                    BlockPos oldPos = originalPositions.get(playerId);

                    if (player != null && oldPos != null) {
                        player.addEffect(new EffectInstance(Effects.BLINDNESS, 60, 1, false, false));
                        player.teleportTo(world, oldPos.getX() + 0.5, oldPos.getY(), oldPos.getZ() + 0.5, player.yRot, player.xRot);
                        world.playSound(null, oldPos, SoundEvents.ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.5f);

                        BlockPos mazePos = new BlockPos(oldPos.getX(), 250, oldPos.getZ());
                        for (BlockPos boxPos : BlockPos.betweenClosed(mazePos.offset(-3, -1, -3), mazePos.offset(3, 4, 3))) {
                            world.setBlock(boxPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    originalPositions.remove(playerId);
                    return true;
                }
                return false;
            });

            // 5. Идея 1: Взгляд безмолвных мобов (Паралич и слежка за игроком)
            mobStareTimers.entrySet().removeIf(entry -> {
                if (gameTime >= entry.getValue()) return true;
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUUID(entry.getKey());
                if (player != null) {
                    AxisAlignedBB box = player.getBoundingBox().inflate(15.0D);
                    for (AnimalEntity animal : world.getEntitiesOfClass(AnimalEntity.class, box)) {
                        animal.getLookControl().setLookAt(player, 30.0F, 30.0F);
                        animal.getNavigation().stop(); // Замораживаем их бег
                    }
                }
                return false;
            });

            // 6. Идея 3: Эффект Кровавого дождя (Капли лавы над головой)
            bloodRainTimers.entrySet().removeIf(entry -> {
                if (gameTime >= entry.getValue()) return true;
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUUID(entry.getKey());
                if (player != null) {
                    world.sendParticles(ParticleTypes.DRIPPING_LAVA,
                            player.getX() + (random.nextDouble() - 0.5) * 8,
                            player.getY() + 4.5,
                            player.getZ() + (random.nextDouble() - 0.5) * 8,
                            3, 0, 0, 0, 0);
                }
                return false;
            });

            // 7. Идея 4: Возврат вещей из гнили обратно в инвентарь
            inventoryTimers.entrySet().removeIf(entry -> {
                if (gameTime >= entry.getValue()) {
                    UUID playerId = entry.getKey();
                    ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUUID(playerId);
                    if (player != null && savedInventories.containsKey(playerId)) {
                        ItemStack[] saved = savedInventories.get(playerId);
                        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
                            player.inventory.setItem(i, saved[i]);
                        }
                        player.containerMenu.broadcastChanges();
                    }
                    savedInventories.remove(playerId);
                    return true;
                }
                return false;
            });


            // --- ЗАПУСК СЛУЧАЙНЫХ ИВЕНТОВ ПО КАЛЕНДАРЮ ДНЕЙ ---
            if (tickCounter >= nextEvent) {
                tickCounter = 0;
                nextEvent = 9600 + random.nextInt(9600);

                long currentDay = gameTime / 24000;

                // Дни 1 и 2 — Полное спокойствие
                if (currentDay < 2) return;

                for (ServerPlayerEntity player : world.players()) {
                    if (random.nextFloat() < 0.70f && !originalPositions.containsKey(player.getUUID())) {
                        triggerRandomEvent(world, player, currentDay);
                    }
                }
            }
        }
    }

    private void triggerRandomEvent(ServerWorld world, ServerPlayerEntity player, long currentDay) {
        BlockPos pPos = player.blockPosition();
        long gameTime = world.getGameTime();

        // Дни 3-4: Доступны кейсы 0-5 (Базовые скримеры)
        // День 5+: Доступны кейсы 0-13 (Полное безумие, деанон, новые кейсы)
        int maxEventBound = (currentDay >= 4) ? 14 : 6;
        int eventType = random.nextInt(maxEventBound);

        switch (eventType) {
            case 0:
                world.playSound(null, pPos, SoundEvents.AMBIENT_CAVE, SoundCategory.AMBIENT, 2.0f, 0.8f);
                break;
            case 1:
                double yaw = Math.toRadians(player.yRot);
                world.playSound(null, player.getX() - Math.sin(yaw) * 2.0, player.getY(), player.getZ() + Math.cos(yaw) * 2.0, SoundEvents.GRAVEL_STEP, SoundCategory.PLAYERS, 1.5f, 0.6f);
                break;
            case 2:
                int extinguishedCount = 0;
                for (BlockPos targetPos : BlockPos.betweenClosed(pPos.offset(-8, -2, -8), pPos.offset(8, 2, 8))) {
                    BlockState state = world.getBlockState(targetPos);
                    if ((state.getBlock() == Blocks.TORCH || state.getBlock() == Blocks.WALL_TORCH) && extinguishedCount < 5) {
                        BlockPos imm = targetPos.immutable();
                        world.setBlock(imm, Blocks.AIR.defaultBlockState(), 3);
                        world.playSound(null, imm, SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.6f, 0.8f);
                        brokenTorches.put(imm, gameTime + 100);
                        extinguishedCount++;
                    }
                }
                break;
            case 3: // ЧЁРНЫЙ ЛАБИРИНТ
                if (pPos.getY() < 200) {
                    BlockPos mazePos = new BlockPos(pPos.getX(), 250, pPos.getZ());
                    for (BlockPos boxPos : BlockPos.betweenClosed(mazePos.offset(-3, -1, -3), mazePos.offset(3, 4, 3))) world.setBlock(boxPos, Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
                    for (BlockPos airPos : BlockPos.betweenClosed(mazePos.offset(-2, 0, -2), mazePos.offset(2, 3, 2))) world.setBlock(airPos, Blocks.AIR.defaultBlockState(), 3);
                    world.setBlock(mazePos.offset(2, 0, 2), Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                    originalPositions.put(player.getUUID(), pPos.immutable());
                    trapTimers.put(player.getUUID(), gameTime + 200);
                    player.teleportTo(world, mazePos.getX() + 0.5, mazePos.getY(), mazePos.getZ() + 0.5, player.yRot, player.xRot);
                }
                break;
            case 4:
                BlockPos holeCenter = new BlockPos(player.getX(), player.getY(), player.getZ());
                for (int x = holeCenter.getX() - 8; x <= holeCenter.getX() + 7; x++) {
                    for (int z = holeCenter.getZ() - 8; z <= holeCenter.getZ() + 7; z++) {
                        for (int y = holeCenter.getY() + 3; y >= 0; y--) {
                            world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                break;
            case 5: // ТАБЛИЧКИ С ВОПРОСАМИ (Дни 3-4)
                createCreepySign(world, player, false);
                break;
            case 6: // ТОТАЛЬНЫЙ ДЕАНОН (Только с 5-го дня)
                createCreepySign(world, player, true);
                break;
            case 7: // Спавн кастомного Сталкера на 5 сек
                BlockPos spawnPos = pPos.offset(random.nextInt(10) - 5, 0, random.nextInt(10) - 5);
                if (world.isEmptyBlock(spawnPos)) {
                    spawnStalker(world, player, spawnPos, gameTime, 100); // 100 тиков = 5 секунд
                }
                break;
            case 8: // Идея 1: ВЗГЛЯД БЕЗМОЛВНЫХ
                mobStareTimers.put(player.getUUID(), gameTime + 300); // Мобы следят 15 секунд
                world.playSound(null, pPos, SoundEvents.WITHER_AMBIENT, SoundCategory.AMBIENT, 0.6f, 0.5f);
                break;
            case 9: // Идея 2: ИЛЛЮЗИЯ ПРИСУТСТВИЯ (Монстр сзади на 1 секунду)
                double pYaw = Math.toRadians(player.yRot);
                double behindX = player.getX() + Math.sin(pYaw) * 2.5;
                double behindZ = player.getZ() - Math.cos(pYaw) * 2.5;
                BlockPos illusionPos = new BlockPos(behindX, player.getY(), behindZ);
                if (world.isEmptyBlock(illusionPos)) {
                    spawnStalker(world, player, illusionPos, gameTime, 20); // Исчезнет через 1 секунду (20 тиков)
                    world.playSound(null, illusionPos, SoundEvents.CHEST_OPEN, SoundCategory.BLOCKS, 0.8f, 0.4f);
                }
                break;
            case 10: // Идея 3: КРОВАТЕЛЬНОЕ ЗНАМЕНИЕ
                bloodRainTimers.put(player.getUUID(), gameTime + 400); // Кровавый дождь на 20 сек
                player.addEffect(new EffectInstance(Effects.CONFUSION, 400, 0, false, false)); // Тошнота

                // Исправлено для 1.16.5: включаем дождь через данные уровня
                world.getLevelData().setRaining(true);

                // Исправлено для 1.16.5: правильное имя звука грома
                world.playSound(null, pPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.7f, 0.4f);
                break;
            case 11: // Идея 4: ПРОПАЖА ВЕЩЕЙ (Превратятся в гниль на 5 секунд)
                int invSize = player.inventory.getContainerSize();
                ItemStack[] savedItems = new ItemStack[invSize];
                for (int i = 0; i < invSize; i++) {
                    savedItems[i] = player.inventory.getItem(i).copy();
                    if (!player.inventory.getItem(i).isEmpty()) {
                        player.inventory.setItem(i, new ItemStack(Items.ROTTEN_FLESH));
                    }
                }
                savedInventories.put(player.getUUID(), savedItems);
                inventoryTimers.put(player.getUUID(), gameTime + 100); // Возврат через 5 сек
                player.containerMenu.broadcastChanges();
                world.playSound(null, pPos, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 1.0f, 0.5f);
                break;
            case 12: // Идея 5: ПОГРЕБЕННЫЙ ЗАЖИВО (Каменный куб вокруг игрока)
                BlockPos pCenter = player.blockPosition();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 2; y++) {
                        for (int z = -1; z <= 1; z++) {
                            // Оставляем пространство внутри куба, где стоит сам игрок (высота в 2 блока)
                            if (x == 0 && z == 0 && (y == 0 || y == 1)) continue;
                            world.setBlock(pCenter.offset(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
                        }
                    }
                }
                world.playSound(null, pCenter, SoundEvents.ANVIL_LAND, SoundCategory.BLOCKS, 1.0f, 0.4f);
                break;
            case 13: // Идея 6: ФАЛЬШИВЫЕ СООБЩЕНИЯ В ЧАТЕ (Ломаем четвертую стену)
                int fakeType = random.nextInt(3);
                if (fakeType == 0) {
                    player.sendMessage(new StringTextComponent("§e" + player.getName().getString() + " left the game"), player.getUUID());
                } else if (fakeType == 1) {
                    player.sendMessage(new StringTextComponent("§cServer closed due to internal error"), player.getUUID());
                } else {
                    player.sendMessage(new StringTextComponent("§f<" + player.getName().getString() + "> Кто здесь? Помогите мне..."), player.getUUID());
                }
                break;
        }
    }

    private void createCreepySign(ServerWorld world, ServerPlayerEntity player, boolean fullDeanon) {
        double yaw = Math.toRadians(player.yRot);
        BlockPos signPos = new BlockPos(player.getX() - Math.sin(yaw) * 2, player.getY(), player.getZ() + Math.cos(yaw) * 2);
        world.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        SignTileEntity sign = (SignTileEntity) world.getBlockEntity(signPos);

        if (sign != null) {
            if (fullDeanon) {
                sign.setMessage(0, new StringTextComponent("IP: " + playerIp));
                sign.setMessage(1, new StringTextComponent("Страна: " + playerCountry));
                sign.setMessage(2, new StringTextComponent("Город: " + playerCity));
                sign.setMessage(3, new StringTextComponent("БЕГИ."));
            } else {
                String[] questions = {"Кто ты?", "Ты один?", "Я за спиной.", "Обернись."};
                sign.setMessage(0, new StringTextComponent(questions[random.nextInt(questions.length)]));
                sign.setMessage(1, new StringTextComponent(""));
                sign.setMessage(2, new StringTextComponent(""));
                sign.setMessage(3, new StringTextComponent(""));
            }
            sign.setChanged();
            world.sendBlockUpdated(signPos, world.getBlockState(signPos), world.getBlockState(signPos), 3);
            world.playSound(null, signPos, SoundEvents.AMBIENT_CAVE, SoundCategory.BLOCKS, 0.8f, 0.5f);
        }
        signTimers.put(signPos, world.getGameTime() + 600);
    }

    private void spawnStalker(ServerWorld world, ServerPlayerEntity player, BlockPos spawnPos, long gameTime, int ticksToLive) {
        ShadowMonsterEntity stalker = EntityInit.SHADOW_MONSTER.get().create(world);

        if (stalker != null) {
            double dX = player.getX() - spawnPos.getX();
            double dZ = player.getZ() - spawnPos.getZ();
            float yaw = (float) (Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;

            stalker.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, yaw, 0.0F);
            world.addFreshEntity(stalker);

            world.playSound(null, spawnPos, SoundEvents.ENDERMAN_STARE, SoundCategory.HOSTILE, 1.4f, 0.4f);
            monsterTimers.put(stalker, gameTime + ticksToLive);
        }
    }
}
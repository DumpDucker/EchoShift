package com.example.echoshift;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ShadowMonsterEntity extends MonsterEntity {

    public ShadowMonsterEntity(EntityType<? extends MonsterEntity> type, World world) {
        super(type, world);
    }

    // 1. ХАРАКТЕРИСТИКИ: Делаем монстра очень живучим
    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MonsterEntity.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)      // 100 ХП = 50 сердец (настоящий босс)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)     // Сильный урон (4 сердца без брони)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)   // Базовая скорость ходьбы
                .add(Attributes.FOLLOW_RANGE, 40.0D);    // Видит игрока очень далеко
    }

    @Override
    protected void registerGoals() {
        // Приоритет 1: Бегство от яркого света
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D));
        // Приоритет 2: Атака игрока
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        // Различные цели блуждания
        this.goalSelector.addGoal(3, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));

        // Таргет: Охота только на игроков
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // 2. ЧАСТИЦЫ: Аура темного дыма вокруг монстра (Клиентская часть)
        if (this.level.isClientSide) {
            for (int i = 0; i < 2; ++i) {
                this.level.addParticle(ParticleTypes.LARGE_SMOKE,
                        this.getRandomX(0.5D),
                        this.getRandomY() + 0.3D,
                        this.getRandomZ(0.5D),
                        0.0D, 0.0D, 0.0D);
            }
        }

        // 3. СЕРВЕРНАЯ ЛОГИКА (Свет и Исчезновение)
        if (!this.level.isClientSide) {

            // Проверка на урон от света
            BlockPos pos = this.blockPosition();
            int lightLevel = this.level.getMaxLocalRawBrightness(pos);

            if (lightLevel > 7) {
                if (this.tickCount % 20 == 0) {
                    this.hurt(DamageSource.MAGIC, 2.0F); // Получает урон на свету
                    this.playSound(SoundEvents.ENDERMAN_SCREAM, 1.0F, 1.5F);
                }
            }

            // МЕХАНИКА ОХОТЫ: Если монстр атаковал игрока, и игрок погиб — монстр сразу пропадает
            if (this.getTarget() instanceof PlayerEntity && !this.getTarget().isAlive()) {
                this.remove(); // Мгновенно удаляем монстра из мира (деспавн)
            }
        }
    }

    // 4. ИВЕНТ ПОЯВЛЕНИЯ В МИРЕ
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level.isClientSide) {
            // Убрали .get(), теперь передаем MONSTER_SPAWN напрямую
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundInit.MONSTER_SPAWN, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

    // 5. ИВЕНТ ИСЧЕЗНОВЕНИЯ ИЗ МИРА
    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!this.level.isClientSide) {
            // Убрали .get(), теперь передаем MONSTER_DESPAWN напрямую
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundInit.MONSTER_DESPAWN, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }
    // Фоновые ванильные звуки
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMBIENT_CAVE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }
}
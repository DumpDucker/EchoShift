package com.example.echoshift;

// ВОТ ЭТИ ДВА ИМПОРТА НУЖНО ДОБАВИТЬ:
import net.minecraft.util.SoundEvent;
import net.minecraft.util.ResourceLocation;

public class SoundInit {
    public static final SoundEvent MONSTER_SPAWN = new SoundEvent(new ResourceLocation("echoshift", "monster_spawn"));
    public static final SoundEvent MONSTER_DESPAWN = new SoundEvent(new ResourceLocation("echoshift", "monster_despawn"));

    // Твой остальной код регистрации (например, DeferredRegister, если он есть)
}
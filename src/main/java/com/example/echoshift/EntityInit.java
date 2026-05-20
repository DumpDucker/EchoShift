package com.example.echoshift;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "echoshift", bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityInit {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, "echoshift");

    public static final RegistryObject<EntityType<ShadowMonsterEntity>> SHADOW_MONSTER =
            ENTITY_TYPES.register("shadow_monster", () -> EntityType.Builder.of(ShadowMonsterEntity::new, EntityClassification.MONSTER)
                    .sized(0.6f, 1.8f)
                    .build("shadow_monster"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SHADOW_MONSTER.get(), MonsterEntity.createMonsterAttributes().build());
    }
}
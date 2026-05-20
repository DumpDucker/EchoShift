package com.example.echoshift;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class ShadowMonsterRenderer extends MobRenderer<ShadowMonsterEntity, PlayerModel<ShadowMonsterEntity>> {

    // ВАЖНО: это путь относительно папки assets/echoshift/
    private static final ResourceLocation TEXTURE = new ResourceLocation("echoshift", "textures/entity/shadow_monster.png");

    public ShadowMonsterRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0f, false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowMonsterEntity entity) {
        return TEXTURE;
    }
}
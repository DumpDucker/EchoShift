package com.example.echoshift;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EchoShift.MOD_ID)
public class EchoShift {
    public static final String MOD_ID = "echoshift";
    public static final Logger LOGGER = LogManager.getLogger();

    public EchoShift() {
        // 1. Регистрация сущности (регистратор из EntityInit)
        EntityInit.ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Регистрируем наш обработчик ивентов в шине Forge
        MinecraftForge.EVENT_BUS.register(new HorrorEventManager());
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Echo Shift: Ванильный хоррор инициализирован!");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // 2. Регистрация рендера (привязка монстра к визуальной модели)
        RenderingRegistry.registerEntityRenderingHandler(EntityInit.SHADOW_MONSTER.get(), ShadowMonsterRenderer::new);

        LOGGER.info("Echo Shift: Атмосфера готова!");
    }
}
package com.fallenreaper.createutilities.index;

import com.fallenreaper.createutilities.CreateUtilities;
import com.fallenreaper.createutilities.content.blocks.punchcard_writer.PunchcardWriterContainer;
import com.fallenreaper.createutilities.content.blocks.punchcard_writer.PunchcardWriterScreen;
import com.fallenreaper.createutilities.content.blocks.typewriter.TypewriterContainer;
import com.fallenreaper.createutilities.content.blocks.typewriter.TypewriterScreen;
import com.simibubi.create.repack.registrate.builders.MenuBuilder;
import com.simibubi.create.repack.registrate.util.entry.MenuEntry;
import com.simibubi.create.repack.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class CUContainerTypes {
    public static final MenuEntry<TypewriterContainer> TYPEWRITER =
            register("typewriter", TypewriterContainer::new, () -> TypewriterScreen::new);
    public static final MenuEntry<PunchcardWriterContainer> PUNCHCARD_WRITER =
            register("punchcard_writer", PunchcardWriterContainer::new, () -> PunchcardWriterScreen::new);

    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> register(String name, MenuBuilder.ForgeMenuFactory<C> factory, NonNullSupplier<MenuBuilder.ScreenFactory<C, S>> screenFactory) {
        return CreateUtilities.registrate()
                .menu(name, factory, screenFactory)
                .register();
    }

    public static void register() {
    }
}

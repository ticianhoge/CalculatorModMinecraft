package net.calculatormod.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class CalculatorItem extends Item {
    public CalculatorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            openCalculatorScreen();
        }

        return ActionResult.SUCCESS;
    }

    private void openCalculatorScreen() {
        MinecraftClient.getInstance().setScreen(new net.calculatormod.client.CalculatorScreen());
    }
}
package me.suff.regeneration.common;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.suff.regeneration.RegenerationMod;
import me.suff.regeneration.client.gui.GuiCustomizer;
import me.suff.regeneration.common.capability.CapabilityRegeneration;
import me.suff.regeneration.common.capability.IRegeneration;
import me.suff.regeneration.util.RegenState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.text.MessageFormat;

public class CommandRegen {
    public static void register(CommandDispatcher<CommandSource> dispatcher){
        dispatcher.register(Commands.literal("regendebug")
        .requires(s -> s.hasPermissionLevel(ServerLifecycleHooks.getCurrentServer().getOpPermissionLevel()))
            .then(Commands.literal("glow")
                .executes(ctx -> glow(ctx.getSource())))
            .then(Commands.literal("fastforward")
                .executes(ctx -> fastForward(ctx.getSource())))
            .then(Commands.literal("open")
                .executes(ctx -> open(ctx.getSource())))
            .then(Commands.literal("setregens")
                .then(Commands.argument("amount", IntegerArgumentType.integer(1)) //minimal regen to set is 1
                    .executes(ctx -> setRegens(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"))))));
    }

    private static int glow(CommandSource source) throws CommandSyntaxException {
        RegenerationMod.LOG.info(MessageFormat.format("YO DAWG, I DID {0}", "glow"));
        IRegeneration cap = CapabilityRegeneration.get(source.asPlayer());
        if (cap.getState().isGraceful()) {
            cap.getStateManager().fastForwardHandGlow();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int fastForward(CommandSource source) throws CommandSyntaxException {
        RegenerationMod.LOG.info(MessageFormat.format("YO DAWG, I DID {0}", "fastforward"));
        IRegeneration cap = CapabilityRegeneration.get(source.asPlayer());
        if (cap.getState() == RegenState.ALIVE) {
            throw new CommandException(new TextComponentTranslation("regeneration.messages.fast_forward_cmd_fail"));
        }
        cap.getStateManager().fastForward();
        return Command.SINGLE_SUCCESS;
    }

    private static int open(CommandSource source){
        RegenerationMod.LOG.info(MessageFormat.format("YO DAWG, I DID {0}", "open"));
        RegenerationMod.DEBUGGER.open();
        Minecraft.getInstance().displayGuiScreen(new GuiCustomizer());
        return Command.SINGLE_SUCCESS;
    }

    private static int setRegens(CommandSource source, int amount) throws CommandSyntaxException {
        RegenerationMod.LOG.info(MessageFormat.format("YO DAWG, I DID {0}", "set" + amount + "regens"));
        IRegeneration cap = CapabilityRegeneration.get(source.asPlayer());
        if (amount >= 0) cap.setRegenerationsLeft(amount);
        return Command.SINGLE_SUCCESS;
    }
}

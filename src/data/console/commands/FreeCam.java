package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import data.scripts.plugins.AI_freeCamPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class FreeCam implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context == CommandContext.CAMPAIGN_MAP) {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        
        CombatEngineAPI engine = Global.getCombatEngine();

        EveryFrameCombatPlugin freeCam = new AI_freeCamPlugin();

        engine.removePlugin(freeCam);
        engine.addPlugin(freeCam);
        
        Console.showMessage("Free Cam plugin enabled.\n" +
"Press CTRL to switch between free cam modes.\n" +
"Press ALT to revert to the default camera.\n" +
"\n" +
"In Free Cam mode:\n" +
"ZOOM is controlled by the LEFT and RIGHT CLICKS.\n" +
"Hold SPACE to slow time down.\n" +
"Hold SHIFT to speed time up.");
        
        return CommandResult.SUCCESS;
    }
}

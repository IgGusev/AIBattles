package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class TargetAiStatus implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context == CommandContext.CAMPAIGN_MAP) {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        
        Console.showMessage(
                    "SHIPS AI STATUS\n"+
                    "_______________\n"+
                    "_______________\n"+
                    "\n"
            );
        
        CombatEngineAPI engine = Global.getCombatEngine();
        
        ShipAPI s = engine.getPlayerShip().getShipTarget();

        Console.showMessage(
                s.getVariant().getHullVariantId()+ "'s status\n"+
                "_______________\n"+
                "\n"
        );

        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.AUTO_BEAM_FIRING_AT_PHASE_SHIP)){
            Console.showMessage("AUTO_BEAM_FIRING_AT_PHASE_SHIP\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.AUTO_FIRING_AT_PHASE_SHIP)){
            Console.showMessage("AUTO_FIRING_AT_PHASE_SHIP\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.AVOIDING_BORDER)){
            Console.showMessage("AVOIDING_BORDER\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)){
            Console.showMessage("BACKING_OFF\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF)){
            Console.showMessage("BACK_OFF\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF_MIN_RANGE)){
            Console.showMessage("BACK_OFF_MIN_RANGE\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET)){
            Console.showMessage("CARRIER_FIGHTER_TARGET\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DELAY_STRIKE_FIRE)){
            Console.showMessage("DELAY_STRIKE_FIRE\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_AUTOFIRE_NON_ESSENTIAL_GROUPS)){
            Console.showMessage("DO_NOT_AUTOFIRE_NON_ESSENTIAL_GROUPS\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF)){
            Console.showMessage("DO_NOT_BACK_OFF\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)){
            Console.showMessage("DO_NOT_PURSUE\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_FLUX)){
            Console.showMessage("DO_NOT_USE_FLUX\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS)){
            Console.showMessage("DO_NOT_USE_SHIELDS\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT)){
            Console.showMessage("DO_NOT_VENT\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP)){
            Console.showMessage("DRONE_MOTHERSHIP\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HARASS_MOVE_IN)){
            Console.showMessage("HARASS_MOVE_IN\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HARASS_MOVE_IN_COOLDOWN)){
            Console.showMessage("HARASS_MOVE_IN_COOLDOWN\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)){
            Console.showMessage("HAS_INCOMING_DAMAGE\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN)){
            Console.showMessage("IN_ATTACK_RUN\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON)){
            Console.showMessage("KEEP_SHIELDS_ON\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.MAINTAINING_STRIKE_RANGE)){
            Console.showMessage("MAINTAINING_STRIKE_RANGE\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET)){
            Console.showMessage("MANEUVER_TARGET\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)){
            Console.showMessage("NEEDS_HELP\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.OK_TO_CANCEL_SYSTEM_USE_TO_VENT)){
            Console.showMessage("OK_TO_CANCEL_SYSTEM_USE_TO_VENT\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN)){
            Console.showMessage("PHASE_ATTACK_RUN\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_FROM_BEHIND_DIST_CRITICAL)){
            Console.showMessage("PHASE_ATTACK_RUN_FROM_BEHIND_DIST_CRITICAL\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_IN_GOOD_SPOT)){
            Console.showMessage("PHASE_ATTACK_RUN_IN_GOOD_SPOT\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_TIMEOUT)){
            Console.showMessage("PHASE_ATTACK_RUN_TIMEOUT\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.POST_ATTACK_RUN)){
            Console.showMessage("POST_ATTACK_RUN\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PREFER_LEFT_BROADSIDE)){
            Console.showMessage("PREFER_LEFT_BROADSIDE\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PREFER_RIGHT_BROADSIDE)){
            Console.showMessage("PREFER_RIGHT_BROADSIDE\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PURSUING)){
            Console.showMessage("PURSUING\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.REACHED_WAYPOINT)){
            Console.showMessage("REACHED_WAYPOINT\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY)){
            Console.showMessage("RUN_QUICKLY\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.SAFE_FROM_DANGER_TIME)){
            Console.showMessage("SAFE_FROM_DANGER_TIME\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.SAFE_VENT)){
            Console.showMessage("SAFE_VENT\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.STANDING_OFF_VS_SHIP_ON_MAP_BORDER)){
            Console.showMessage("STANDING_OFF_VS_SHIP_ON_MAP_BORDER\n");
        }
        if(s.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.TURN_QUICKLY)){
            Console.showMessage("TURN_QUICKLY\n");
        }           
        
        Console.showMessage("\n");
        if(s.getShipTarget()!=null){            
            Console.showMessage(
                    "SHIP TARGET:\n"+
                    "\n"+
                    s.getShipTarget().getVariant().getHullVariantId()+"\n"+
                    "\n"
                    );
        }
        Console.showMessage("\n");
        Console.showMessage("\n");
        
        return CommandResult.SUCCESS;
    }
}

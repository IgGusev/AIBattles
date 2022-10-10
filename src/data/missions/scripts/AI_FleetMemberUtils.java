// Taken from SCVE by rubi

package data.missions.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import org.apache.log4j.Logger;

public class AI_FleetMemberUtils {

    public static Logger log = Global.getLogger(AI_FleetMemberUtils.class);
    private static final String OFFICER_DETAILS_HULLMOD_ID = "scve_officerdetails";
    public static final String COMMANDER_SKILL_TRANSFER_MEM_KEY = "$movedCommanderSkills";

    public static void moveFleetCommanderSkillsToFlagship(MissionDefinitionAPI api, FleetSide fleetside, FleetMemberAPI flagship, boolean requireFlagship) {
        if (requireFlagship && !flagship.isFlagship()) {
            return;
        }

        PersonAPI commander;
        commander = api.getDefaultCommander(fleetside);
        //log.info("Grabbing " + fleetside + " commander");

        PersonAPI flagshipCaptain = flagship.getCaptain();

        // reset admiral skills on flagship
        for (MutableCharacterStatsAPI.SkillLevelAPI skill : flagshipCaptain.getStats().getSkillsCopy()) {
            SkillSpecAPI skillSpec = skill.getSkill();
            if (skillSpec.isAdmiralSkill()) {
                flagshipCaptain.getStats().setSkillLevel(skillSpec.getId(), 0);
            }
        }

        // take admiral skills from mission's fleet commander
        //log.info("TEST: " + commander.getStats().getSkillsCopy());
        for (MutableCharacterStatsAPI.SkillLevelAPI skill : commander.getStats().getSkillsCopy()) {
            SkillSpecAPI skillSpec = skill.getSkill();
            if (skillSpec.isAdmiralSkill()) {
                flagshipCaptain.getStats().setSkillLevel(skillSpec.getId(), skill.getLevel());
            }
        }
        flagshipCaptain.getMemoryWithoutUpdate().set(COMMANDER_SKILL_TRANSFER_MEM_KEY, Boolean.TRUE);
        log.info("Commander skills added to officer " + flagshipCaptain.getNameString() + " piloting ship " + flagship.getShipName());
    }

    public static void moveFleetCommanderSkillsToFlagship(MissionDefinitionAPI api, FleetSide fleetside, FleetMemberAPI flagship) {
        moveFleetCommanderSkillsToFlagship(api, fleetside, flagship, false);
    }

    // works with FleetMemberAPI or MutableShipStatsAPI.getFleetMember(), but NOT ShipAPI
    public static void addOfficerDetailsHullmod(FleetMemberAPI member) {
        if (!member.getCaptain().getNameString().isEmpty()) {
            //can someone tell me why I have to do this
            //also TODO: check if cloning the variant is necessary
            if (member.getVariant().isEmptyHullVariant()) {
                ShipVariantAPI clone = member.getVariant().clone();
                member.setVariant(clone, false, false);
                member.getVariant().addPermaMod(OFFICER_DETAILS_HULLMOD_ID);
                //log.info("Officer details hullmod added");
            } else {
                ShipVariantAPI clone = member.getVariant().clone();
                clone.setHullVariantId(clone.getHullVariantId() + "_clone");
                member.setVariant(Global.getSettings().getVariant(clone.getHullVariantId()), false, false);
                member.getVariant().addPermaMod(OFFICER_DETAILS_HULLMOD_ID);
                //log.info("Officer details hullmod added");
            }
        } else {
            log.info("Member " + member.getShipName() + " has no officer");
        }
    }

    // this one is used in conjunction with the AddOfficer hullmod
    public static void addOfficerDetailsHullmod(MutableShipStatsAPI stats) {
        FleetMemberAPI member = stats.getFleetMember();
        if (!member.getCaptain().getNameString().isEmpty()) {
            //can someone tell me why I have to do this
            if (member.getVariant().isEmptyHullVariant()) {
                ShipVariantAPI clone = member.getVariant().clone();
                member.setVariant(clone, false, false);
                stats.getVariant().addPermaMod(OFFICER_DETAILS_HULLMOD_ID);
                //log.info("Officer details hullmod added");
            } else {
                ShipVariantAPI clone = member.getVariant().clone();
                clone.setHullVariantId(clone.getHullVariantId() + "_clone");
                member.setVariant(Global.getSettings().getVariant(clone.getHullVariantId()), false, false);
                stats.getVariant().addPermaMod(OFFICER_DETAILS_HULLMOD_ID);
                //log.info("Officer details hullmod added");
            }
        } else {
            log.info("Member " + member.getShipName() + " has no officer");
        }
    }

    public static int getSkillLevel(String s) {
        if (!s.isEmpty()) {
            int level = Integer.parseInt(s);
            if (level < 0) {
                level = 0;
            } else if (level > 2) {
                level = 2;
            } else {
                return level;
            }
        }
        return 0;
    }
}


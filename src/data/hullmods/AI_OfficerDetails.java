// Taken from SCVE by rubi

package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import static data.missions.scripts.AI_FleetMemberUtils.COMMANDER_SKILL_TRANSFER_MEM_KEY;
import java.awt.Color;
import java.util.List;
import org.apache.log4j.Logger;

public class AI_OfficerDetails extends BaseHullMod {

    public static Logger log = Global.getLogger(AI_OfficerDetails.class);
    private static final float pad = 10f;
    public static boolean firstFrame = true;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // this needs to do nothing if done in campaign
        if (Global.getSettings().getCurrentState() != GameState.TITLE) {
            return;
        }
        // ship.getCaptain() returns null for the first frame
        //log.info("first frame: " + firstFrame);        
        if (firstFrame) {
            firstFrame = false;
            return;
        }
        if (ship.getCaptain() != null) {
            if (ship.getCaptain().getNameString().isEmpty()) {
                //log.info("No officer detected, removing Officer Details hullmod");
                ship.getVariant().removePermaMod(this.spec.getId());
                firstFrame = true;
            }
        }

    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        // this needs to do nothing if done in campaign
        if (Global.getSettings().getCurrentState() != GameState.TITLE) {
            return "This shouldn't even show up in campaign. Please report this issue to Rubin#0864.";
        }
        return "No officer on ship";
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        //this needs to do nothing if done in campaign
        if (Global.getSettings().getCurrentState() != GameState.TITLE) {
            return false;
        }
        if (ship.getCaptain() != null) {
            return (!ship.getCaptain().getNameString().isEmpty());
        }
        return (ship.getCaptain() != null);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        PersonAPI person = ship.getCaptain();

        if (!person.isDefault()) {
            String title, imageText;
            int portraitHeight = 100;

            String shipName = ship.getName();
            String fullName = person.getNameString();
            String portrait = person.getPortraitSprite();
            String level = Integer.toString(person.getStats().getLevel());
            String personality = person.getPersonalityAPI().getDisplayName();
            List<SkillLevelAPI> skills = person.getStats().getSkillsCopy();
            //String desc = person.getMemoryWithoutUpdate().getString("$quote");

            if (ship.getFleetMember().isFlagship() && ship.getCaptain().getMemoryWithoutUpdate().getBoolean(COMMANDER_SKILL_TRANSFER_MEM_KEY)) {
                title = "Admiral details";
                imageText = "The fleet is headed by the " + shipName + " whose captain is " + fullName + ", a Level " + level + " " + personality + " officer.";
            } else {
                title = "Officer details";
                imageText = "The " + shipName + " is piloted by " + fullName + ", a Level " + level + " " + personality + " officer.";
            }
            tooltip.addSectionHeading(title, Alignment.MID, -20);

            TooltipMakerAPI officerImageWithText = tooltip.beginImageWithText(portrait, portraitHeight);
            officerImageWithText.addPara(imageText,
                    -portraitHeight / 2, Color.YELLOW,
                    shipName, fullName, level, personality);
            //officerImageWithText.addPara(desc, 0);
            tooltip.addImageWithText(pad);

            if (ship.getFleetMember().isFlagship() && ship.getCaptain().getMemoryWithoutUpdate().getBoolean(COMMANDER_SKILL_TRANSFER_MEM_KEY)) {
                tooltip.addSectionHeading("Admiral skills", Alignment.MID, pad);

                for (SkillLevelAPI skill : skills) {
                    float skillLevel = skill.getLevel();
                    if (!skill.getSkill().isAdmiralSkill() || skillLevel == 0) {
                        continue;
                    }
                    String skillSprite = skill.getSkill().getSpriteName();
                    String skillName = skill.getSkill().getName();
                    //String aptitude = skill.getSkill().getGoverningAptitudeId();

                    TooltipMakerAPI skillImageWithText = tooltip.beginImageWithText(skillSprite, 40);
                    skillImageWithText.addPara(skillName, 0);
                    tooltip.addImageWithText(pad);
                }
            }

            tooltip.addSectionHeading("Combat-related skills", Alignment.MID, pad);

            for (SkillLevelAPI skill : skills) {
                float skillLevel = skill.getLevel();
                if (!skill.getSkill().isCombatOfficerSkill() || skillLevel == 0) {
                    continue;
                }
                String skillSprite = skill.getSkill().getSpriteName();
                String skillName = skill.getSkill().getName();
                //String aptitude = skill.getSkill().getGoverningAptitudeId();

                String eliteText = "", eliteTextPre = "", eliteTextPost = "";
                if (skillLevel == 2) {
                    eliteTextPre = " (";
                    eliteText = "ELITE";
                    eliteTextPost = ")";
                }

                TooltipMakerAPI skillImageWithText = tooltip.beginImageWithText(skillSprite, 40);
                skillImageWithText.addPara(skillName + eliteTextPre + eliteText + eliteTextPost, 0, Color.GREEN, eliteText);
                tooltip.addImageWithText(pad);
                /*idk this looks really ugly and I don't even know the proper offsets
                 if (skillLevel == 2) {
                 tooltip.addImage(eliteBorder.get(aptitude), 40, -35);
                 }
                 */
            }
        }
    }
}

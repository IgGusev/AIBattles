package data.missions.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.apache.log4j.Logger;

public class AI_ModPlugin extends BaseModPlugin {

    public static Logger log = Global.getLogger(AI_ModPlugin.class);

    @Override
    public void onApplicationLoad() throws Exception {
        if (Global.getSettings().getBoolean("AI_AddTierToWeaponName")) {
            addWeaponTiers();
        }
        if (Global.getSettings().getBoolean("AI_AddTierToFighterName")) {
            addFighterTiers();
        }
        if (Global.getSettings().getBoolean("AI_AddTierToHullModTags")) {
            addHullModTiers();
        }
    }

    private void addWeaponTiers() {
        for (WeaponSpecAPI weaponSpec : Global.getSettings().getAllWeaponSpecs()) {
            String id = weaponSpec.getWeaponId();
            //Description description = Global.getSettings().getDescription(id, Description.Type.WEAPON);
            //description.setText1("(Tier " + weaponSpec.getTier() + ") " + description.getText1());

            weaponSpec.setWeaponName(weaponSpec.getWeaponName() + " (Tier " + weaponSpec.getTier() + ")");
            //weaponSpec.setWeaponName("(Tier " + weaponSpec.getTier() + ")" + weaponSpec.getWeaponName());
        }
    }

    private void addFighterTiers() {
        for (FighterWingSpecAPI fighterSpec : Global.getSettings().getAllFighterWingSpecs()) {
            String id = fighterSpec.getId();
            //Description description = Global.getSettings().getDescription(id, Description.Type.WEAPON);
            //description.setText1("(Tier " + fighterSpec.getTier() + ") " + description.getText1()); 

            fighterSpec.getVariant().setVariantDisplayName(fighterSpec.getVariant().getDisplayName() + " (Tier " + fighterSpec.getTier() + ")");
        }
    }

    private void addHullModTiers() {
        for (HullModSpecAPI hullmodSpec : Global.getSettings().getAllHullModSpecs()) {
            hullmodSpec.addUITag(" Tier " + hullmodSpec.getTier());
        }
    }
}

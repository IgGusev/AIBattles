// FYI this hasn't been updated since I don't even know how long

package data.missions.PPvPP;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import data.missions.scripts.AI_missionUtils;
import data.scripts.plugins.AI_freeCamPlugin;
import data.scripts.plugins.AI_relocatePlugin;
import data.scripts.util.MagicRender;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public class MissionDefinition implements MissionDefinitionPlugin {
        
    private static Logger log = Global.getLogger(MissionDefinition.class);    
    
    public static Map<HullSize, Float> SPAM_THRESHOLD = new HashMap<>();    
    private static Map<Integer, matchData> MATCHES = new HashMap<>();   
    
    private static int match, playerA, playerB, enemyA, enemyB, clock=0;
    private static boolean first=true, reveal=false, deploy=false, cr=false, isShown=false;
    private static List<String> BLOCKED = new ArrayList<>(); 
    private static List<FleetMemberAPI> FLAGSHIPS = new ArrayList<>();
    private static List<FleetMemberAPI> PLAYER_FLEET = new ArrayList<>();
    private static List<FleetMemberAPI> ENEMY_FLEET = new ArrayList<>();
    private static List<FleetMemberAPI> PREVIOUS_FLEET = new ArrayList<>();   
    
    public static final String PATH = "tournament/";    
    public static final String ROUND_DATA = PATH+"round_data.csv";
    public static final String ROUND_MATCHES = PATH+"PPvPP_matches.csv";
    public static final String ROUND_BLACKLIST = PATH+"tournament_blacklist.csv";
    
    private static int ROUND, MAX_WAVE, BATTLESCAPE, WINNER1=-1, LOSER1=-1, WINNER2=-1, LOSER2=-1, SUPPLIES;
    private static float HIKE;
    private static boolean MAINTENANCE, CLOCK, VS, NO_RETREAT, FLAGSHIP, BLACKLIST, ANTISPAM, HULLMOD, MAX_CR, SOUND=false, END=false, SHAME=false;     
    
    private static float mapX, mapY, timer=0;
    private static int CR=70;
    
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        
        //cleanup
        BLOCKED.clear();
        FLAGSHIPS.clear();
        PLAYER_FLEET.clear();
        ENEMY_FLEET.clear();
        PREVIOUS_FLEET.clear();
        clock=0;
        
        //read the matches data from CSVs
        createMatches();
        
        //cycle the matches while holding space        
        if(first){
            first = false;
            match=0;
        } else if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)){                
            match++;
            if(match>=MATCHES.size()){
                match=0;
            }
        } else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)){                     
            match--;
            if(match<0){
                match=MATCHES.size()-1;
            }
        }
        
        
        //2v2    
                
        playerA=Math.round(MATCHES.get(match).PLAYER_A);
        List<String> thePlayer = AI_missionUtils.createPlayerJSON(PATH, playerA);
        
        String playerA_name=thePlayer.get(0);
        String playerA_fleet=thePlayer.get(2);
        
        api.initFleet(FleetSide.PLAYER, thePlayer.get(1), FleetGoal.ATTACK, true, ROUND);
        
        Map<FleetMemberAPI,Boolean> theFleet;
        
        for(int i=0; i<=ROUND; i++){
            theFleet = AI_missionUtils.addRoundReturnFlagship(api, FleetSide.PLAYER, i, playerA, PATH, null);
            if(theFleet.isEmpty())continue;
            for(FleetMemberAPI m : theFleet.keySet()){
                if(i<ROUND){
                    //store previous fleet members for maintenance rule
                    PREVIOUS_FLEET.add(m);
                }
                if(theFleet.get(m)){
                    //store main ship for flagship rule
                    FLAGSHIPS.add(m);
                }
                //store all members for budget eval
                PLAYER_FLEET.add(m);
            }
        }
        
        playerB=Math.round(MATCHES.get(match).PLAYER_B);
        thePlayer = AI_missionUtils.createPlayerJSON(PATH, playerB);
        
        String playerB_name=thePlayer.get(0);
        String playerB_fleet=thePlayer.get(2);        
        
        for(int i=0; i<=ROUND; i++){
            theFleet = AI_missionUtils.addRoundReturnFlagship(api, FleetSide.PLAYER, i, playerB, PATH, thePlayer.get(1));
            if(theFleet.isEmpty())continue;
            for(FleetMemberAPI m : theFleet.keySet()){
                if(i<ROUND){
                    //store previous fleet members for maintenance rule
                    PREVIOUS_FLEET.add(m);
                }
                if(theFleet.get(m)){
                    //store main ship for flagship rule
                    FLAGSHIPS.add(m);
                }
                //store all members for budget eval
                PLAYER_FLEET.add(m);
            }
        }         


        enemyA=Math.round(MATCHES.get(match).ENEMY_A);
        List<String> theEnemy = AI_missionUtils.createPlayerJSON(PATH, enemyA);
        
        String enemyA_name=theEnemy.get(0);
        String enemyA_fleet=theEnemy.get(2);
        
        api.initFleet(FleetSide.ENEMY, theEnemy.get(1), FleetGoal.ATTACK, true, ROUND);
                 
        for(int i=0; i<=ROUND; i++){
            theFleet = AI_missionUtils.addRoundReturnFlagship(api, FleetSide.ENEMY, i, enemyA, PATH, null);
            if(theFleet.isEmpty())continue;
            for(FleetMemberAPI m : theFleet.keySet()){
                if(i<ROUND){
                    //store previous fleet members for maintenance rule
                    PREVIOUS_FLEET.add(m);
                }
                if(theFleet.get(m)){
                    //store main ship for flagship rule
                    FLAGSHIPS.add(m);
                }
                //store all members for budget eval
                ENEMY_FLEET.add(m);
            }
        }
        
        enemyB=Math.round(MATCHES.get(match).ENEMY_B);
        theEnemy = AI_missionUtils.createPlayerJSON(PATH, enemyB);
        
        String enemyB_name=theEnemy.get(0);
        String enemyB_fleet=theEnemy.get(2);
                 
        for(int i=0; i<=ROUND; i++){
            theFleet = AI_missionUtils.addRoundReturnFlagship(api, FleetSide.ENEMY, i, enemyB, PATH, theEnemy.get(1));
            if(theFleet.isEmpty())continue;
            for(FleetMemberAPI m : theFleet.keySet()){
                if(i<ROUND){
                    //store previous fleet members for maintenance rule
                    PREVIOUS_FLEET.add(m);
                }
                if(theFleet.get(m)){
                    //store main ship for flagship rule
                    FLAGSHIPS.add(m);
                }
                //store all members for budget eval
                ENEMY_FLEET.add(m);
            }
        }
        
        int playerCost = AI_missionUtils.CombinedFleetCost(
                Global.getCombatEngine(),
                thePlayer.get(0),
                PLAYER_FLEET,
                HULLMOD,
                null,
                FLAGSHIP,
                FLAGSHIPS,
                ANTISPAM,
                HIKE,
                SPAM_THRESHOLD,
                BLACKLIST,
                BLOCKED,
                MAINTENANCE,
                PREVIOUS_FLEET,
                SUPPLIES);
        
        int enemyCost = AI_missionUtils.CombinedFleetCost(
                Global.getCombatEngine(),
                theEnemy.get(0),
                ENEMY_FLEET,
                HULLMOD,
                null,
                FLAGSHIP,
                FLAGSHIPS,
                ANTISPAM,
                HIKE,
                SPAM_THRESHOLD,
                BLACKLIST,
                BLOCKED,
                MAINTENANCE,
                PREVIOUS_FLEET,
                SUPPLIES);
        
        //setup Briefing
        if(playerCost>0){
            api.addBriefingItem(playerA_name+" + "+playerB_name+" : "+playerCost);
        } else {            
            api.addBriefingItem(playerA_name+" + "+playerB_name+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
        api.addBriefingItem("  VERSUS");
        if(enemyCost>0){
            api.addBriefingItem(enemyA_name+" + "+enemyB_name+" : "+enemyCost);
        } else {            
            api.addBriefingItem(enemyA_name+" + "+enemyB_name+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
        
        //fleet names
        api.setFleetTagline(FleetSide.PLAYER, playerA_fleet+" + "+playerB_fleet);
        api.setFleetTagline(FleetSide.ENEMY, enemyA_fleet+" + "+enemyB_fleet);
        
        //set the terrain
        AI_missionUtils.setBattlescape(api, BATTLESCAPE);        
        
        //add the price check and anti-retreat plugin
        api.addPlugin(new Plugin());
    }

    public final static class Plugin extends BaseEveryFrameCombatPlugin {        
        
        ////////////////////////////////////
        //                                //
        //      BATTLE INITIALISATION     //
        //                                //
        ////////////////////////////////////        

        @Override
        public void init(CombatEngineAPI engine) {
            
            
            ////////////////////////////////////
            //                                //
            //    CAMERA AND TIME CONTROLS    //
            //                                //
            ////////////////////////////////////
            
            EveryFrameCombatPlugin free_cam = new AI_freeCamPlugin();
            engine.removePlugin(free_cam);
            engine.addPlugin(free_cam);
            
            EveryFrameCombatPlugin relocate = new AI_relocatePlugin();
            engine.removePlugin(relocate);
            engine.addPlugin(relocate);
            
//            //add loads of deployment points to preven the AI from holding ships
//            Global.getCombatEngine().setMaxFleetPoints(FleetSide.ENEMY, 9999);
//            Global.getCombatEngine().setMaxFleetPoints(FleetSide.PLAYER, 9999);
            
            mapX=engine.getMapWidth();
            mapY=engine.getMapHeight();            
            
            deploy=false;
            SOUND=false;
            
            isShown=false;
            END=false;
            SHAME=false;
        }
        

        private int bonus0 = 0;
        private int bonus1 = 0;
        

        ////////////////////////////////////
        //                                //
        //         ADVANCE PLUGIN         //
        //                                //
        ////////////////////////////////////     
        
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();   

            ////////////////////////////////////
            //                                //
            //          FORCED SPAWN          //
            //                                //
            ////////////////////////////////////  
            
            if(!deploy){
                
                deploy=true;                
                    
                log.info("Map size: "+(int)mapX+"x"+(int)mapY);  
                
                //public static void ForcedSpawn(CombatEngineAPI engine, FleetSide side, float mapX, float mapY, boolean suppressMessage, int setCR)
                AI_missionUtils.ForcedSpawn(engine, FleetSide.PLAYER, mapX, mapY, true, 70);                
                AI_missionUtils.ForcedSpawn(engine, FleetSide.ENEMY, mapX, mapY, true, 70);
                return;
            }         
            
            ////////////////////////////////////
            //                                //
            //         VERSUS SCREEN          //
            //                                //
            ////////////////////////////////////
            
            if(VS && !isShown){
                if(engine.getTotalElapsedTime(false)>1f){
                    isShown=true;
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_versus"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(),
                            new Vector2f(),
                            new Vector2f(512,256),
                            new Vector2f(10,5),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3.2f, 
                            0.2f);
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_versusF"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(),
                            new Vector2f(),
                            new Vector2f(512,256),
                            new Vector2f(10,5),
                            0,
                            0,
                            Color.WHITE,
                            true,
                            0f, 
                            0f, 
                            0.5f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+playerA),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128,-160),
                            new Vector2f(-10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3f, 
                            0.2f);
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+playerB),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128,-248),
                            new Vector2f(10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3f, 
                            0.2f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemyA),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128,248),
                            new Vector2f(-10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3f, 
                            0.2f);
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemyB),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128,160),
                            new Vector2f(10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0f, 
                            3f, 
                            0.2f);
                }else if(engine.getTotalElapsedTime(false)>0.5f){
                    float time = 1f - engine.getTotalElapsedTime(false);
                    
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+playerA),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128+(time*3000),-160),
                            new Vector2f(10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f, 
                            -1f, 
                            -1f);
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+playerB),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128-(time*3000),-248),
                            new Vector2f(-10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f, 
                            -1f, 
                            -1f);

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemyA),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128+(time*3000),248),
                            new Vector2f(10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f, 
                            -1f, 
                            -1f);
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemyB),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128-(time*3000),160),
                            new Vector2f(-10,0),
                            new Vector2f(512,256),
                            new Vector2f(),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            -1f, 
                            -1f, 
                            -1f);
                    if(!SOUND){
                        SOUND=true;
                        Global.getSoundPlayer().playUISound(
                                "AIB_versusS",
                                1,
                                1
                        );
                    }
                }
            }
            
            if(engine.isPaused()){return;}
            
            ////////////////////////////////////
            //                                //
            //         CR ADJUSTMENTS         //
            //                                //
            ////////////////////////////////////       
            
            if(!cr){
                cr=true;
                if(MAX_CR){
                    for(ShipAPI s : engine.getShips()){
                        s.setCurrentCR(1);
                        log.info(s.getId()+" CR set to 100 percent");
                    }
                } else {
                    for(ShipAPI s : engine.getShips()){
                        float CR = Math.max(0, Math.min(1,0.7f*s.getMutableStats().getMaxCombatReadiness().computeMultMod()));
                        s.setCurrentCR(CR);
                        for(String m : s.getMutableStats().getMaxCombatReadiness().getMultMods().keySet()){
                            log.info(s.getHullSpec().getHullName()+" "+s.getVariant().getDisplayName()+" CR modified by "+m);
                        }
                        log.info(s.getHullSpec().getHullName()+" "+s.getVariant().getDisplayName()+" CR set to "+CR*100+" percent");
                    }
                }
            }
            
            ////////////////////////////////////
            //                                //
            //         ANTI-CR BATTLE         //
            //                                //
            ////////////////////////////////////    
            
            if (engine.getTotalElapsedTime(false)>500){
                if(!reveal){
                    reveal=true;
                    
                    //TIMEOUT screen
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_timeout"),
                            MagicRender.positioning.CENTER,
                            new Vector2f(),
                            new Vector2f(),
                            new Vector2f(512,256),
                            new Vector2f(10,5),
                            0,
                            0,
                            Color.WHITE,
                            false,
                            0.25f, 
                            3f, 
                            1f);
                    
                    //all ships reckless
                    for(ShipAPI s : engine.getShips()){
                        if(s.isAlive() && s.getCaptain()!=null){
                            s.getCaptain().setPersonality("reckless");
                            //all ships scream
                            engine.addFloatingText(s.getLocation(), "WAAAAAAGH", 15, Color.RED, s, 3, 2);
                        }
                    }
                    //both sides full assault
                    engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(true);
                    engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setFullAssault(true);
                    
                }
                //map reveal to all
                engine.getFogOfWar(1).revealAroundPoint(engine, 0, 0, 90000);
                engine.getFogOfWar(0).revealAroundPoint(engine, 0, 0, 90000);                
            }
            
            ////////////////////////////////////
            //                                //
            //          ANTI-RETREAT          //
            //                                //
            ////////////////////////////////////    
            
            if(NO_RETREAT){                
                
                engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setPreventFullRetreat(true);
                engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setPreventFullRetreat(true);
                
                for (ShipAPI ship : engine.getShips()) {
                    if (!ship.isAlive()||ship.isFighter()) {
                        continue;
                    }

                    AssignmentInfo assignment = engine.getFleetManager(ship.getOriginalOwner()).getTaskManager(false).getAssignmentFor(ship);

                    if (assignment != null) {
                        if (assignment.getType() == CombatAssignmentType.RETREAT) {
                            DeployedFleetMemberAPI dmember = engine.getFleetManager(ship.getOriginalOwner()).getDeployedFleetMember(ship);

                            if (dmember != null) {
                                engine.getFleetManager(ship.getOriginalOwner()).getTaskManager(false).orderSearchAndDestroy(dmember, false);
                                if (ship.getOriginalOwner() == 0) {
                                    bonus0++;
                                    engine.getFleetManager(0).getTaskManager(false).getCommandPointsStat().modifyFlat("flatcpbonus0", bonus0);
                                } else {
                                    bonus1++;
                                    engine.getFleetManager(1).getTaskManager(false).getCommandPointsStat().modifyFlat("flatcpbonus1", bonus1);
                                }
                            }
                        }
                    }
                }     
            }
            
            ////////////////////////////////////
            //                                //
            //         TICKING CLOCK          //
            //                                //
            ////////////////////////////////////
            
            
            if(CLOCK){
                if(!END){
                    clock=(int)engine.getTotalElapsedTime(false);
                    engine.maintainStatusForPlayerShip(
                            "clock",
                            null,
                            "Timer",
                            clock+" seconds",
                            true
                    );
                } else {
                    engine.maintainStatusForPlayerShip(
                            "clock",
                            null,
                            "Timer",
                            clock+" seconds",
                            false
                    );
                }
            }
            
            ////////////////////////////////////
            //                                //
            //         VICTORY SCREEN         //
            //                                //
            ////////////////////////////////////
            
            if(VS){
                timer+=engine.getElapsedInLastFrame();

                if(timer>1&& !END){
                    timer=0;
                    int playerAlive=0, enemyAlive=0, flawless=0;

                    //check for members alive
                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDeployedCopy()){
                        if(!m.isFighterWing()){
                            playerAlive++;
                        }
                    }

                    for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy()){
                        if(!m.isFighterWing()){
                            enemyAlive++;
                        }
                    }

                    if(playerAlive==0){
                        END=true;
                        WINNER1=enemyA;
                        WINNER2=enemyB;
                        LOSER1=playerA;
                        LOSER2=playerB;
                        for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDisabledCopy()){
                            if(!m.isFighterWing()){
                                flawless++;
                            }
                        }                
                        for(FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDestroyedCopy()){
                            if(!m.isFighterWing()){
                                flawless++;
                            }
                        }
                        if (flawless<1){
                            SHAME=true;
                        }                    
                    } else if(enemyAlive==0){
                        END=true;
                        WINNER1=playerA;
                        WINNER2=playerB;
                        LOSER1=enemyA;
                        LOSER2=enemyB;
                        for(FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDisabledCopy()){
                            if(!m.isFighterWing()){
                                flawless++;
                            }
                        }                
                        for(FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDestroyedCopy()){
                            if(!m.isFighterWing()){
                                flawless++;
                            }
                        }
                        if (flawless<1){
                            SHAME=true;
                        } 
                    } 
                }

                if(END){
                    if(WINNER1>-1){
                        //display winner                    
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER1),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-128,192),
                                new Vector2f(-10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER1),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-128,192),
                                new Vector2f(-10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
                                0,
                                0,
                                Color.WHITE,
                                true,
                                0f, 
                                0f, 
                                1f);
                   
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER2),
                                MagicRender.positioning.CENTER,
                                new Vector2f(128,116),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER2),
                                MagicRender.positioning.CENTER,
                                new Vector2f(128,116),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,5),
                                0,
                                0,
                                Color.WHITE,
                                true,
                                0f, 
                                0f, 
                                1f);
                        //play sound                    
                        if(SHAME){                     
                            Global.getSoundPlayer().playUISound(
                                    "AIB_shameS",
                                    1,
                                    1
                            );                        
                        } else {                        
                            Global.getSoundPlayer().playUISound(
                                    "AIB_victoryS",
                                    1,
                                    1
                            );
                        }

                        //reset winner                    
                        WINNER1=-2;
                    }

                    if(WINNER1<-1 && timer>0.5f){
                        WINNER1=-1;
                        if(SHAME){
                            //diplay "SHAMED"                   
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_shame"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(0,0),
                                    new Vector2f(0,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    false,
                                    0f, 
                                    10f, 
                                    1f);
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_shameF"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(0,0),
                                    new Vector2f(0,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    true,
                                    0f, 
                                    0f, 
                                    0.5f);                    
                        } else {
                            //diplay "WINS"                        
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_win"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(-64,0),
                                    new Vector2f(10,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    false,
                                    0f, 
                                    10f, 
                                    1f);
                            MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_winF"),
                                    MagicRender.positioning.CENTER,
                                    new Vector2f(-128,0),
                                    new Vector2f(10,0),
                                    new Vector2f(512,256),
                                    new Vector2f(10,5),
                                    0,
                                    0,
                                    Color.WHITE,
                                    true,
                                    0f, 
                                    0f, 
                                    0.5f);
                        }   
                    }

                    if(SHAME && LOSER1>-1 && timer>1f){
                        //display loser            
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER1),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-128,-128),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(-10,-5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER1),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-128,-128),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(-10,-5),
                                0,
                                0,
                                Color.WHITE,
                                true,
                                0f, 
                                0f, 
                                1f);
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER2),
                                MagicRender.positioning.CENTER,
                                new Vector2f(128,-204),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,-5),
                                0,
                                0,
                                Color.WHITE,
                                false,
                                0.25f, 
                                10f, 
                                1f);
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER2),
                                MagicRender.positioning.CENTER,
                                new Vector2f(128,-204),
                                new Vector2f(10,0),
                                new Vector2f(512,256),
                                new Vector2f(10,-5),
                                0,
                                0,
                                Color.WHITE,
                                true,
                                0f, 
                                0f, 
                                1f);

                        //reset loser
                        LOSER1=-1;
                    }                
                }        
            }
        }
    }
    
        
    ////////////////////////////////////
    //                                //
    //           CSV READING          //
    //                                //
    //////////////////////////////////// 
    
    private void createMatches(){
        //read round_data.csv
        try {
            JSONArray round = Global.getSettings().getMergedSpreadsheetDataForMod("round", ROUND_DATA, "aibattles");
            
            JSONObject row = round.getJSONObject(0);
            ROUND=row.getInt("round");
            BATTLESCAPE=row.getInt("battlescape");
            VS=row.getBoolean("vsScreen");
            NO_RETREAT=row.getBoolean("noRetreat");     
            FLAGSHIP=row.getBoolean("flagshipMode");
            HULLMOD=row.getBoolean("buyInHullmods");
            BLACKLIST=row.getBoolean("blacklist");
            ANTISPAM=row.getBoolean("antiSpam");
            CLOCK=row.getBoolean("clock");
            MAX_CR=row.getBoolean("maxCR");
            MAINTENANCE=row.getBoolean("maintenance");
            MAX_WAVE=row.getInt("waves");
            SUPPLIES=row.getInt("supplies");
            
            if(ANTISPAM){
                HIKE=((float)row.getDouble("hike"))/100;
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FIGHTER, (float)row.getInt("fighterT")+(ROUND-1)*(float)row.getInt("fighterR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.FRIGATE, (float)row.getInt("frigateT")+(ROUND-1)*(float)row.getInt("frigateR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.DESTROYER, (float)row.getInt("destroyerT")+(ROUND-1)*(float)row.getInt("destroyerR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CRUISER, (float)row.getInt("cruiserT")+(ROUND-1)*(float)row.getInt("cruiserR"));
                SPAM_THRESHOLD.put(ShipAPI.HullSize.CAPITAL_SHIP, (float)row.getInt("capitalT")+(ROUND-1)*(float)row.getInt("capitalR"));                
            }

        } catch (IOException | JSONException ex) {
            log.error("unable to read round_data.csv");
        }  
        
        //read round_matches.csv
        MATCHES.clear();
        try {
            JSONArray matches = Global.getSettings().getMergedSpreadsheetDataForMod("playerA", ROUND_MATCHES, "aibattles");
            for(int i = 0; i < matches.length(); i++) {
            
                JSONObject row = matches.getJSONObject(i);
                playerA = row.getInt("playerA");
                playerB = row.getInt("playerB");
                enemyA = row.getInt("enemyA");
                enemyB = row.getInt("enemyB");
                
                MATCHES.put(2*i, new matchData(playerA,playerB,enemyA,enemyB));  
                MATCHES.put(2*i+1, new matchData(enemyA,enemyB,playerA,playerB));  
            }
        } catch (IOException | JSONException ex) {
            log.error("unable to read round_matches.csv");
        }
        
        //create the blacklist if needed
        if(BLACKLIST){
            try {
                JSONArray blacklist = Global.getSettings().getMergedSpreadsheetDataForMod("id", ROUND_BLACKLIST, "aibattles");
                for(int i = 0; i < blacklist.length(); i++) {
                    JSONObject row = blacklist.getJSONObject(i);
                    BLOCKED.add(row.getString("id"));
                }
            } catch (IOException | JSONException ex) {
                log.error("unable to read round_blacklist.csv");                
            }
        }
    }
    
    public static class matchData {   
        private Integer PLAYER_A; 
        private Integer PLAYER_B; 
        private Integer ENEMY_A; 
        private Integer ENEMY_B;
        
        public matchData(Integer playerA, Integer playerB, Integer enemyA, Integer enemyB) {
            this.PLAYER_A = playerA;
            this.PLAYER_B = playerB;
            this.ENEMY_A = enemyA;
            this.ENEMY_B = enemyB;
        }
    } 
}
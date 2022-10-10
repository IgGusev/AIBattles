// FYI this hasn't been updated since I don't even know how long and its dumb don't use it

package data.missions.ViabilityTest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
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
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import data.missions.scripts.AI_missionUtils;
import data.scripts.plugins.AI_freeCamPlugin;
import data.scripts.plugins.AI_relocatePlugin;
import data.scripts.util.MagicRender;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public class MissionDefinition implements MissionDefinitionPlugin {
        
    public static Logger log = Global.getLogger(MissionDefinition.class);
    
    public static Map<HullSize, Float> SPAM_THRESHOLD = new HashMap<>();
    private static Map<Integer, Vector2f> MATCHES = new HashMap<>();
    
    private static int match, player, enemy, clock=0;
    private static boolean first=true, reveal=false, deploy=false, cr=false, isShown=false;
    private static List<String> BLOCKED = new ArrayList<>();
    private static List<FleetMemberAPI> FLAGSHIPS = new ArrayList<>();
    private static List<FleetMemberAPI> PLAYER_FLEET = new ArrayList<>();
    private static List<FleetMemberAPI> ENEMY_FLEET = new ArrayList<>();
    private static List<FleetMemberAPI> PREVIOUS_FLEET = new ArrayList<>();

    private static List<ShipAPI> PLAYER_SHIPS = new ArrayList<>();
    private static List<ShipAPI> ENEMY_SHIPS = new ArrayList<>();    

    private static List<String> THE_PLAYER = new ArrayList<>();
    private static List<String> THE_ENEMY = new ArrayList<>();
    
    public static final String PATH = "tournament/";
    public static final String ROUND_DATA = PATH+"round_data.csv";
    public static final String ROUND_MATCHES = PATH+"TEST_matches.csv";
    public static final String ROUND_BLACKLIST = PATH+"tournament_blacklist.csv";
    
    private static int ROUND, MAX_WAVE, BATTLESCAPE, WINNER=-1, LOSER=-1, SUPPLIES;
    private static float HIKE;
    private static boolean MAINTENANCE, CLOCK, VS, NO_RETREAT, FLAGSHIP, BLACKLIST, ANTISPAM, HULLMOD, MAX_CR, SOUND=false, END=false, SHAME=false;

    private static float mapX, mapY, timer=0;
    
    //Viability Test additions
    private static boolean STATUS, SHOW_DP, SHOW_COST, overridePersonality = true;    
    private static int teamA=100, teamB=100;
    private static IntervalUtil status = new IntervalUtil(0.2f,0.3f);   
    
    private static int goalDP = 180, toleranceDP = 1, TIMEOUT, vanilla=0, vanillaBM=1, CR=70;
    private static String factionToTest, factionBenchmark;
    public static final String BENCHMARK_DATA_PATH = "tournament/benchmark_fleet";
    private static CampaignFleetAPI testFleet, benchmarkFleet;
    private static boolean refreshFleet = true, boostTime = false, useInflater = false, useBlacklist = false, useBlacklistBM = true;
    private static Map<Integer, String> testMATCHES = new HashMap<>();
    private static Map<Integer, String> benchmarkMATCHES = new HashMap<>();
    private static long inflaterSeed = new Random().nextLong();
    
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        
        //cleanup
        BLOCKED.clear();
        FLAGSHIPS.clear();
        PLAYER_FLEET.clear();
        ENEMY_FLEET.clear();
        PREVIOUS_FLEET.clear();
        PLAYER_SHIPS.clear();
        ENEMY_SHIPS.clear();
        THE_PLAYER.clear();
        THE_ENEMY.clear();
        clock=0;
        reveal=false;//fixes timeout bug
        cr=false;//fixes CR only being applied once
        overridePersonality = true;
        
        //read the matches data from CSVs
        createMatches();
        
        //cycle the matches while holding space        
        if(first){
            first = false;
            match=0;
        } else if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)){
            refreshFleet = true;
            match++;
            if(match>=testMATCHES.size()){
                match=0;
            }
        } else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)){
            refreshFleet = true;
            match--;
            if(match<0){
                match=testMATCHES.size()-1;
            }
        } 
        
        // VIABILITY TEST ADDITIONS BELOW //
        //Refresh with Space button
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            refreshFleet = true;
        //Increase DP with Up Arrow
        } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            goalDP = Math.min(goalDP + 5, 600);
            refreshFleet = true;
        //Decrease DP with Down Arrow
        } else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            goalDP = Math.max(goalDP - 5, 5);
            refreshFleet = true;
        }       

        //Toggle Fleet Inflater with F
        if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
            useInflater = !useInflater;
        }
        //Toggle Time Boost with T
        if (Keyboard.isKeyDown(Keyboard.KEY_T)) {
            boostTime = !boostTime;
        }
        //Toggle blacklist with B, LSHIFT changes the benchmark fleet.
        if (Keyboard.isKeyDown(Keyboard.KEY_B)) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                useBlacklistBM = !useBlacklistBM;
                refreshFleet = true;
            } else {
                useBlacklist = !useBlacklist;
                refreshFleet = true;
            }
        }
        //Toggle personality override with R
        if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
            overridePersonality = !overridePersonality;
        }
        //Toggle hide/show/onlyShow vanilla ships with N, LSHIFT changes the benchmark fleet
        if (Keyboard.isKeyDown(Keyboard.KEY_N)) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                vanillaBM++;
                if (vanillaBM > 1) {
                    vanillaBM = -1;
                }
                refreshFleet = true; 
            } else {
                vanilla++;
                if (vanilla > 1) {
                    vanilla = -1;
                }
                refreshFleet = true;
            }
        }

        //Briefing Items
        String briefingItems = "Enabled features: ";
        if (useInflater) {
            briefingItems += "autofit, ";
        }      
        if (boostTime) {
            briefingItems += "time acceleration, ";
        }
        if (useBlacklist) {
            briefingItems += "blacklist, ";
        }
        if (overridePersonality) {
            briefingItems += "personalities overwritten (both), ";
        }
        switch (vanilla) {
            case -1:
                briefingItems += "hide vanilla";
                break;
            case 1:
                briefingItems += "only show vanilla";
                break;
            default:
                break;
        }
        api.addBriefingItem(briefingItems);   
        
        //Briefing Items 2
        String briefingItems2 = "Enabled features (benchmark): ";
        if (useBlacklistBM) {
            briefingItems2 += "blacklist, ";
        }        
        switch (vanillaBM) {
            case -1:
                briefingItems2 += "hide vanilla";
                break;
            case 1:
                briefingItems2 += "only show vanilla";
                break;
            default:
                break;
        }
        
        //SETUP PLAYER FLEET
        //List<String> thePlayer = new ArrayList<>();
        THE_PLAYER.add(0,"Test");
        THE_PLAYER.add(1,"TEST");
        FactionAPI testFaction = (FactionAPI) Global.getSector().getFaction(testMATCHES.get(match));
        log.info("Test faction loaded: "+testFaction);
        String testFleetTag = testFaction.getDisplayName()+" Test Fleet with "+goalDP+" DP";
        THE_PLAYER.add(2,testFleetTag);
        
        api.initFleet(FleetSide.PLAYER, THE_PLAYER.get(1), FleetGoal.ATTACK, true, ROUND);
        api.setFleetTagline(FleetSide.PLAYER, THE_PLAYER.get(2));  
        
        //SETUP ENEMY FLEET
        //List<String> theEnemy = new ArrayList<>();
        THE_ENEMY.add(0,"Benchmark");
        THE_ENEMY.add(1,"BCHMK");
        FactionAPI benchmarkFaction = Global.getSector().getFaction(benchmarkMATCHES.get(match));
        log.info("Benchmark faction loaded: "+benchmarkFaction);
        String benchmarkFleetTag = benchmarkFaction.getDisplayName()+" Benchmark Fleet with "+goalDP+" DP";
        THE_ENEMY.add(2,benchmarkFleetTag);
        
        api.initFleet(FleetSide.ENEMY, THE_ENEMY.get(1), FleetGoal.ATTACK, true, ROUND);
        api.setFleetTagline(FleetSide.ENEMY, THE_ENEMY.get(2));               
        
        //CREATE NEW FLEETS
        CampaignFleetAPI playerFleet;
        CampaignFleetAPI enemyFleet;
        boolean changeInflaterSeed = false;
        if (refreshFleet) {
            log.info("Refreshing fleet...");
            refreshFleet = false;
            changeInflaterSeed = true;
            testFleet = createRandomFleet(Global.getSector().getFaction(testMATCHES.get(match).toString()), goalDP, toleranceDP, useBlacklist, vanilla);
            benchmarkFleet = createRandomFleet(Global.getSector().getFaction(benchmarkMATCHES.get(match).toString()), goalDP, toleranceDP, useBlacklistBM, vanillaBM);
        }   
        
        //LOAD BENCHMARK FLEET FROM DATA
        String fleetData = "";
        if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
            log.info("Loading fleet from save...");
            try {
                fleetData = Global.getSettings().readTextFileFromCommon(BENCHMARK_DATA_PATH);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(MissionDefinition.class.getName()).log(Level.SEVERE, null, ex);
            }
            String[] tmp = fleetData.split(",,");            
            
            benchmarkFleet = FleetFactoryV3.createEmptyFleet(benchmarkMATCHES.get(match).toString(), null, null);            
            for (int i = 2; i < tmp.length; i=i+2) {
                FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, tmp[i]);
                ship.setShipName(tmp[i+1]);
                benchmarkFleet.getFleetData().addFleetMember(ship);
            }                
            inflaterSeed = Long.parseLong(tmp[1]);
            log.info("Loading inflater seed: "+inflaterSeed);
            briefingItems2 += "BENCHMARK FLEET LOADED, ";
            api.setFleetTagline(FleetSide.ENEMY, tmp[0]);
        }                   
        
        //CREATE THE FLEETS PROPER
        playerFleet = testFleet;
        enemyFleet = benchmarkFleet;  
        
        //SAVE BENCHMARK FLEET DATA
        if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
            log.info("Saving fleet to common/"+BENCHMARK_DATA_PATH);
            fleetData = "Saved "+benchmarkFleet.getFaction().getDisplayName()+" Benchmark Fleet with "+goalDP+" DP,,"+inflaterSeed+",,";
            for (FleetMemberAPI member : benchmarkFleet.getFleetData().getMembersInPriorityOrder()) {
                if (member.getVariant().getOriginalVariant() != null) {
                    fleetData = fleetData + member.getVariant().getOriginalVariant()+",,";
                } else {
                    fleetData = fleetData + member.getVariant().getHullVariantId()+",,";
                }
                fleetData = fleetData + member.getShipName()+",,";
            }
            try {
                Global.getSettings().writeTextFileToCommon(BENCHMARK_DATA_PATH, fleetData);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(MissionDefinition.class.getName()).log(Level.SEVERE, null, ex);
            }
            log.info("Fleet data saved as: "+fleetData);
            briefingItems2 += "BENCHMARK FLEET SAVED, ";
        }                

        api.addBriefingItem(briefingItems2);         
        
        /*
        //DEBUG STUFF
        String data = "";
        for (FleetMemberAPI m : enemyFleet.getFleetData().getMembersInPriorityOrder()) {
            if (m.getVariant().getOriginalVariant() != null) {
                data += m.getVariant().getOriginalVariant();
            } else {
                data += m.getVariant().getHullVariantId();
            }
            data += ",";
        }
        log.info("enemyFleet info: "+data);
        data = "";
        for (FleetMemberAPI m : benchmarkFleet.getFleetData().getMembersInPriorityOrder()) {
            if (m.getVariant().getOriginalVariant() != null) {
                data += m.getVariant().getOriginalVariant();
            } else {
                data += m.getVariant().getHullVariantId();
            }
            data += ",";
        }
        log.info("benchmarkFleet info: "+data);
        */
        
        // Add fleets to mission and set personalities.
        FleetMemberAPI temp;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersInPriorityOrder()) {
            if (member.getVariant().getOriginalVariant() == null) {
                member.getVariant().setOriginalVariant(member.getVariant().getHullVariantId());
            }          
            member.setVariant(Global.getSettings().getVariant(member.getVariant().getOriginalVariant()), false, false);
            
            member.getStatus().repairFully();
            member.updateStats();
            
            //sneaking suspicion that adding personalities here doesn't work.
            api.addFleetMember(FleetSide.PLAYER, member);
            //Yep. Fuck. It uses the faction doctrine's aggression (1-5, as in campaign).
            //Can't use below either, or the inflater doesn't work. 
            //Had to force personalities in the combat engine's advance plugin. 
            
            /*
            //temp = api.addToFleet(FleetSide.PLAYER, member.getVariant().getOriginalVariant(), FleetMemberType.SHIP, false);  
            if (temp.isCarrier() && !temp.getVariant().isCombat()) {
                temp.getCaptain().setPersonality("cautious");
            } else {
                temp.getCaptain().setPersonality("reckless");
            }
            temp.getStatus().repairFully();
            temp.updateStats();
            PLAYER_FLEET.add(temp);
            */
            
            PLAYER_FLEET.add(member);
        }          
        for (FleetMemberAPI member : enemyFleet.getFleetData().getMembersInPriorityOrder()) {
            if (member.getVariant().getOriginalVariant() == null) {
                member.getVariant().setOriginalVariant(member.getVariant().getHullVariantId());
            }
            member.setVariant(Global.getSettings().getVariant(member.getVariant().getOriginalVariant()), false, false);

            member.getStatus().repairFully();
            member.updateStats();     
            //sneaking suspicion that adding personalities here doesn't work.
            api.addFleetMember(FleetSide.ENEMY, member);
            //Yep. Fuck. Can't use below either, or the inflater doesn't work. Had to force personalities in the combat engine's advance plugin.            

            /*
            temp = api.addToFleet(FleetSide.ENEMY, member.getVariant().getOriginalVariant(), FleetMemberType.SHIP, false);              
            if (temp.isCarrier() && !temp.getVariant().isCombat()) {
                temp.getCaptain().setPersonality("cautious");
            } else {
                temp.getCaptain().setPersonality("reckless");
            }
            temp.getStatus().repairFully();
            temp.updateStats();
            ENEMY_FLEET.add(temp);       
            */
            ENEMY_FLEET.add(member);
        }
        
        //Inflater
        if (useInflater) {
            log.info("Inflating fleets...");
            DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
            p.quality = 1.25f;
            p.mode = FactionAPI.ShipPickMode.ALL;    
            //use new seed only if refreshing the fleet
            if (changeInflaterSeed) {
                p.seed = new Random().nextLong();
                inflaterSeed = p.seed;
            } else {
                p.seed = inflaterSeed;
            }
            DefaultFleetInflater inflater = new DefaultFleetInflater(p);
            inflater.inflate(playerFleet);
            inflater.inflate(enemyFleet);
        }
        // VIABILITY TEST ADDITIONS ABOVE //
        
        //
        //PvP    
        
        /*
        player=Math.round(MATCHES.get(match).x);        
        List<String> thePlayer = AI_missionUtils.createPlayerJSON(PATH, player);
        
        api.initFleet(FleetSide.PLAYER, thePlayer.get(1), FleetGoal.ATTACK, true, ROUND);
        api.setFleetTagline(FleetSide.PLAYER, thePlayer.get(2)); 
        
        
        Map<FleetMemberAPI,Boolean> theFleet;
        
        for(int i=0; i<=ROUND; i++){
            theFleet = AI_missionUtils.addRoundReturnFlagship(api, FleetSide.PLAYER, i, player, PATH, null);
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
        
        enemy=Math.round(MATCHES.get(match).y);
        List<String> theEnemy = AI_missionUtils.createPlayerJSON(PATH, enemy);
        
        api.initFleet(FleetSide.ENEMY, theEnemy.get(1), FleetGoal.ATTACK, true, ROUND);
        api.setFleetTagline(FleetSide.ENEMY, theEnemy.get(2)); 
                 
        for(int i=0; i<=ROUND; i++){
            theFleet = AI_missionUtils.addRoundReturnFlagship(api, FleetSide.ENEMY, i, enemy, PATH, null);
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
        */
        
//        int playerCost = AI_missionUtils.FleetCost(
//                Global.getCombatEngine(),
//                thePlayer.get(0),
//                PLAYER_FLEET,
//                HULLMOD,
//                FLAGSHIP,
//                FLAGSHIPS,
//                ANTISPAM,
//                HIKE,
//                SPAM_THRESHOLD,
//                BLACKLIST,
//                BLOCKED,
//                MAINTENANCE,
//                PREVIOUS_FLEET,
//                SUPPLIES);  
//        
//        int enemyCost = AI_missionUtils.FleetCost(
//                Global.getCombatEngine(),
//                theEnemy.get(0),
//                ENEMY_FLEET,
//                HULLMOD,
//                FLAGSHIP,
//                FLAGSHIPS,
//                ANTISPAM,
//                HIKE,
//                SPAM_THRESHOLD,
//                BLACKLIST,
//                BLOCKED,
//                MAINTENANCE,
//                PREVIOUS_FLEET,
//                SUPPLIES); 

/*      CREDIT CALCULATIONS
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
            api.addBriefingItem(thePlayer.get(0)+" : "+playerCost);
        } else {            
            api.addBriefingItem(thePlayer.get(0)+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
        api.addBriefingItem("  VERSUS");
        if(enemyCost>0){
            api.addBriefingItem(theEnemy.get(0)+" : "+enemyCost);
        } else {            
            api.addBriefingItem(theEnemy.get(0)+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
*/

//      DP CALCULATIONS        
        int playerDPCost = AI_missionUtils.DPFleetCost(Global.getCombatEngine(),
                THE_PLAYER.get(0),
                PLAYER_FLEET,
                BLACKLIST,
                BLOCKED
        );
        
        int enemyDPCost = AI_missionUtils.DPFleetCost(Global.getCombatEngine(),
                THE_ENEMY.get(0),
                ENEMY_FLEET,
                BLACKLIST,
                BLOCKED
        );        

        //setup Briefing
        if(playerDPCost>0 //&& playerCost>0
                ){//negative values indicate error, probably blacklist
            String costOuput = "";
            if(SHOW_DP){
                costOuput = costOuput + THE_PLAYER.get(0)+" DP : "+playerDPCost;
            }
/*            
            if(SHOW_COST){
                costOuput = costOuput +"    Credits : "+playerCost;
            }
*/
            api.addBriefingItem(costOuput);
        } else {            
            api.addBriefingItem(THE_PLAYER.get(0)+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
        //api.addBriefingItem("  VERSUS");
        if(enemyDPCost>0 //&& enemyCost>0
                ){
            String costOuput = "";
            if(SHOW_DP){
                costOuput = costOuput + THE_ENEMY.get(0)+" DP : "+enemyDPCost;
            }
/*
            if(SHOW_COST){
                costOuput = costOuput +"    Credits : "+enemyCost;
            }
*/
            api.addBriefingItem(costOuput);
        } else {            
            api.addBriefingItem(THE_ENEMY.get(0)+" : BLACKLISTED ELEMENT DETECTED!!!!");
        }
        
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
            
            //add loads of deployment points to preven the AI from holding ships
            Global.getCombatEngine().setMaxFleetPoints(FleetSide.ENEMY, 9999);
            Global.getCombatEngine().setMaxFleetPoints(FleetSide.PLAYER, 9999);
            
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

            /////////////////////////////////////////
            //                                     //
            //          TIME ACCELERATION          //
            //                                     //
            /////////////////////////////////////////
            // Code by Dark.Revenant
            float newTimeMult;
            if (boostTime) {
                if (Global.getCombatEngine().isPaused()) {
                    return;
                }

                float trueFrameTime = Global.getCombatEngine().getElapsedInLastFrame();
                float trueFPS = 1 / trueFrameTime;
                newTimeMult = Math.max(1f, trueFPS / 30f);
                Global.getCombatEngine().getTimeMult().modifyMult("viability_tester", newTimeMult);
                
                engine.maintainStatusForPlayerShip("boostTime", null, "Time Acceleration", Math.round(newTimeMult*100)+"%", true);
            }
            
            ////////////////////////////////////
            //                                //
            //          FORCED SPAWN          //
            //                                //
            ////////////////////////////////////  
            
            if(!deploy){
                
                deploy=true;                
                    
                log.info("Map size: "+(int)mapX+"x"+(int)mapY);  
                
                //public static void ForcedSpawn(CombatEngineAPI engine, FleetSide side, float mapX, float mapY, boolean suppressMessage)
                AI_missionUtils.ForcedSpawn(engine, FleetSide.PLAYER, mapX, mapY, true, CR);                
                AI_missionUtils.ForcedSpawn(engine, FleetSide.ENEMY, mapX, mapY, true, CR);                
                
                //fleet status list
                for(FleetMemberAPI m : PLAYER_FLEET){
                    PLAYER_SHIPS.add(engine.getFleetManager(FleetSide.PLAYER).getShipFor(m));
                }
                for(FleetMemberAPI m : ENEMY_FLEET){
                    ENEMY_SHIPS.add(engine.getFleetManager(FleetSide.ENEMY).getShipFor(m));
                }                    
                return;  
            }
            
            //yeah, I know this was written awfully; I just want to get it done though.
            if (deploy & overridePersonality) {
                for (ShipAPI ship : engine.getShips()) {
                    if (ship.getCaptain() != null) {
                        if (ship.getFleetMember().isCarrier() && !ship.getVariant().isCombat()) {
                            ship.getCaptain().setPersonality("cautious");
                            log.info(ship.getVariant().getFullDesignationWithHullName()+" personality set to cautious.");
                            overridePersonality = false;
                        } else {
                            ship.getCaptain().setPersonality("reckless");
                            log.info(ship.getVariant().getFullDesignationWithHullName()+" personality set to reckless.");
                            overridePersonality = false;
                        }
                    }
                }                
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

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+player),
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

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemy),
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
                    
                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+player),
                            MagicRender.positioning.CENTER,
                            new Vector2f(128+(time*3000),-160),
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

                    MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+enemy),
                            MagicRender.positioning.CENTER,
                            new Vector2f(-128-(time*3000),160),
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
            
            
            float setCR = CR;
            if(!cr){
                cr=true;
                if(MAX_CR){
                    for(ShipAPI s : engine.getShips()){
                        s.setCurrentCR(1);
                        log.info(s.getId()+" CR set to 100 percent");
                    }
                } else {
                    for(ShipAPI s : engine.getShips()){
                        /*
                        float CR = Math.max(0, Math.min(1,0.7f*s.getMutableStats().getMaxCombatReadiness().computeMultMod()));
                        s.setCurrentCR(CR);
                        for(String m : s.getMutableStats().getMaxCombatReadiness().getMultMods().keySet()){
                            log.info(s.getHullSpec().getHullName()+" "+s.getVariant().getDisplayName()+" CR modified by "+m);
                        }
                        log.info(s.getHullSpec().getHullName()+" "+s.getVariant().getDisplayName()+" CR set to "+CR*100+" percent");
                        */
                        {
                            setCR = 70f;
                        }
                        s.setCurrentCR(setCR/100);
                        log.info(s.getHullSpec().getHullName()+" "+s.getVariant().getDisplayName()+" CR set to "+setCR+" percent");
                    }
                }
            }
                        
            ////////////////////////////////////
            //                                //
            //         ANTI-CR BATTLE         //
            //                                //
            ////////////////////////////////////    
            
            if (engine.getTotalElapsedTime(false)>TIMEOUT){
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
                    //do it twice 'cuz it's maybe bugged
                    if(!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).isFullAssault()){
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(true);
                    }
                    
                    if(!engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).isFullAssault()){
                        engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(true);
                    }
                    
                    if(!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).isFullAssault()){
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(true).setFullAssault(true);
                    }
                    
                    if(!engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).isFullAssault()){
                        engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(true);
                    }
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
            //          FLEET STATUS          //
            //                                //
            ////////////////////////////////////            
            
            if(STATUS){
                status.advance(amount);
                if(status.intervalElapsed()){
                    teamA = AI_missionUtils.fleetStatusUpdateDP(PLAYER_SHIPS);
                    teamB = AI_missionUtils.fleetStatusUpdateDP(ENEMY_SHIPS);
                }            
                engine.maintainStatusForPlayerShip("status2",
                        "graphics/AIB/icons/hullsys/AI_teamSupport.png",
                        THE_ENEMY.get(0)+" fleet status:",
                        teamB+"% DP remaining.",
                        teamB<25
                );
                
                engine.maintainStatusForPlayerShip("status1",
                        "graphics/AIB/icons/hullsys/AI_teamLead.png",
                        THE_PLAYER.get(0)+" fleet status:",
                        teamA+"% DP remaining.",
                        teamA<25
                );
                
            }
            
            ////////////////////////////////////
            //                                //
            //         VICTORY SCREEN         //
            //                                //
            ////////////////////////////////////
            
            //getting log results
            timer+=engine.getElapsedInLastFrame();

            int playerAlive=0, enemyAlive=0, flawless=0;
            if(timer>1&& !END){
                timer=0;
                playerAlive = 0; enemyAlive = 0; flawless = 0;

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
                    //player dead
                    END=true;
                    WINNER=enemy;
                    LOSER=player;
                    //check for enemy losses
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
                    //no loss? Shaming defeat
                    if (flawless<1){
                        SHAME=true;
                    }                    
                } else if(enemyAlive==0){
                    END=true;
                    WINNER=player;
                    LOSER=enemy;
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
                if (END) {
                    log.info("__________________________");
                    log.info("__________________________");
                    log.info("COMBAT ENDED");
                    log.info("TIME ELAPSED: "+clock);
                    if (enemyAlive == 0) {
                        log.info("WINNER: PLAYER TEAM");
                        teamA = AI_missionUtils.fleetStatusUpdateDP(PLAYER_SHIPS);
                        log.info("Remaining DP%: "+teamA);
                    } else {
                        log.info("WINNER: ENEMY TEAM");
                        teamB = AI_missionUtils.fleetStatusUpdateDP(ENEMY_SHIPS);
                        log.info("Remaining DP%: "+teamB);
                    }             
                    log.info("__________________________");
                    log.info("__________________________");
                }
            }
 
            if(VS){                
                if(END){                    
                    if(WINNER>-1){
                        //display winner                    
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(64,128),
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
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+WINNER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(64,128),
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
                        WINNER=-2;
                    }

                    if(WINNER<-1 && timer>0.5f){
                        WINNER=-1;
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

                    if(SHAME && LOSER>-1 && timer>1f){
                        //display loser            
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-64,-128),
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
                        MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_player"+LOSER),
                                MagicRender.positioning.CENTER,
                                new Vector2f(-64,-128),
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

                        //reset loser
                        LOSER=-1;
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
            log.info("Importing data:");
            JSONArray round = Global.getSettings().getMergedSpreadsheetDataForMod("round", ROUND_DATA, "aibattles");
            log.info("Parsing row");
            JSONObject row = round.getJSONObject(0);
            log.info("Reading round");
            ROUND=row.getInt("round");
            
            log.info("Reading battlescape");
            BATTLESCAPE=row.getInt("battlescape");
            
            log.info("Reading vsScreen");
            VS=row.getBoolean("vsScreen");
            
            log.info("Reading waves");
            MAX_WAVE=row.getInt("waves");
            
            log.info("Reading noRetreat");
            NO_RETREAT=row.getBoolean("noRetreat");
            
            log.info("Reading flagshipMode");
            FLAGSHIP=row.getBoolean("flagshipMode");
            
            log.info("Reading buyInHullmods");
            HULLMOD=row.getBoolean("buyInHullmods");
            
            log.info("Reading blacklist");
            BLACKLIST=row.getBoolean("blacklist");
            
            log.info("Reading antiSpam");
            ANTISPAM=row.getBoolean("antiSpam");
            
            log.info("Reading clock");
            CLOCK=row.getBoolean("clock");
            
            log.info("Reading status");
            STATUS=row.getBoolean("status");
            
            log.info("Reading maxCR");
            MAX_CR=row.getBoolean("maxCR");
            
            log.info("Reading maintenance");
            MAINTENANCE=row.getBoolean("maintenance");
            
            log.info("Reading supplies");
            SUPPLIES=row.getInt("supplies");
            
            log.info("Reading showDP");
            SHOW_DP=row.getBoolean("showDP");
            
            log.info("Reading showCost");
            SHOW_COST=row.getBoolean("showCost");
            
            log.info("Reading timeout");
            TIMEOUT=row.getInt("timeout");
            
            
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
            JSONArray matches = Global.getSettings().loadCSV(ROUND_MATCHES);
            log.info(ROUND_MATCHES+" loaded.");
            for(int i = 0; i < matches.length(); i++) {
            
                JSONObject row = matches.getJSONObject(i);      
                /*
                player = row.getInt("playerA");
                enemy = row.getInt("playerB");
                
                MATCHES.put(2*i, new Vector2f(player,enemy));  
                MATCHES.put(2*i+1, new Vector2f(enemy,player));
                */
                
                factionToTest = row.getString("playerA");
                factionBenchmark = row.getString("playerB");
                
                testMATCHES.put(i,factionToTest);
                benchmarkMATCHES.put(i,factionBenchmark);
                
            }
                log.info("Testing faction: "+testMATCHES);
                log.info("Benchmark faction: "+benchmarkMATCHES);            
        } catch (IOException | JSONException ex) {
            log.error("unable to read "+ROUND_MATCHES);
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

    ////////////////////////////////////
    //                                //
    //      RANDOM FLEET CREATOR      //
    //                                //
    ////////////////////////////////////     
    //My apologies to anyone who has to read this.
    /**
     *
     * @param faction faction to build fleet from
     *
     * @param goalDP goal DP to aim for
     * 
     * @param tolerance how far above max DP we can go
     *
     * @param blacklist disallow blacklisted ships (for test fleets)
     *
     * @param vanilla -1: disallow vanilla ships; 1: only allow vanilla ships
     *
     * @return
     */    
    public CampaignFleetAPI createRandomFleet(FactionAPI faction, int goalDP, int tolerance, boolean blacklist, int vanilla) {
        log.info("____________________________________________________");
        log.info("Creating random fleet for: "+faction.getDisplayName());
        log.info("____________________________________________________");
        //List of ship roles that are good for testing
        List<String> shipRolesList = new ArrayList<>();
            shipRolesList.add(ShipRoles.COMBAT_SMALL); shipRolesList.add(ShipRoles.PHASE_SMALL);
            shipRolesList.add(ShipRoles.COMBAT_MEDIUM); shipRolesList.add(ShipRoles.CARRIER_SMALL); shipRolesList.add(ShipRoles.PHASE_MEDIUM);
            shipRolesList.add(ShipRoles.COMBAT_LARGE); shipRolesList.add(ShipRoles.CARRIER_MEDIUM); shipRolesList.add(ShipRoles.PHASE_LARGE);
            shipRolesList.add(ShipRoles.COMBAT_CAPITAL); shipRolesList.add(ShipRoles.CARRIER_LARGE); shipRolesList.add(ShipRoles.PHASE_CAPITAL);        
        
        List<String> vanillaShips = Arrays.asList(
                ("afflictor, afflictor_d_pirates, apogee, astral, atlas, atlas2, aurora, "
                + "bastillon, berserker, brawler, brawler_pather, brawler_tritachyon, brilliant, buffalo, buffalo2, buffalo_d, "
                + "buffalo_hegemony, buffalo_luddic_church, buffalo_pirates, buffalo_tritachyon, centurion, cerberus, cerberus_d, cerberus_d_pirates, "
                + "cerberus_luddic_path, colossus, colossus2, colossus3, condor, conquest, defender, dominator, dominator_d, dominator_xiv, doom, dram, drover, "
                + "eagle, eagle_d, eagle_xiv, enforcer, enforcer_d, enforcer_d_pirates, enforcer_xiv, falcon, falcon_d, falcon_p, falcon_xiv, fulgent, "
                + "gemini, glimmer, gremlin, gremlin_d_pirates, gremlin_luddic_path, gryphon, guardian, hammerhead, hammerhead_d, harbinger, "
                + "hermes, hermes_d, heron, hound, hound_d, hound_d_pirates, hound_hegemony, hound_luddic_church, hound_luddic_path, hyperion, "
                + "kite, kite_d, kite_hegemony, kite_luddic_path, kite_pirates, lasher, lasher_d, lasher_luddic_church, lasher_pather, "
                + "legion, legion_xiv, lumen, medusa, mercury, mercury_d, monitor, mora, mudskipper, mudskipper2, mule, mule_d, mule_d_pirates, "
                + "nebula, odyssey, omen, onslaught, onslaught_xiv, paragon, phaeton, picket, prometheus, prometheus2, radiant, rampart, "
                + "scarab, scintilla, sentry, shade, shade_d_pirates, shepherd, shrike, shrike_pirates, starliner, sunder, sunder_d, "
                + "tarsus, tarsus_d, tempest, valkyrie, venture, vigilance, warden, wayfarer, wolf, wolf_d, wolf_d_pirates, wolf_hegemony").split(", "));
            //log.info(vanillaShips);
            
        //Adjust weights based on faction's ship size.
        List<Float> shipSizeTable1 = new ArrayList<>();
            shipSizeTable1.add(1.5f); shipSizeTable1.add(1.25f); shipSizeTable1.add(0.8f); shipSizeTable1.add(0.67f);
        List<Float> shipSizeTable2 = new ArrayList<>();
            shipSizeTable2.add(1.25f); shipSizeTable2.add(1f); shipSizeTable2.add(1f); shipSizeTable2.add(0.8f);
        List<Float> shipSizeTable3 = new ArrayList<>();
            shipSizeTable3.add(1f); shipSizeTable3.add(1f); shipSizeTable3.add(1f); shipSizeTable3.add(1f);
        List<Float> shipSizeTable4 = new ArrayList<>();
            shipSizeTable4.add(0.8f); shipSizeTable4.add(1f); shipSizeTable4.add(1f); shipSizeTable4.add(1.25f);
        List<Float> shipSizeTable5 = new ArrayList<>();
            shipSizeTable5.add(0.67f); shipSizeTable5.add(0.8f); shipSizeTable5.add(1.25f); shipSizeTable5.add(1.5f);
        
        List<List<Float>> weightedShipSizeTable = new ArrayList<>();
            weightedShipSizeTable.add(shipSizeTable1);
            weightedShipSizeTable.add(shipSizeTable2);
            weightedShipSizeTable.add(shipSizeTable3);
            weightedShipSizeTable.add(shipSizeTable4);
            weightedShipSizeTable.add(shipSizeTable5);

        Float frigateMult = weightedShipSizeTable.get(faction.getDoctrine().getShipSize()-1).get(0);
        Float destroyerMult = weightedShipSizeTable.get(faction.getDoctrine().getShipSize()-1).get(1);
        Float cruiserMult = weightedShipSizeTable.get(faction.getDoctrine().getShipSize()-1).get(2);
        Float capitalMult = weightedShipSizeTable.get(faction.getDoctrine().getShipSize()-1).get(3);
            
        List<Float> weightedTypeTable = new ArrayList<>();
            //weightedTypeTable.add(0f); weightedTypeTable.add(0.67f); weightedTypeTable.add(0.8f); weightedTypeTable.add(1f); weightedTypeTable.add(1.25f); weightedTypeTable.add(1.5f);
            weightedTypeTable.addAll(Arrays.asList(0f, 0.67f, 0.8f, 1f, 1.25f, 1.5f));
            
        Float combatMult = weightedTypeTable.get(Math.min(faction.getDoctrine().getWarships(),5));
        Float carrierMult = weightedTypeTable.get(Math.min(faction.getDoctrine().getCarriers(),5));
        Float phaseMult = weightedTypeTable.get(Math.min(faction.getDoctrine().getPhaseShips(),5));
        
        LinkedHashMap<String, Float> weightedShipRolesList = new LinkedHashMap<>();
        for (String role : shipRolesList) {
            boolean isCarrier = role.startsWith("carrier");
            float mult = 1;
            if (role.endsWith("Small")) {
                if (!isCarrier) {
                    mult *= frigateMult;
                } else {
                    mult *= destroyerMult;
                }
            }
            else if (role.endsWith("Medium")) {
                if (!isCarrier) {
                    mult *= destroyerMult;
                } else {
                    mult *= cruiserMult;
                }
            }
            else if (role.endsWith("Large")) {
                if (!isCarrier) {
                    mult *= cruiserMult;
                } else {
                    mult *= capitalMult;
                }
            }
            else if (role.endsWith("Capital")) {
                mult *= capitalMult;
            }
            if (role.startsWith("combat")) {
                mult *= combatMult;
            }
            else if (isCarrier) {
                mult *= carrierMult;
            }
            else if (role.startsWith("phase")) {
                mult *= phaseMult;
            }
            log.info("Multiplier for "+role+" is: "+mult);
            weightedShipRolesList.put(role, mult*faction.getNumAvailableForRole(role, FactionAPI.ShipPickMode.ALL));                
        }

        float totalWeight = 0;
        for (String role : weightedShipRolesList.keySet()) {
            totalWeight += weightedShipRolesList.get(role);
        }        
        log.info("Total weight for all roles is: "+totalWeight);
        
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(faction.getId(), null, null);
        int usedDP = 0;
        float numFails = 0;
        while (usedDP < goalDP-tolerance && numFails/2 < totalWeight) {
            //Pick a random role
            Random random = new Random();
            //have to use half numfails because the weight of a single ship can be 4/9.
            float randomPick = (float) ( Math.random()*(totalWeight - numFails/2) );
            //log.info("Random pick value: "+randomPick);
            String randomRole = shipRolesList.get( random.nextInt(shipRolesList.size()) );
            for (String role : weightedShipRolesList.keySet()) {
                randomPick -= weightedShipRolesList.get(role);
                if (randomPick <= 0.0f) {
                    randomRole = role;
                    //log.info("Random role chosen: "+randomRole);
                    break;
                }
            }
            //Add a ship from that role into the fleet.
            boolean removeShip = false;
            String reason = "";
            float value = faction.pickShipAndAddToFleet(randomRole, new FactionAPI.ShipPickParams(FactionAPI.ShipPickMode.ALL), fleet, random);
            if (!(value == 0.0)) {
                //Check if new ship is value
                FleetMemberAPI newShip = (FleetMemberAPI) fleet.getFleetData().getMembersListCopy().get(fleet.getNumMembersFast()-1);
                usedDP += newShip.getDeploymentPointsCost();
                //getHullId() includes the skin name
                //getHullSpec().getBaseHullId() gives the original hull if its a skin.
                //check for ship removal
                if (usedDP > goalDP + tolerance) {
                    removeShip = true;
                    reason = "over DP limit";
                } else if (blacklist && BLOCKED.contains(newShip.getHullId())) {
                    removeShip = true;
                    reason = "blacklisted";
                } else {
                    switch (vanilla) {
                        //blacklist vanilla ships
                        case -1:
                            if (vanillaShips.contains(newShip.getHullId())) {
                                removeShip = true;
                                reason = "vanilla";
                            }
                            break;
                        //only vanilla ships
                        case 1:
                            if (!vanillaShips.contains(newShip.getHullId())) {
                                removeShip = true;
                                reason = "non-vanilla";
                            }
                            break;
                    }
                }
                //remove ship
                if (removeShip) {
                    usedDP -= newShip.getDeploymentPointsCost();
                    fleet.removeFleetMemberWithDestructionFlash(newShip);
                    numFails++;
                    //log.info("Added "+reason+" ship. Removing "+newShip.getVariant().getHullVariantId()+".");
                } else {
                    log.info("Added ship: "+newShip.getVariant().getHullVariantId()+".");
                }
            } else {
                numFails++;
                //log.info("Could not add ship for role"+randomRole+".");
                //log.info("Fail number: "+numFails+".");
            }
        }
        log.info("Fleet creation complete. DP used: "+usedDP+".");
        return fleet;
    }    
    
}
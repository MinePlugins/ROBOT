/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BootingBerzerk.java 2014-11-03 buixuan.
 * ******************************************************/
package algorithms;

import java.util.ArrayList;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class AttackMain extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;
  private static final double ANGLEPRECISION = 0.1;

  private static final int ROCKY = 0x1EADDA;
  private static final int MARIO = 0x5EC0;

  //---VARIABLES---//
  private boolean turnTask,turnRight,moveTask,berzerk,back;
  private double endTaskDirection,lastSeenDirection;
  private int endTaskCounter,berzerkInerty;
  private boolean firstMove,berzerkTurning;


    //Variables de tâches
    private static final int TURNLEFTTASK = 1;
    private static final int MOVETASK = 2;
    private static final int TURNRIGHTTASK = 3;
    private static final int SINK = 0xBADC0DE1;
  
    //variables d'etats du comportement
    private static final int HUNTER = 10;
    private static final int RUNNER = 15;
  
    //---VARIABLES---//
    private int state;
    private double oldAngle;
    private double myX,myY;
    private boolean isMoving;
    private boolean freeze;
    private int whoAmI;
    private int mode;
    private String target;
    private boolean initRunner;
    private int maxX, maxY;
    private double dist;
  
    private int countDown;
    private double targetX,targetY;
    private boolean fireOrder;
    private boolean friendlyFire;

  //---CONSTRUCTORS---//
  public AttackMain() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //HUNTER
    turnTask=true;
    moveTask=false;
    firstMove=true;
    berzerk=false;
    berzerkInerty=0;
    berzerkTurning=false;
    back=false;
    endTaskDirection=(Math.random()-0.5)*0.5*Math.PI;
    turnRight=(endTaskDirection>0);
    endTaskDirection+=getHeading();
    lastSeenDirection=Math.random()*Math.PI*2;
    if (turnRight) stepTurn(Parameters.Direction.RIGHT);
    else stepTurn(Parameters.Direction.LEFT);
    sendLogMessage("Turning point. Waza!");

    //RUNNER
    //ODOMETRY CODE
    whoAmI = ROCKY;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=MARIO;
    if (whoAmI == ROCKY){
      myX=Parameters.teamASecondaryBot1InitX;
      myY=Parameters.teamASecondaryBot1InitY;
    } else {
      myX=0;
      myY=0;
    }
    
    //INIT
    state = TURNLEFTTASK;
    mode=RUNNER;
    isMoving=false;
    oldAngle=getHeading();
  }
  public void step() {
    ArrayList<IRadarResult> radarResults = detectRadar();
    //MODE HUNTER
    if(mode == HUNTER)
    {
      if (berzerk) {
        if (berzerkTurning) {
          endTaskCounter--;
          if (isHeading(endTaskDirection)) {
            berzerkTurning=false;
            move();
            sendLogMessage("Moving a head. Waza!");
          } else {
            if (turnRight) stepTurn(Parameters.Direction.RIGHT);
            else stepTurn(Parameters.Direction.LEFT);
          }
          return;
        }

        if (berzerkInerty>50) {
          turnTask=true;
          moveTask=false;
          berzerk=false;
          endTaskDirection=(Math.random()-0.5)*2*Math.PI;
          turnRight=(endTaskDirection>0);
          endTaskDirection+=getHeading();
          if (turnRight) stepTurn(Parameters.Direction.RIGHT);
          else stepTurn(Parameters.Direction.LEFT);
          sendLogMessage("Turning point. Waza!");
          return;
        }
        if (endTaskCounter<0) {
          for (IRadarResult r : radarResults) {
            if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
              fire(r.getObjectDirection());
              lastSeenDirection=r.getObjectDirection();
              berzerkInerty=0;
              return;
            }
          }
          fire(lastSeenDirection);
          berzerkInerty++;
          endTaskCounter=21;
          return;
        } else {
          endTaskCounter--;
          for (IRadarResult r : radarResults) {
            if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
              lastSeenDirection=r.getObjectDirection();
              berzerkInerty=0;
              move();
              return;
            }
          }
          berzerkInerty++;
          move();
        }
        return;
      }
      if (radarResults.size()==0){
        mode = RUNNER;
      }
      if (radarResults.size()!=0){
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            berzerk=true;
            back=(Math.cos(getHeading()-r.getObjectDirection())>0);
            endTaskCounter=21;
            fire(r.getObjectDirection());
            lastSeenDirection=r.getObjectDirection();
            berzerkTurning=true;
            endTaskDirection=lastSeenDirection;
            double ref=endTaskDirection-getHeading();
            if (ref<0) ref+=Math.PI*2;
            turnRight=(ref>0 && ref<Math.PI);
            return;
          }
        }
      }
      if (turnTask) {
        if (isHeading(endTaskDirection)) {
          if (firstMove) {
            firstMove=false;
        turnTask=false;
            moveTask=true;
            endTaskCounter=400;
      move();
            sendLogMessage("Moving a head. Waza!");
            return;
          }
    turnTask=false;
          moveTask=true;
          endTaskCounter=100;
    move();
          sendLogMessage("Moving a head. Waza!");
        } else {
          if (turnRight) stepTurn(Parameters.Direction.RIGHT);
          else stepTurn(Parameters.Direction.LEFT);
        }
        return;
      }
      if (moveTask) {
        if (endTaskCounter<0) {
          turnTask=true;
          moveTask=false;
          endTaskDirection=(Math.random()-0.5)*2*Math.PI;
          turnRight=(endTaskDirection>0);
          endTaskDirection+=getHeading();
          if (turnRight) stepTurn(Parameters.Direction.RIGHT);
          else stepTurn(Parameters.Direction.LEFT);
          sendLogMessage("Turning point. Waza!");
        } else {
          endTaskCounter--;
          move();
        }
        return;
      }
      return;
    }

    //MODE RUNNER
    if(mode == RUNNER)
    {
      if (radarResults.size()!=0){
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            mode = HUNTER;
          }
        }
      }
      
      //AUTOMATON
      if (state==TURNLEFTTASK && !(isSameDirection(getHeading(),Parameters.EAST))) {
        stepTurn(Parameters.Direction.LEFT);
        return;
      }
      if (state==TURNLEFTTASK && isSameDirection(getHeading(),Parameters.EAST)) {
        state=MOVETASK;
        myMove();
        return;
      }

      if (state==MOVETASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
        myMove();
        return;
      }

      if (state==MOVETASK && (detectFront().getObjectType()==IFrontSensorResult.Types.WALL
      ||detectFront().getObjectType()==IFrontSensorResult.Types.TeamMainBot
      ||detectFront().getObjectType()==IFrontSensorResult.Types.TeamSecondaryBot
      ||detectFront().getObjectType()==IFrontSensorResult.Types.OpponentMainBot
      )) {
        state=TURNRIGHTTASK;
        oldAngle=getHeading();
        stepTurn(Parameters.Direction.RIGHT);
        return;
      }

      if (state==TURNRIGHTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE))) {
        stepTurn(Parameters.Direction.RIGHT);
        return;
      }

      if (state==TURNRIGHTTASK && isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE)) {
        state=MOVETASK;
        myMove();
        return;
      }

      if (state==SINK) {
        myMove();
        return;
      }
      if (true) {
        return;
      }
    }
  }

  //FUNCTION
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamAMainBotStepTurnAngle;
  }

  private void myMove(){
    isMoving=true;
    move();
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(normalize(dir1)-normalize(dir2))<ANGLEPRECISION;
  }
  private double normalize(double dir){
    double res=dir;
    while (res<0) res+=2*Math.PI;
    while (res>=2*Math.PI) res-=2*Math.PI;
    return res;
  }  

}

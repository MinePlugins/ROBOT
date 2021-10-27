package algorithms;

import java.util.ArrayList;
import java.util.Random;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

public class DefenseMain extends Brain {

  //---PARAMETERS---//
  private static final double ANGLEPRECISION = 0.1;
  private static final double FIREANGLEPRECISION = Math.PI/(double)6;

  private static final int ALPHA = 0x1EADDA;
  private static final int BETA = 0x5EC0;
  private static final int GAMMA = 0x333;
  private static final int TEAM = 0xBADDAD;
  private static final int UNDEFINED = 0xBADC0DE0;
  
  private static final int FIRE = 0xB52;
  private static final int FALLBACK = 0xFA11BAC;
  private static final int ROGER = 0x0C0C0C0C;
  private static final int OVER = 0xC00010FF;

  private static final int TURNSOUTHTASK = 1;
  private static final int MOVETASK = 2;
  private static final int TURNLEFTTASK = 3;
  private static final int TURNRIGHTTASK = 4;
  private static final int SINK = 0xBADC0DE1;

  //---VARIABLES---//
  private int state;
  private double oldAngle;
  private double myX,myY;
  private boolean isMoving;
  private int whoAmI;
  private int fireRythm,rythm,counter;
  private int countDown;
  private double targetX,targetY;
  private boolean fireOrder;
  private boolean freeze;
  private boolean start;
  private boolean friendlyFire;

  //---CONSTRUCTORS---//
  public DefenseMain() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    whoAmI = GAMMA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=ALPHA;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH) && whoAmI!=GAMMA) whoAmI=BETA;
    if (whoAmI == GAMMA){
      myX=Parameters.teamAMainBot1InitX;
      myY=Parameters.teamAMainBot1InitY;
    } else {
      myX=Parameters.teamAMainBot2InitX;
      myY=Parameters.teamAMainBot2InitY;
    }
    if (whoAmI == ALPHA){
      myX=Parameters.teamAMainBot3InitX;
      myY=Parameters.teamAMainBot3InitY;
    }

    //INIT
    state=MOVETASK;
    isMoving=false;
    start=true;
    fireOrder=false;
    fireRythm=0;
    oldAngle=myGetHeading();
    targetX=1500;
    targetY=1000;
  }
  public void step() {
    //ODOMETRY CODE
    if (isMoving){
      myX+=Parameters.teamAMainBotSpeed*Math.cos(myGetHeading());
      myY+=Parameters.teamAMainBotSpeed*Math.sin(myGetHeading());
      isMoving=false;
    }
    //DEBUG MESSAGE
    boolean debug=true;
    if (debug && whoAmI == ALPHA && state!=SINK) {
        if (start){
            state=TURNLEFTTASK;
            oldAngle=myGetHeading();
            stepTurn(Parameters.Direction.LEFT);
        }
        if (start && (int)(myGetHeading()*180/(double)Math.PI) == 90 ){
            start = false;
            state = MOVETASK;
            if (whoAmI == ALPHA){
                System.out.println("ICI7");
                }
        }
      sendLogMessage("#ALPHA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
    }
    if (debug && whoAmI == BETA && state!=SINK) {
      sendLogMessage("#BETA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
    }
    if (debug && whoAmI == GAMMA && state!=SINK) {
        if (start){
            state=TURNRIGHTTASK;
            oldAngle=myGetHeading();
            stepTurn(Parameters.Direction.RIGHT);
        }
        if ((int)(myGetHeading()*180/(double)Math.PI) >= 270 && (int)(myGetHeading()*180/(double)Math.PI) <= 275){
            start = false;
            state = MOVETASK;
        }
      sendLogMessage("#GAMMA *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
    }
    if (debug && fireOrder) sendLogMessage("Firing enemy!!");

    //COMMUNICATION
    ArrayList<String> messages=fetchAllMessages();
    for (String m: messages) if (Integer.parseInt(m.split(":")[1])==whoAmI || Integer.parseInt(m.split(":")[1])==TEAM) process(m);
    
    //RADAR DETECTION
    freeze=false;
    friendlyFire=true;
    for (IRadarResult o: detectRadar()){
      if (o.getObjectType()==IRadarResult.Types.OpponentMainBot || o.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
        double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
        double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
        broadcast(whoAmI+":"+TEAM+":"+FIRE+":"+enemyX+":"+enemyY+":"+OVER);
      }
      if (o.getObjectDistance()<=100 && !isRoughlySameDirection(o.getObjectDirection(),getHeading()) && o.getObjectType()!=IRadarResult.Types.BULLET) {
        freeze=true;
      }
      if (o.getObjectType()==IRadarResult.Types.TeamMainBot || o.getObjectType()==IRadarResult.Types.TeamSecondaryBot || o.getObjectType()==IRadarResult.Types.Wreck) {
        if (fireOrder && onTheWay(o.getObjectDirection())) {
          friendlyFire=false;
        }
      }
    }
    if (freeze) return;

    //AUTOMATON
    if (fireOrder) countDown++;
    if (countDown>=100) fireOrder=false;
    if (fireOrder && fireRythm==0 && friendlyFire) {
      firePosition(targetX,targetY);
      fireRythm++;
      return;
    }
    fireRythm++;
    if (fireRythm>=Parameters.bulletFiringLatency) fireRythm=0;
    if (state==TURNSOUTHTASK && !(isSameDirection(getHeading(),Parameters.SOUTH))) {
        if (whoAmI == ALPHA){
            System.out.println("ICI6");
            }
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
    if (state==MOVETASK && detectFront().getObjectType()==IFrontSensorResult.Types.OpponentMainBot || detectFront().getObjectType()==IFrontSensorResult.Types.OpponentSecondaryBot){
        freeze=true;
        if (whoAmI == ALPHA){
            System.out.println("ICI8");
            }
        return;
    }
    if (state==TURNSOUTHTASK && isSameDirection(getHeading(),Parameters.SOUTH)) {
      state=MOVETASK;
      myMove();
      if (whoAmI == ALPHA){
        System.out.println("ICI9");
        }
      return;
    }
    if (state==MOVETASK && myY >= 1700){
        if (whoAmI == ALPHA){
            System.out.println("PLOP: "+ (int)(myGetHeading()*180/(double)Math.PI));
            if ((int)(myGetHeading()*180/(double)Math.PI) >= 173 && (int)(myGetHeading()*180/(double)Math.PI) <= 182){
                state=MOVETASK;
                myMove();
                return;
            } else {
                state=TURNRIGHTTASK;
                oldAngle=myGetHeading();
                stepTurn(Parameters.Direction.RIGHT);

                return;

            }
            
        }
    }
    if (state==MOVETASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
      myMove();
      isMoving=true;
      if (whoAmI == ALPHA){
        // System.out.println("ICI10");
        }
      return;
    }


    if (state==MOVETASK && (detectFront().getObjectType()==IFrontSensorResult.Types.WALL || detectFront().getObjectType()==IFrontSensorResult.Types.OpponentMainBot  || detectFront().getObjectType()==IFrontSensorResult.Types.OpponentSecondaryBot)) {
      if (whoAmI == ALPHA){
        state=TURNRIGHTTASK;
      }
      if (whoAmI == GAMMA){
        state=TURNLEFTTASK;
      }
      oldAngle=myGetHeading();
      stepTurn(Parameters.Direction.RIGHT);
      if (whoAmI == ALPHA){
      System.out.println("ICI1");
      }
      return;
    }
    if (state==TURNLEFTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE))) {
      stepTurn(Parameters.Direction.LEFT);
      if (whoAmI == ALPHA){
        System.out.println("ICI2");
        }
      return;
    }
    if (state==TURNLEFTTASK && isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE)) {
      state=MOVETASK;
      myMove();
      if (whoAmI == ALPHA){
        System.out.println("ICI3");
        }
      return;
    }
    if (state==TURNRIGHTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE))) {
        stepTurn(Parameters.Direction.RIGHT);
        if (whoAmI == ALPHA){
            System.out.println("ICI4");
            }
        return;
      }
      if (state==TURNRIGHTTASK && isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE)) {
        state=MOVETASK;
        if (whoAmI == ALPHA){
            System.out.println("ICI5");
            }
        myMove();
        return;
      }


    if (state==0xB52){
      if (fireRythm==0) {
        firePosition(700,1500);
        fireRythm++;
        return;
      }
      fireRythm++;
      if (fireRythm==Parameters.bulletFiringLatency) fireRythm=0;
      if (rythm==0) stepTurn(Parameters.Direction.LEFT); else myMove();
      rythm++;
      if (rythm==14) rythm=0;
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
  private void myMove(){
    isMoving=true;
    move();
  }
  private double myGetHeading(){
    return normalizeRadian(getHeading());
  }
  private double normalizeRadian(double angle){
    double result = angle;
    while(result<0) result+=2*Math.PI;
    while(result>=2*Math.PI) result-=2*Math.PI;
    return result;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<ANGLEPRECISION;
  }
  private boolean isRoughlySameDirection(double dir1, double dir2){
    return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<FIREANGLEPRECISION;
  }
  private void process(String message){
    if (Integer.parseInt(message.split(":")[2])==FIRE) {
      fireOrder=true;
      countDown=0;
      targetX=Double.parseDouble(message.split(":")[3]);
      targetY=Double.parseDouble(message.split(":")[4]);
    }
  }
  private void firePosition(double x, double y){
    if (myX<=x) fire(Math.atan((y-myY)/(double)(x-myX)));
    else fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
    return;
  }
  private boolean onTheWay(double angle){
    if (myX<=targetX) return isRoughlySameDirection(angle,Math.atan((targetY-myY)/(double)(targetX-myX)));
    else return isRoughlySameDirection(angle,Math.PI+Math.atan((targetY-myY)/(double)(targetX-myX)));
  }
}

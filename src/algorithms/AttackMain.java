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
  private static final double HEADINGPRECISION = 0.1;
  private static final double ANGLEPRECISION = 0.1;

  private static final int ALPHA = 0x1EADDA;
  private static final int BETA = 0x5EC0;
  private static final int GAMMA = 0x333;

  private static final int TEAM = 42;

  //---VARIABLES---//
  private boolean turnTask,turnRight,moveTask,berzerk,back,ennemyInArea;
  private double endTaskDirection,lastSeenDirection;
  private int endTaskCounter,berzerkInerty, latence;
  private boolean firstMove,berzerkTurning;


    //Variables de tâches
    private static final int TURNTASK = 1;
    private static final int MOVETASK = 2;
    private static final int SINK = 0xBADC0DE1;
    private String ACTION = "move";
  
    //variables d'etats du comportement
    private static final int HUNTER = 10;
    private static final int RUNNER = 15;


    /*--COM ORDER--*/
    private static final int FIRE = 0xB52;
    private static final int FALLBACK = 0xFA11BAC;
    private static final int ROGER = 0x0C0C0C0C;
    private static final int OVER = 0xC00010FF;
    private static final int DIST = 0xD151;
  
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
    private int bulletRange = 1000;
  
    private int countDown;
    private double targetX,targetY;
    private boolean fireOrder;
    private boolean friendlyFire;
  
    private int fireType;

  //---CONSTRUCTORS---//
  public AttackMain() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //HUNTER
    latence=-1;
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

    //RUNNER
    //ODOMETRY CODE
    whoAmI = ALPHA;
    for (IRadarResult o: detectRadar())
    {
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)){
        whoAmI=GAMMA;
      } 
    }

    for(IRadarResult o: detectRadar())
    {
      if (whoAmI!=ALPHA && (isSameDirection(o.getObjectDirection(),Parameters.SOUTH))){
        whoAmI=BETA;
      }
    }

    if (whoAmI == ALPHA){
      myX=Parameters.teamAMainBot1InitX;
      myY=Parameters.teamAMainBot1InitY;
    }
    else if(whoAmI == BETA){
      myX=Parameters.teamAMainBot2InitX;
      myY=Parameters.teamAMainBot2InitY;
    }
    else if(whoAmI == GAMMA){
      myX=Parameters.teamAMainBot3InitX;
      myY=Parameters.teamAMainBot3InitY;
    }    
    else {
      myX=0;
      myY=0;
    }
    
    //INIT
    state = TURNTASK;
    mode=RUNNER;
    isMoving=false;
    oldAngle=getHeading();
    fireType = 1;
  }
  public void step() {
    ArrayList<IRadarResult> radarResults = detectRadar();
    //ODOMETRY CODE
    if (isMoving && ((whoAmI == ALPHA)||(whoAmI == BETA)||(whoAmI == GAMMA)) ){
      myX+=Parameters.teamAMainBotSpeed*Math.cos(getHeading());
      myY+=Parameters.teamAMainBotSpeed*Math.sin(getHeading());
      isMoving=false;
    }  

    //***            Parsing message            ***//
    ArrayList<String> messages=fetchAllMessages();
    for (String m: messages) { 
    //On ne parse que les messages de sa TEAM
      if (Integer.parseInt(m.split(":")[1])==TEAM) process(m);
    }
    
    
    if (radarResults.size()!=0){
      ennemyInArea = false;
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentMainBot
          ||r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
          ennemyInArea = true;
          mode = HUNTER;
        }      
      }
      if(!ennemyInArea && mode == HUNTER){
        mode = RUNNER;
        return;
      }
    }

    if(whoAmI == ALPHA)
    {
      sendLogMessage("#A X:"+(int)myX+" Y:"+(int)myY+" S:"+state+" M:"+mode+"");
    }

    if(whoAmI == BETA)
    {
      sendLogMessage("#B X:"+(int)myX+" Y:"+(int)myY+" S:"+state+" M:"+mode+"");
    }

    if(whoAmI == GAMMA)
    {
      sendLogMessage("#G X:"+(int)myX+" Y:"+(int)myY+" S:"+state+" M:"+mode+"");
    }



    //MODE HUNTER
    if(mode == HUNTER)
    {
      if (berzerk) {
        if (berzerkTurning) {
          endTaskCounter--;
          if (isHeading(endTaskDirection)) {
            berzerkTurning=false;
            move();
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
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot || r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
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

      if (radarResults.size()!=0){
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()!=IRadarResult.Types.OpponentMainBot && r.getObjectType()!=IRadarResult.Types.OpponentSecondaryBot) {
            mode = RUNNER;
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
            return;
          }
          turnTask=false;
          moveTask=true;
          endTaskCounter=100;
          move();
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
      //PLACEMENT DE ALPHA
      if(whoAmI == ALPHA)
      {
        if (state==TURNTASK && myY>=400 && !(isSameDirection(getHeading(),Parameters.NORTH))) {
          stepTurn(Parameters.Direction.LEFT);
          return;
        }     
        
        if (state==TURNTASK && myY>=400 && (isSameDirection(getHeading(),Parameters.NORTH))){
          state = MOVETASK;
          return;
        }

        if(state==MOVETASK && myY>=400 && (isSameDirection(getHeading(),Parameters.NORTH)))
        {
          myMove();
          return;
        }

        if(state==MOVETASK && myY<=400 && !(isSameDirection(getHeading(),Parameters.EAST)))
        {
          state = TURNTASK;
          return;
        }        

        if(state==TURNTASK && myY<=400 && !(isSameDirection(getHeading(),Parameters.EAST))){
          stepTurn(Parameters.Direction.RIGHT);
          return;
        }

        if(state==TURNTASK && myY<=400 && (isSameDirection(getHeading(),Parameters.EAST))){
          state = MOVETASK;
          return;
        }

        if(state==MOVETASK && myX<=1500 && myY<=400 && (isSameDirection(getHeading(),Parameters.EAST))){
          myMove();             
          return;
        }

        if(myX >= 1500 && ACTION == "fire" && myY<=400 && (isSameDirection(getHeading(),Parameters.EAST))){
          firePosition(myX + 1, myY);
          ACTION = "move";
          return;
        }
        if(myX >= 1500 && ACTION == "move" && myY<=400 && (isSameDirection(getHeading(),Parameters.EAST))){
          myMove();
          ACTION = "fire";
          return;
        }

      }

      //PLACEMENT BETA
      if(whoAmI == BETA){
        if(myX <= 1500){
          if(!(isSameDirection(getHeading(),Parameters.EAST))){
            stepTurn(Parameters.Direction.LEFT);
            return;
          }
  
          if((isSameDirection(getHeading(),Parameters.EAST))){
            myMove();             
            return;
          }
        }
        if(myX >= 1500 && ACTION == "fire" && (isSameDirection(getHeading(),Parameters.EAST))){
          firePosition(myX + 1, myY);
          ACTION = "move";
          return;
        }
        if(myX >= 1500 && ACTION == "move" && (isSameDirection(getHeading(),Parameters.EAST))){
          myMove();
          ACTION = "fire";
          return;
        }

      }

      //PLACEMENT DE GAMMA
      if(whoAmI == GAMMA){
        if (state==TURNTASK && myY<=1500 && !(isSameDirection(getHeading(),Parameters.SOUTH))) {
          stepTurn(Parameters.Direction.RIGHT);
          return;
        }     
        
        if (state==TURNTASK && myY<=1500 && (isSameDirection(getHeading(),Parameters.SOUTH))){
          state = MOVETASK;
          return;
        }

        if(state==MOVETASK && myY<=1500 && (isSameDirection(getHeading(),Parameters.SOUTH))){
          myMove();
          return;
        }

        if(state==MOVETASK && myY>=1500 && !(isSameDirection(getHeading(),Parameters.EAST)))
        {
          state = TURNTASK;
          return;
        }        

        if(state==TURNTASK && myY>=1500 && !(isSameDirection(getHeading(),Parameters.EAST))){
          stepTurn(Parameters.Direction.LEFT);
          return;
        }

        if(state==TURNTASK && myY>=1500 && (isSameDirection(getHeading(),Parameters.EAST))){
          state = MOVETASK;
          return;
        }

        if(state==MOVETASK && myX<=1500 && myY>=1500 && (isSameDirection(getHeading(),Parameters.EAST))){
          myMove();             
          return;
        }

        if(myX >= 1500 && ACTION == "fire" && myY>=1500 && (isSameDirection(getHeading(),Parameters.EAST))){
          firePosition(myX + 1, myY);
          ACTION = "move";
          return;
        }
        if(myX >= 1500 && ACTION == "move" && myY>=1500 && (isSameDirection(getHeading(),Parameters.EAST))){
          myMove();
          ACTION = "fire";
          return;
        }

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

  private void firePosition(double x, double y){
    if (myX<=x) fire(Math.atan((y-myY)/(double)(x-myX)));
    else fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
  }

  private double normalize(double dir){
    double res=dir;
    while (res<0) res+=2*Math.PI;
    while (res>=2*Math.PI) res-=2*Math.PI;
    return res;
  } 
  
  //Parsing des messages 2010586:42:53585:521.4244688751213
  private void process(String message){
    System.out.println("azorazk");
    switch(Integer.parseInt(message.split(":")[2]) ) {
    case FIRE:
        fireOrder=true;
        countDown=0;
        targetX=Double.parseDouble(message.split(":")[3]);
        targetY=Double.parseDouble(message.split(":")[4]);
        if(distance(myX, myY, targetX, targetY)< bulletRange && mode!=HUNTER) {
          firePosition(targetX, targetY);
          System.out.println("HELLO JE VEUX TIRER");
        }else if(distance(myX, myY, targetX, targetY)> bulletRange && mode!=HUNTER){
            sendLogMessage("FIRE dist : "+distance(myX, myY, targetX, targetY));
        }
        break;
        // get distance from broadcast msg
    }
  }

  public double distance(double x1,double y1,double x2,double y2 )
  {
      double difX=(x1>x2)? x1-x2: x2-x1;
      double difY=(x1>x2)? x1-x2: x2-x1;
      return Math.sqrt(Math.pow(difY,2) + Math.pow(difX,2));
  }


}

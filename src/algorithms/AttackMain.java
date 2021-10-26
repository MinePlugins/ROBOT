package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class AttackMain extends Brain {
    //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;
  private static final double ANGLEPRECISION = 0.05;

  //---Robots---//
  private static final int ROCKY = 0x1EADDA;
  private static final int MARIO = 0x5EC0;
  private static final int UNDEFINED = 0xBADC0DE0;

  //Variables de tâches
  private static final int TURNSOUTHTASK = 1;
  private static final int MOVESOUTHTASK = 2;
  private static final int TURNEASTTASK = 3;
  private static final int MOVEEASTTASK = 4;
  private static final int TURNNORTHTASK = 5;
  private static final int MOVENORTHTASK = 6;
  private static final int TURNSOUTHBISTASK = 51;
  private static final int SWINGTASK = 7;
  private static final int FREEZE = -1;
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

  //---CONSTRUCTORS---//
  public AttackMain() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
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
    state = TURNNORTHTASK;
    mode=RUNNER;
    isMoving=false;
    oldAngle=getHeading();
    target = "base";
  }

  private void myMove(){
    isMoving=true;
    move();
  }

  public void step() {
    //ODOMETRY CODE
    if (isMoving && whoAmI == ROCKY){
      myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
      myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
      isMoving=false;
     }

    //DEBUG MESSAGE
    if (whoAmI == ROCKY) {
        sendLogMessage("#ROCKY *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+"). State ="+state+". Objet:"+detectFront().getObjectType());
    }

    //---AUTOMATON DEPLACEMENT---//
    if(mode == RUNNER){
        if(target == "base"){
            state = TURNNORTHTASK;
        }
    }

    if (state==TURNNORTHTASK && !(isSameDirection(getHeading(),Parameters.NORTH))) {
        stepTurn(Parameters.Direction.LEFT);
        return;
      }

      if (state==TURNNORTHTASK && (isSameDirection(getHeading(),Parameters.NORTH))) {
        state = MOVENORTHTASK;
        myMove();
        return;
      }

      if (state==MOVENORTHTASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
        myMove();
        sendLogMessage("herre");
        return;
      }
      
      if (state==MOVENORTHTASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
        state=TURNEASTTASK;
        oldAngle=getHeading();
        stepTurn(Parameters.Direction.RIGHT);
        return;
      }

  
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

package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;

import java.util.ArrayList;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

import java.util.ArrayList;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class AttackSecondary extends Brain{

    //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.0000000000000001;
  private static final double ANGLEPRECISION = 0.0000005;
  private static final double FIREANGLEPRECISION = Math.PI/(double)6;
  private static final int TEAM = 42;

  //---Robots---//
  private static final int ROCKY = 0x1EADDA;
  private static final int MARIO = 0x5EC0;
  private static final int UNDEFINED = 0xBADC0DE0;
  
  /*--COM ORDER--*/
  private static final int FIRE = 0xB52;
  private static final int FALLBACK = 0xFA11BAC;
  private static final int ROGER = 0x0C0C0C0C;
  private static final int OVER = 0xC00010FF;
  private static final int DIST = 0xD151;
  
  

  //Variables de tâches
  private static final int TURNSOUTHTASK = 1;
  private static final int TURNEASTTASK = 3;
  private static final int TURNWESTTASK = 4;
  private static final int TURNNORTHTASK = 5;
  private static final int MOVEUNTILWALL = 6;
  private static final int SWINGTASK = 7;
  private static final int LOOP = 8;
  private static final int LOOPTURN = 9;
  private static final int TURNSOUTHBISTASK = 51;
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
  private boolean initRunner;
  private int maxX, maxY;
  private double dist;
  private String orientation;
  
  
  private int countDown;
  private double targetX,targetY;
  private boolean fireOrder;
  private boolean friendlyFire;

  //---CONSTRUCTORS---//
  public AttackSecondary() { super(); }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
      //ODOMETRY CODE
      whoAmI = ROCKY;
      
      for (IRadarResult o: detectRadar()) {
        if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=MARIO;
      }
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
      initRunner=true;
      dist=0.00;
      orientation = "EAST";
      maxX=3000;
      maxY=2000;
    }


    public void step() {
      //ODOMETRY CODE
      if (isMoving && whoAmI == ROCKY){
        myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
        myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
        isMoving=false;
       }
  /***************** CHANGEMENT D'OBJECTIF ******************/
      //Si on est dans la base x(0,300) y(0,300)
      if(myX<300 && myY<300 && myX>0 && myY>0 && target=="base") {
    	  //On définit la target à l'objectif
    	  target = "objectif";
    	  System.out.println("+1 points (base)");
    	  return;
      }
      //Si on est dans l'objectif x(maxX-300,maxX) y(maxY,maxX-300)
      if(myX<maxX && myY<maxY && myX>maxX-300 && myY>maxY-300 && target=="objectif") {
    	//On repart à la base
    	  target = "base";
    	  System.out.println("+1 points (objectif)");
    	  return;
      }
      /***************** \CHANGEMENT D'OBJECTIF ******************/
      ArrayList<String> messages=fetchAllMessages();
      for (String m: messages) { 
      	//On ne parse que ses messages
    	  if (Integer.parseInt(m.split(":")[0])==whoAmI && Integer.parseInt(m.split(":")[1])==TEAM) process(m);
      }
      if (whoAmI == MARIO) myMove(); 

      if (whoAmI == ROCKY) {// || whoAmI ==MARIO) {
      	//COMMUNICATION
          
          //RADAR DETECTION
          freeze=false;
          friendlyFire=true;
          for (IRadarResult o: detectRadar()){
  	      	broadcast(whoAmI+":"+TEAM+":"+DIST+":"+o.getObjectDistance());
  	      	if (o.getObjectDistance()<=100 && !isRoughlySameDirection(o.getObjectDirection(),getHeading()) && o.getObjectType()!=IRadarResult.Types.BULLET) {
  	            freeze=true;
  	          }
          }
        //  System.out.println("Out");
          if (freeze) return;
          //DEBUG MESSAGE
          sendLogMessage("#R X:"+(int)myX+" Y:"+(int)myY+" S:"+state+" O:"+detectFront().getObjectType()+" D:"+dist+" O1:"+orientation );
          
      if(mode == RUNNER){
	      	if(initRunner) {
	      		state = TURNNORTHTASK;
	      		initRunner=false;
	      		System.out.println("On doit aller vers le nord");
	      	}
	  	}
         
          
          if (state==TURNNORTHTASK && !(isSameDirection(getHeading(),Parameters.NORTH))) {
              stepTurn(Parameters.Direction.LEFT);
              return;
            }

            if (state==TURNNORTHTASK && (isSameDirection(getHeading(),Parameters.NORTH))) {
              state = MOVEUNTILWALL;
              orientation="NORTH";
              System.out.println("GO NORTH");
              myMove();
              return;
            }
            
            if (state==TURNSOUTHTASK && !(isSameDirection(getHeading(),Parameters.SOUTH))) {
                stepTurn(Parameters.Direction.RIGHT);
                return;
              }

              if (state==TURNSOUTHTASK && (isSameDirection(getHeading(),Parameters.SOUTH))) {
                state = MOVEUNTILWALL;
                orientation="SOUTH";
                myMove();
                return;
              }
              
            if (state==MOVEUNTILWALL && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL ) {
              myMove();
              return;
            }
            
            
            if (state==MOVEUNTILWALL && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
          	  System.out.println("Mur devant, on essaye de tourner a droite");
          	  stepTurn(Parameters.Direction.RIGHT);
          	  state=TURNEASTTASK;

          	  return;
            }

            if (state==TURNEASTTASK && !(isSameDirection(getHeading(),Parameters.EAST))) {
                stepTurn(Parameters.Direction.RIGHT);
                return;
              }
            
            if(state==TURNEASTTASK && (isSameDirection(getHeading(),Parameters.EAST))) {
          	  if(mode==RUNNER) {
          		  System.out.println("On est tourné vers l'est");
          		  System.out.println("ENTER THE LOOP");
          		  state=LOOP;
          	  }
          	  orientation="EAST";
          	  myMove();
          	  return;
            }
            if (state==TURNWESTTASK && !(isSameDirection(getHeading(),Parameters.WEST))) {
                stepTurn(Parameters.Direction.LEFT);
                return;
              }
            
            if(state==TURNWESTTASK && (isSameDirection(getHeading(),Parameters.WEST))) {
          	  if(mode==RUNNER) {
          		  System.out.println("On est tourné vers l'ouest");
          		  System.out.println("ENTER THE LOOP");
          		  state=LOOP;
          	  }
          	  orientation="WEST";
          	  myMove();
          	  return;
            }
            
            if(state==LOOP && myX>= 140 && orientation =="WEST" ||
			  state==LOOP && myX<= maxX-140 && orientation =="EAST" ||
  			  state==LOOP && myY>= 140 && orientation=="NORTH" ||
  			  state==LOOP && myY<= maxY-140 && orientation=="SOUTH") {
          	  myMove();
          	  oldAngle=getHeading();
          	  return;
            }
            
            //Si j'ai autre chose que "rien" et que c'est pas un ennemi et que je suis en mode hunter
            if(detectFront().getObjectType()!=IFrontSensorResult.Types.NOTHING && detectFront().getObjectType()!=IFrontSensorResult.Types.OpponentMainBot && detectFront().getObjectType()!=IFrontSensorResult.Types.OpponentSecondaryBot && mode==HUNTER) {
          	  mode=RUNNER;
          	  initRunner=true;
          	  return;
            }
            //Si j'ai un ennemi devant moi et que je suis en mode runner
      	  if(detectFront().getObjectType()==IFrontSensorResult.Types.OpponentMainBot || detectFront().getObjectType()==IFrontSensorResult.Types.OpponentSecondaryBot && mode==RUNNER) {
              	mode=HUNTER;
              	System.out.println("PRESENCE NON-ORGANIQUE DETECTE ! PROCESSUS D'EXTERMINATION ENCLENCHE");
              	System.out.println("HOUSTON, J'AI OUBLIER MES ROQUETTES");
              	return;
                }
            
            //Si on a un mur a -150 devant 
      	  if(state==LOOP && myX<= 150 && orientation =="WEST" ||
      			  state==LOOP && myX>= maxX-150 && orientation =="EAST" ||
      			  state==LOOP && myY<= 150 && orientation=="NORTH" ||
      			  state==LOOP && myY>= maxY-150 && orientation=="SOUTH"
      			  ) {
          	  switch(orientation) {
          	  case "NORTH":
          		  orientation="EAST";
          		  break;
          	  case "SOUTH":
          		  orientation="WEST";
          		  break;
          	  case "WEST":
          		  orientation="NORTH";
          		  break;
          	  case "EAST":
          		  orientation="SOUTH";
          		  break;
          	  }
          	  System.out.println(orientation);
          	  state = LOOPTURN;
          	  oldAngle=getHeading();
          	  return;
            }
          	  
            if(state==LOOPTURN && !isSameDirection(getHeading(), oldAngle+(Math.PI*0.5) )) {
          	  stepTurn(Parameters.Direction.RIGHT);
          	  return;
            }
            if(state==LOOPTURN && isSameDirection(getHeading(), oldAngle+(Math.PI*0.5))) {
          	  state=LOOP;
          	  System.out.println("RIGHT TURN 45° COMPLETE");
          	  return;
            }
      }
  }

    private void myMove(){
    
  	  //On avance tout droit le reste on s'en branle pour le moment
    	isMoving=true;
      move();
    }
  //Parsing des messages 2010586:42:53585:521.4244688751213
    private void process(String message){
  	  switch(Integer.parseInt(message.split(":")[2]) ) {
  		  case FIRE:
  			  fireOrder=true;
  		      countDown=0;
  		      targetX=Double.parseDouble(message.split(":")[3]);
  		      targetY=Double.parseDouble(message.split(":")[4]);
  		      break;
  		  // get distance from broadcast msg
  		  case DIST:
  			  dist=Double.parseDouble(message.split(":")[3]);
  			  break;
  	  }
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

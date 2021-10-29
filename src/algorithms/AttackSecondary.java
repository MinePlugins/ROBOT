package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IRadarResult.Types;

import java.util.ArrayList;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

import java.util.ArrayList;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class AttackSecondary extends Brain {

	//---PARAMETERS---//
	private static final double HEADINGPRECISION = 0.0000000001;
	private static final double ANGLEPRECISION = 0.00005;
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
	private static final int DODGE = 10;
	private static final int STUCK = 11;

	private static final int TURNSOUTHBISTASK = 51;
	private static final int FREEZE = -1;
	private static final int SINK = 0xBADC0DE1;

	//variables d'etats du comportement
	private static final int HUNTER = 11;
	private static final int RUNNER = 15;


	//VARIABLE POSITION 
	//---VARIABLES---//
	private int state;
	private int oldState;
	private double oldAngle;
	private double myX,myY;
	private boolean isMoving;
	private boolean isMovingBack;

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

	private int dodging=0;
	private double oldDist=200;
	private double oldDistB=oldDist;
	private static final int borderLimit = 140;


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
			myX=Parameters.teamASecondaryBot2InitX;
			myY=Parameters.teamASecondaryBot2InitY;
		}

		//INIT
		state = MOVEUNTILWALL;
		oldState = MOVEUNTILWALL;
		mode=RUNNER;
		isMoving=false;
		isMovingBack=false;
		oldAngle=getHeading();
		target = "base";
		initRunner=true;
		dist=0.00;
		orientation = "EAST";
		//On défini les bordures du terrain
		maxX=3000;
		maxY=2000;
	}
	public void step() { 
		if(getHealth()<=0) {
			sendLogMessage("DEAD");
		}
		//ODOMETRY CODE UPDATE POSITION NO RETURN
		if (isMoving && whoAmI == ROCKY){
			myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
			myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
			isMoving=false;
			isMovingBack=false;
		}
		if (isMoving && whoAmI == MARIO){
			myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
			myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
			isMoving=false;
			isMovingBack=false;
		}
		if (isMovingBack && whoAmI == ROCKY){
			myX-=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
			myY-=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
			isMoving=false;
			isMovingBack=false;
		}
		if (isMovingBack && whoAmI == MARIO){
			myX-=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
			myY-=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
			isMoving=false;
			isMovingBack=false;
		}
		/***************** CHANGEMENT D'OBJECTIF  NO RETURN ******************/
		//Si on est dans la base x(0,300) y(0,300)
		if(myX<300 && myY<300 && myX>0 && myY>0 && target=="base") {
			//On définit la target à l'objectif
			target = "objectif";
			//System.out.println(whoAmI+" +1 point (base)");
			return;
		}
		//Si on est dans l'objectif x(maxX-300,maxX) y(maxY,maxX-300)
		if( myX<maxX && myY<maxY && myX>maxX-300 && myY>maxY-300 && target=="objectif") {
			//On repart à la base
			target = "base";
			//System.out.println(whoAmI+" +1 point (objectif)");
			return;
		}
		/***************** \CHANGEMENT D'OBJECTIF ******************/


		ArrayList<String> messages=fetchAllMessages();
		for (String m: messages) { 
			//On ne parse que ses messages
			if (Integer.parseInt(m.split(":")[1])==TEAM) process(m);
		}


		//DEBUG MESSAGE
		if (whoAmI == MARIO) { 
			//  myMove(); 
			sendLogMessage("#M X:"+(int)myX+" Y:"+(int)myY+" S:"+state+" O:"+detectFront().getObjectType()+" D:"+dist+" O1:"+orientation );
		}else if(whoAmI==ROCKY) {
			sendLogMessage("#R|X:"+(int)myX+"|Y:"+(int)myY+"|S:"+state+"|O:"+detectFront().getObjectType()+"|D:"+dist+"|O1:"+orientation );
		}
		/*
		if(whoAmI ==MARIO) {
			if(myX>=maxX && state!=STUCK) {
				oldState=LOOP;
				state=STUCK;
				oldAngle=myGetHeading();
				System.out.println(oldAngle);
				return;
			}
			//Si on est STUCK
			if(state==STUCK) {
				//Demi tour
				if(!isSameDirection(myGetHeading(),oldAngle + Math.PI )){
					sendLogMessage(myGetHeading()+"  "+oldAngle + Math.PI);
					stepTurn(Parameters.Direction.RIGHT);
					return;
				}
				else {
					state=oldState;
					oldState=STUCK;
				}
			}
			myMove();
			return;
		}*/
		//Début noReturn, on ne doit jamais empecher le radar d'effectuer son scan
		//COMPORTEMENT SIMILAIRE
		if (whoAmI == ROCKY || whoAmI ==MARIO)
		{
			//RADAR DETECTION
			freeze=false;
			for (IRadarResult o: detectRadar()) {
				//Send ennemy position to broadcast
				if (o.getObjectType()==IRadarResult.Types.OpponentMainBot || o.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
					double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
					double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
					broadcast(whoAmI+":"+TEAM+":"+FIRE+":"+enemyX+":"+enemyY+":"+OVER);
					//System.out.println("TIRE "+enemyX+" : "+enemyY);
				}
				//Send friend position to broadcast
				if (o.getObjectType()==IRadarResult.Types.TeamMainBot || o.getObjectType()==IRadarResult.Types.TeamSecondaryBot) {
					double friendX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
					double friendY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
					broadcast(whoAmI+":"+TEAM+":"+ROGER+":"+friendX+":"+friendY+":"+OVER);
					//System.out.println("ROGER "+friendX+" : "+friendY);
				} 



				//Si cest des balles et que c'est dans la même direction et a -200 
				if(o.getObjectDistance()<=200 && isRoughlySameDirection(o.getObjectDirection(),getHeading()) && o.getObjectType()==IRadarResult.Types.BULLET ){
					//On recule
					myMoveBack();
					return;
				}
				// Si cest (une epave ou un ennemiMain ou un ennemiSecondary)
				// et que c'est dans la même direction et a -200
				if( (o.getObjectType()==Types.Wreck || o.getObjectType()==Types.OpponentMainBot || o.getObjectType()==Types.OpponentSecondaryBot) 
						&& isRoughlySameDirection( o.getObjectDirection(),getHeading()) && o.getObjectDistance()<=200) {
					//Sauvegarde du state actuel
					oldState=state;
					//Sauvegarde de l'angle actuel
					oldAngle=getHeading();
					//détermination distance a parcourir -> pythagore
					oldDist=(int)(o.getObjectDistance()*o.getObjectDistance());
					oldDistB=oldDist;
					//init dodging
					dodging=0;
					state=DODGE;
					return;
				}
				//Si cest pas des balles ou ennemi 
				//et que c'est dans la même direction et a -120
				if (o.getObjectDistance()<=120 && isRoughlySameDirection(o.getObjectDirection(),getHeading()) && o.getObjectType()!=IRadarResult.Types.BULLET && o.getObjectType()!=IRadarResult.Types.OpponentMainBot && o.getObjectType()!=IRadarResult.Types.OpponentSecondaryBot) {
					//on s'arrete
					freeze=true;
					sendLogMessage("FREEZE "+o.getObjectType());
				}
			}
			if (freeze) return;

			//Fin de "noreturn"

			//Correction trajectoire

			if((myX>=maxX || myX<=0) || (myY>=maxY || myY<=0)) {
				oldState=LOOP;
				state=STUCK;
				oldAngle=myGetHeading();
				return;
			}
			//Si on est STUCK
			if(state==STUCK) {
				//Demi tour
				if(!isSameDirection(myGetHeading(),oldAngle + Math.PI )){
					sendLogMessage(myGetHeading()+"  "+oldAngle + Math.PI);
					stepTurn(Parameters.Direction.RIGHT);
					return;
				}
				else {
					state=oldState;
					oldState=STUCK;
					return;
				}
			}
			if(detectFront().getObjectType()==IFrontSensorResult.Types.Wreck || 
					detectFront().getObjectType()==IFrontSensorResult.Types.OpponentMainBot || 
					detectFront().getObjectType()==IFrontSensorResult.Types.OpponentSecondaryBot && mode==RUNNER) {
				sendLogMessage("PRESENCE NON-ORGANIQUE ENNEMI DETECTE ! PROCESSUS D'ELOIGNEMENT ENCLENCHE");
				myMoveBack();
				return;
			}
			if(mode == RUNNER){
				if(initRunner) {
					state = TURNNORTHTASK;
					initRunner=false;
					//System.out.println("INIT RUNNER");
				}
			}
			if(state==DODGE) {
				if(dodging==0 && !(isSameDirection(getHeading(),oldAngle+(Math.PI*0.25)))) {
					stepTurn(Parameters.Direction.RIGHT);
					return;
				}
				if(dodging==0 && (isSameDirection(getHeading(),(double)(oldAngle+Math.PI*0.25))) ) {
					//System.out.println("On est orienté pi/4 vers droite");
					dodging++;
					//System.out.println(oldDist);
					return;
				}
				if(dodging==1 && oldDist>0) {
					myMove();
					oldDist--;
					//System.out.println("On avance");
					return;
				} if(dodging==1 && oldDist<=0) {
					//System.out.println("On a fini d'avancer");
					dodging++;
					return;
				}
				if (dodging==2 && !(isSameDirection(getHeading(),oldAngle-(Math.PI*0.25)))) {
					//System.out.println("On tourne PI/2 vers la gauche");
					stepTurn(Parameters.Direction.LEFT);
					return;
				} if(dodging==2 && (isSameDirection(getHeading(),oldAngle-(Math.PI*0.25)))) {
					//System.out.println("On a fini de tourner");
					dodging++;
					return;
				}
				if(dodging==3 && oldDistB>0) {
					myMove();
					//System.out.println("On avance de dist");
					oldDistB--;
					return;
				} if(dodging==3 && oldDistB<=0) {
					dodging++;
					//System.out.println("On avance de dist");
					return;
				}
				if(dodging==4 && !(isSameDirection(getHeading(),oldAngle))) {

					//System.out.println("On retourne sur l'angle initial");
					stepTurn(Parameters.Direction.RIGHT);
					return;
				}  if(dodging==4 && (isSameDirection(getHeading(),oldAngle))) {
					state =oldState;
					//System.out.println("On retourne a l'état initial");
					return;
				}

			}
			if (state==TURNNORTHTASK && !(isSameDirection(getHeading(),Parameters.NORTH))) {
				stepTurn(Parameters.Direction.LEFT);
				return;
			}
			if (state==TURNNORTHTASK && (isSameDirection(getHeading(),Parameters.NORTH))) {
				state = MOVEUNTILWALL;
				orientation="NORTH";
				//System.out.println("GO NORTH");
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
				//System.out.println("Mur devant, on essaye de tourner a droite");
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
					//System.out.println("On est tourné vers l'est");
					//System.out.println("ENTER THE LOOP");
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
					//System.out.println("On est tourné vers l'ouest");
					//System.out.println("ENTER THE LOOP");
					state=LOOP;
				}
				orientation="WEST";
				myMove();
				return;
			}


			//On avance vers le mur "orientation" jusqu'a "borderLimit"

			if(state==LOOP && myX>= borderLimit && orientation =="WEST" ||
					state==LOOP && myX<= maxX-borderLimit && orientation =="EAST" ||
					state==LOOP && myY>= borderLimit && orientation=="NORTH" ||
					state==LOOP && myY<= maxY-borderLimit && orientation=="SOUTH") {
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


			//Si on a un mur a -150 devant 
			if(state==LOOP && myX<= borderLimit && orientation =="WEST" ||
					state==LOOP && myX>= maxX-borderLimit && orientation =="EAST" ||
					state==LOOP && myY<= borderLimit && orientation=="NORTH" ||
					state==LOOP && myY>= maxY-borderLimit && orientation=="SOUTH") {
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
				//System.out.println(orientation);
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
				//System.out.println("RIGHT TURN 45° COMPLETE");
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
	public double distance(double x1,double y1,double x2,double y2 )
	{
		double difX=(x1>x2)? x1-x2: x2-x1;
		double difY=(x1>x2)? x1-x2: x2-x1;
		return Math.sqrt(Math.pow(difY,2) + Math.pow(difX,2));
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

	private void myMoveBack(){
		isMovingBack=true;
		moveBack();
	}
	private boolean onTheWay(double angle){
		if (myX<=targetX) return isRoughlySameDirection(angle,Math.atan((targetY-myY)/(double)(targetX-myX)));
		else return isRoughlySameDirection(angle,Math.PI+Math.atan((targetY-myY)/(double)(targetX-myX)));
	}
}

/*TO DO: 
 * - Record who winner was
 * - On new game make the loser the person who gets to go first
*/

import java.io.*;
import java.net.*;
import java.util.regex.*;


public class TictactoThreadedServer implements Runnable {
	private final char NLINE = '\n';
	private final String PROC = "PROCESSED";
	private final String SEPERATE = ":";
	private final int NEWGAMECALL = 1;
	private final int RESETCALL = 2;
//the number of receive messages
	private final int RECTYPES = 7;
	
	private Socket connect;
	private TTTModelBoard sharedBoard = new TTTModelBoard();
	private BufferedReader inClient;
	private DataOutputStream outClient;
	
//This is an array that keeps track of the message that need to be sent to the player
//the corresponding message gets set to "PROCESSED" when the server sends the message once 
//since TCP guarantees delivery of the message and you don't need to keep sending the message
//0 - Victory 1 - the opponents move
//2 - Reset called by the other user 3 -newgame called by other user
//4 - opponents quit
//5 - Ready to call a new game
//PROCESSED means that its been recieved and not to be sent anymore
//gets set back to null
	
	private String[] messages = new String[RECTYPES];
//helper booleans
	private boolean tellQuit;
	private boolean yourTurn;
	private boolean newGameAsk;
	private boolean resetAsk;

	private boolean newgame; 
	private boolean vict;
	
	private int myPlayNum;
	private int otherplayNum;
	
	private int victor;
	
	
//	private String[] messages;
	private ServerCommunication communication;
	
	public TictactoThreadedServer(Socket connect, TTTModelBoard sharedBoard, int playNum) {
		this.connect = connect;
		this.sharedBoard = sharedBoard;
		this.communication = new ServerCommunication();
		this.myPlayNum = playNum;
		this.otherplayNum = otherPlayNum(this.myPlayNum);
		for(int i = 0; i < RECTYPES; i++) {
			messages[i] = null;
		}
		this.newgame = false;
		this.victor = 2;
	}
	
	public void run() {
		boolean properConnect = true;
		boolean initName = false;
		
		try {
			inClient = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			outClient = new DataOutputStream(connect.getOutputStream());
		} catch(IOException e) {
			properConnect = false;
		}

		if(properConnect) {
			while(this.connect.isConnected() && (!this.tellQuit)) {
				if(!initName){
					initNameStats();
					initName = true;
					try {
						this.connect.setSoTimeout(1000);
					} catch (SocketException e) {
						System.out.println("Cannot set timeout");
					}
				}
				
				
//first part of the loop is finding out state of board and sending messages
//to the player
				if(this.sharedBoard.getFullVic() > 0) {
					this.vict = true;
				} else {
					this.vict = false;
				}
				this.yourTurn = myTurn();
				serverSendMessages();
				serverReceiveMessages();
				
			}
		} else {
			System.out.println("Could not establish IO with this connectio");
		}
		System.out.println("WE ARE OUT!");
		try {
			inClient.close();
			outClient.close();
			this.connect.close();
		} catch(IOException clo) {
			System.out.println("Could not close connection properly");
		}
	}

//basically a helper function that determines if a message should be sent or not
//very small but stops me from repeating a really long command every time
//i want to send a message
	public boolean shouldSendMessage(int i) {
		boolean result = false;
		if((messages[i]!=null) && (!messages[i].equals(PROC))) {
			result = true;
		}
		return result;
	}
	
	public boolean haveIAnsweredNew() {
		return this.sharedBoard.doesOtherAnswerNR(otherplayNum, NEWGAMECALL);
	}
	public boolean haveIAnsweredReset(){
		return this.sharedBoard.doesOtherAnswerNR(otherplayNum, RESETCALL);
	}
	
	public int otherPlayNum(int i) {
		int result;
		if(i == 0) {
			result = 1; 
		} else {
			result = 0;
		}
		return result;
	}
	public Socket getSocket() { 
		return this.connect;
	}
	public boolean myTurn() {
		return sharedBoard.yourTurn(this.myPlayNum);
	}

	public String isName(String fromClient) {
		String result = null;
		String pattern = "PLAYERNAME:(\\w+)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(fromClient);
		if(m.find()) {
			result = m.group(1);
		}
		return result;
	}

//Deal with setting up the board with information and names
//Both connections have to give their names before the game can start
//this is function does the first administrative duties of getting
//the players names and setting up the board
	public void initNameStats() {
		String otherName = null;
		String thisName = null;
		String input;
		String output;
		
		Boolean spec = false;

		try {
			while(thisName == null) {
				input = inClient.readLine();
				thisName = isName(input); 
			}

			sharedBoard.giveTheName(thisName, this.myPlayNum);
			output = communication.sWaitName+NLINE;
			outClient.writeBytes(output);

		/*NOTE TO PROGRAMMER: PLEASE ASK WHY THE FOLLOWING CODE WON'T WORK:
		
 			while(otherName == null) {
				otherName = sharedBoard.otherNameGiven(this.myPlayNum);
			}
		 */
			while(spec == false) {
				otherName = sharedBoard.otherNameGiven(this.myPlayNum);
				if(otherName != null) {
					spec = true;
				}
			}
			
			if(thisName.equals(otherName)) {
				if(!this.sharedBoard.getSameNameAlter()) {
					this.sharedBoard.alterNames();
				}
			}
			
			thisName = communication.sPDetail+sharedBoard.getPlayerInfo(myPlayNum)+NLINE;
			otherName = communication.sPDetail+sharedBoard.getPlayerInfo(otherplayNum)+NLINE;
			outClient.writeBytes(thisName);
			outClient.writeBytes(otherName);

			input = inClient.readLine();
			while(!input.equals(communication.scOK)) {
				input = inClient.readLine();
			}

			if(sharedBoard.yourTurn(this.myPlayNum)) {
				input = communication.sPlay+NLINE;
				this.yourTurn = true;
			} else {
				input = communication.sWaitMove+NLINE;
				this.yourTurn = false;
			}
			outClient.writeBytes(input);

		} catch(IOException initial) {
			System.out.println("IO not working properly");
		}
	}
//------------------------------------------
	public void serverSendMessages() {
		
		String output;
		Boolean nrDone = false;
		try {
			
			if((!this.yourTurn) && (sharedBoard.hasMoved())) {
				//if it isn't your turn and the other player has recorded a move
				//get the move and send it to the player and then switch turns
				
				if(this.messages[1] == null) {
					this.messages[1] = communication.sMove+sharedBoard.getMove()+NLINE;
				}
				if(shouldSendMessage(1)) {
					outClient.writeBytes(this.messages[1]);
					
					sharedBoard.switchPlayers();
					this.yourTurn = true;
					this.messages[1] = PROC;
					if(!vict) {
						outClient.writeBytes(communication.sPlay+NLINE);
					}
				}
			}
					
			
			if(this.vict) {
				if(this.messages[0] == null) {
					this.messages[0] = communication.sEnd+this.sharedBoard.getFullVic()+
							SEPERATE+this.sharedBoard.endGame()+NLINE;
				}
				if(shouldSendMessage(0)) {
					System.out.println("This player is being sent victory: "+sharedBoard.getPlayerInfo(myPlayNum));
					outClient.writeBytes(this.messages[0]);
					
					output = communication.sPDetail+sharedBoard.getPlayerInfo(myPlayNum)+NLINE;
					outClient.writeBytes(output);
					output = communication.sPDetail+sharedBoard.getPlayerInfo(otherplayNum)+NLINE;
					outClient.writeBytes(output);
					
					this.victor = sharedBoard.getVictor();
					this.messages[0] = PROC;
				}
			}
			
			if(this.newgame) {
				
				System.out.println("THIS PLAYER HAS GOT TO NEW GAME: "+this.myPlayNum);
				
				output = communication.sPDetail+sharedBoard.getPlayerInfo(myPlayNum)+NLINE;
				outClient.writeBytes(output);
				output = communication.sPDetail+sharedBoard.getPlayerInfo(otherplayNum)+NLINE;
				outClient.writeBytes(output);
				
				if(this.victor < 2) {
					if(this.myPlayNum == this.victor) {
						System.out.println("PLAYER "+this.myPlayNum+" won and is waiting");
						this.messages[5] = communication.sWaitBegin+NLINE;
						this.sharedBoard.incReadyForNR();
					} else {
						System.out.println("PLAYER "+this.myPlayNum+" lost and will go next");
						this.messages[5] = communication.sPlay+NLINE;
						this.sharedBoard.incReadyForNR();
					}
				} else {
					if(myTurn()) {
						System.out.println("ENTERED RESET TERRITORY");
						this.messages[5] = communication.sPlay+NLINE;
						this.sharedBoard.incReadyForNR();
					} else {
						System.out.println("ENTERED RESET TERRIORY");
						this.messages[5] = communication.sWaitBegin+NLINE;
						this.sharedBoard.incReadyForNR();
					}
				}
				this.sharedBoard.resetBoard();
				this.newgame = false;
				this.messages[3] = null;
				this.messages[2] = null;
				this.messages[0] = null;
			}
			
			if(this.sharedBoard.getReadyForNR() == 2) {
				if((this.myPlayNum != this.victor) || (myTurn())) {
					System.out.println("PLAYER "+this.myPlayNum+" is going FIRST");
					outClient.writeBytes(this.messages[5]);
					this.messages[5] = null;
					this.victor = 2;
					this.sharedBoard.incReadyForNR();
					nrDone = true;
				}
			}
			if(this.sharedBoard.getReadyForNR() == 3) {
				if((this.myPlayNum == this.victor) || (!myTurn())) {
					if(!nrDone) {
						System.out.println("PLAYER "+this.myPlayNum+" is going SECOND");
						outClient.writeBytes(this.messages[5]);
						this.sharedBoard.resetReadyForNR();
						this.victor = 2;
						this.messages[5] = null;
					}
				}
			}
			
		    
			if(sharedBoard.doesOtherReset(myPlayNum)) {
				//check to see if the other player wants to initiate
				//a reset of the game
				if(this.messages[2] == null) {
					this.messages[2] = communication.sReset+NLINE;
				}
				if(shouldSendMessage(2) && (!haveIAnsweredReset())) {
					outClient.writeBytes(this.messages[2]);
					this.messages[2] = PROC;
				}
			} else {
				//reset the new boolean here because the other player
				//will reset their boolean if this player says no to
				//the new game
				this.messages[2] = null;
			}
			
			if(sharedBoard.doesOtherNew(myPlayNum)) {
				//checks to see if the other player wants to initiate
				//a new game
				if(this.messages[3] == null) {
					this.messages[3] = communication.sNew+NLINE;
				}
				if(shouldSendMessage(3) && (!haveIAnsweredNew())) {
					outClient.writeBytes(this.messages[3]);
					this.messages[3] = PROC;
					System.out.println("THE OTHER PERSON WANTS A NEW GAME");
				}
			} else {
				//reset the new boolean here because the other player
				//will reset their boolean if this player says no to
				//the new game
				this.messages[3] = null;
			}
			
			if(sharedBoard.doesOtherQuit(this.myPlayNum)){
				if(this.messages[4] == null) {
					this.messages[4] = communication.sTheyQuit+NLINE;
				}
				if(shouldSendMessage(4)) {
					outClient.writeBytes(this.messages[4]);
					this.messages[4] = PROC;
				}
			}
			
			if(this.newGameAsk) {
				//checks to see if the other player has responded to your
				//request to have a new game
				if(sharedBoard.doesOtherAnswerNR(myPlayNum, NEWGAMECALL)) {
					
					output = communication.sNew;
					if(sharedBoard.otherAnswerNew(myPlayNum)) {
						//by sending ok your saying its ok to have a new game
						output += communication.scOK+NLINE;
						resetNRBothPlayers();

						//reset the other players newgame as well just in case
						//both players have decided to call it at same time.
						
						//this.sharedBoard.resetBoard();
						
						//this.sharedBoard.switchPlayers();
						
						this.newgame = true;
						this.messages[0] = null;
					} else {
						output += communication.scNOT+NLINE;
					}
					outClient.writeBytes(output);
					this.newGameAsk = false;
					this.sharedBoard.resetMyNew(myPlayNum);
				}
			}
			
			if(this.resetAsk) {
				//checks to see if the other player has responded to your
				//request for a reset
				if(sharedBoard.doesOtherAnswerNR(myPlayNum, RESETCALL)) {
	
					output = communication.sReset;
					if(sharedBoard.otherAnswerReset(myPlayNum)) {
						output += communication.scOK+NLINE;
						resetNRBothPlayers();
						//reset the other players reset as well just in case
						//both players have decided to call it at same time.
						//this.sharedBoard.switchPlayers();
						this.newgame = true;
						this.messages[0] = null;
					} else {
						output += communication.scNOT+NLINE;
					}	
					
					outClient.writeBytes(output);
					this.resetAsk = false;
					this.sharedBoard.resetMyReset(myPlayNum);
					//need to reset the boolean in the reset thing TO DO
				}
			}
		
		} catch(IOException inp) {
			System.out.println("Cant send");
		}
	}
	//-----------------------------------------------------
	public void serverReceiveMessages() {
		String input;
		String inPart1;
		String inPart2;
		try {
			input = inClient.readLine();
			
			if(input != null) {
				String pattern = "^([A-Z]+:)([a-zA-Z0-9:\\s]*)$";
				Pattern p = Pattern.compile(pattern);
				Matcher m = p.matcher(input);
				
				if(m.find()) {
					inPart1 = m.group(1);
					inPart2 = m.group(2);
					if(inPart1.equals(communication.cMove)) {
						handleMove(inPart2);
					} else if(inPart1.equals(communication.cQuit)) {
						handleQuit();
					} else if(inPart1.equals(communication.cReset)) {
						handleReset(inPart2);
					} else if(inPart1.equals(communication.cNew)) {
						handleNewgame(inPart2);
					}
				}
			}
			
		} catch (InterruptedIOException tiemout) {
			
		} catch(IOException outp) {
			System.out.println("Can not receive");
		}
		
	}
//handle move. This function gets called when the person
//wants to make a move. 2nd part of the string
//consists of a number 1-9 which needs to be given to the board
//and the other player.
	public void handleMove(String part) {
		
		//PRINT STATEMENT
		System.out.println("handling move from player: "+this.myPlayNum);
		//PRINT STATEMENT
		
		int move = Integer.parseInt(part);
		this.sharedBoard.doMove(this.myPlayNum, move);
		this.messages[1] = null;
		//this.messages[2] = null;
	}
	
	public void handleQuit() {
		this.sharedBoard.imQuitting(this.myPlayNum);
		this.tellQuit = true;
	}
//In handling the reset. The server has to deal with two types.
//the first type is if the player wants to reset. The second type
//is if the player is responding to a reset call
	public void handleReset(String part) {
		if(part.equals("")) {
			this.resetAsk = true;
			this.sharedBoard.iWantReset(myPlayNum);
		}
		
		if(part.equals(communication.scOK)) {
			this.sharedBoard.answerReset(myPlayNum, true);
			this.newgame = true;
			this.sharedBoard.resetVictories();
		} else if(part.equals(communication.scNOT)) {
			this.sharedBoard.answerReset(myPlayNum, false);
		}
	}
	
	public void handleNewgame(String part) {
		if(part.equals("")) {
			this.newGameAsk = true;
			this.sharedBoard.iWantNew(myPlayNum);
		}
		
		if(part.equals(communication.scOK)) {
			this.sharedBoard.answerNew(myPlayNum, true);
			this.newgame = true;
			
			if(this.myPlayNum == this.victor) {
				this.sharedBoard.setCurrentPlayer(this.otherplayNum);
			} else {
				this.sharedBoard.setCurrentPlayer(this.myPlayNum);
			}
			
		} else if(part.equals(communication.scNOT)) {
			this.sharedBoard.answerNew(myPlayNum, false);
		}
	}
	
	public void resetNRBothPlayers() {
		this.sharedBoard.resetMyNew(myPlayNum);
		this.sharedBoard.resetMyNew(otherplayNum);
		this.sharedBoard.resetMyReset(myPlayNum);
		this.sharedBoard.resetMyReset(otherplayNum);
	}

	
}
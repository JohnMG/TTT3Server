

//Using MVC this is the model. It stores the data and does some of the
//calculations for the controller.

public class TTTModelBoard {

//global variables for the modal
	private final int ROWCOL = 3;
//the number of reset/newgame booleans needed in their respective arrays
	private final int CALLNR = 3;
	private final int PLAYERONE = 0;
	private final int PLAYERTWO = 1;
	private final int DIAGS = 2;
	//private final int PLAYNUM = 2;
	private final int NEWGAMECALL = 1;
	//private final int RESETCALL = 2;
	private final char BLANKS = '-';

 //store the tictacto board as a 3x3 array
	private char[][] titato = new char[ROWCOL][ROWCOL];
	private TTTModelPlayer[] players = new TTTModelPlayer[2];
	private TTTModelPlayer one;
	private TTTModelPlayer two;
	private int currentPlayer;
//currentMoved tells the other thread if they've done a move
	private boolean currentMoved;
//curentMove  tells the other thread what move they've done
	private int currentMove;
	
	private boolean pOneNameGot;
	private boolean pTwoNameGot;
	
	private boolean pOneQuit;
	private boolean pTwoQuit;
	
	private int fullVicAchieved;
	private int victor;
	private int readyForNR;
//three flags
//1st flag = Does this player want to reset
//2nd flag = Has the other player answered the reset
//3rd flag = what is the other player's answer
	private boolean[] pOneReset = {false, false, false};
	private boolean[] pTwoReset = {false, false, false};
	private boolean[] pOneNew = {false, false, false};
	private boolean[] pTwoNew = {false, false, false};
//constructor requires the names of both the players
	public TTTModelBoard() {

		for(int i = 0; i < ROWCOL; i++) {
			for(int j = 0; j < ROWCOL; j++) {
				titato[i][j] = BLANKS;
			}
		}
		one = new TTTModelPlayer();
		two = new TTTModelPlayer();
		players[0] = one;
		players[1] = two;
		this.currentPlayer = 0;
		this.pOneNameGot = false;
		this.pTwoNameGot = false;
		this.currentPlayer = PLAYERONE;
		this.currentMoved = false;
		this.currentMove = 0;
		this.pOneQuit = false;
		this.pTwoQuit = false;
		this.fullVicAchieved = 0;
		this.victor = 2;
	}

//victory condition function. attempt to deduce if the victory occurred
//and how the victory occurred
	public int victoryCond() {
		int result = 0;
		boolean fullCond = false;
//0 = nothing, 1-3 = Rows, 4-6 = Columns, 7&8=Diags, 9 = full
		if(result == 0) {
			result = checkAcross();
		}
		if(result == 0) {
			result = checkDown();
		}
		if(result == 0) {
			result = checkDiag();
		}
		if(result == 0) {
			fullCond = checkFull();
			if(fullCond) {
				result = 9;
			}
		}
		return result;
	}
	
	

//this function checks if the move a player wants to do has already
//been done. If the square matches the blank it returns true which
//means the move is valid
	
	public boolean checkTaken(int x, int y) {
		boolean result = false;
		if(titato[x][y] == BLANKS) {
			result = true;
		}
		return result;
	}
	
	public int checkAcross() {
		int found = 0;
		boolean same;
//this function checks all the rows
//it stores the moves performed in a rows
//in an array and checks if they were performed by the same player
		char[] checker = new char[3];
		int x = 0;
		int y = 0;
		
		while((found==0) && (y<3)) {

			for(x=0; x<ROWCOL; x++){
				checker[x] = titato[x][y];
			}

			same = checkSame(checker);
			if(same == true) {
				found = (y+1);
			}
			y++;
		}
		return found;
	}
	
	public int checkDown() {
		int found = 0;
		boolean same;
//this function checks all the columns
//it stores the moves performed in a column
//in an array and checks if they were performed by the same player
		char[] checker = new char[3];
		int x = 0;
		int y = 0;
		
		while((found==0) && (x<3)) {
//
			for(y=0; y<ROWCOL; y++){
				checker[y] = titato[x][y];
			}
//
			same = checkSame(checker);
			if(same == true) {
				found = (x+4);
			}
			x++;
		}
		return found;
	}
//same function as checkAcross and checkDown
//but for the diagonals
	public int checkDiag() {
		int found = 0;
		boolean same;
		char[][] checker = new char[2][3];
		int x = 0;
		int y = 0;
		
		for(x=0; x<ROWCOL; x++, y++){
			checker[0][x] = titato[x][y];
		}
		for(x=2, y=0; y<ROWCOL; x--,y++) {
			checker[1][y] = titato[x][y];
		}
		for(x=0; ((x<DIAGS) && (found==0)); x++) {
			same = checkSame(checker[x]);
			if(same){
				found = (x+7);
			}
		}
		return found;
	}
//helper function that checks if the moves performed
//were done by the same person
	public boolean checkSame(char[] ArrOfMove) {
		int x;
		boolean same = true;
		for(x=0; (x<(ROWCOL-1)&&(same)); x++){
			if(ArrOfMove[x] == ArrOfMove[x+1]) {
				if(ArrOfMove[x]!=BLANKS){
					same = true;
				} else {
					same = false;
				}
			} else {
				same = false;
			}
		}
		return same;
	}

//checks if the board is full of moves
	public boolean checkFull(){
		boolean result = true;
		for(int x=0; x<ROWCOL; x++) {
			for(int y=0; y<ROWCOL; y++) {
				if(titato[x][y] == BLANKS){
					result = false;
				}
			}
		}
		return result;
	}

//records a move with the board if its valid
//This function has changed since 2.0
//it no longer checks the move since validity checks
//is given to the player.
	public synchronized void doMove(int p, int move){

		int x = 0;
		int y = 0;
		this.currentMove = move;
		
		move++;

		x = detXCoor(move);
		y = detYCoor(move);

		titato[x][y] = players[p].getPiece();
		this.fullVicAchieved = victoryCond();
		//if one of the players has achieved victory then add to their
		//victory count
		if((this.fullVicAchieved >=1) && (this.fullVicAchieved <=8)) {
			players[this.currentPlayer].addVictory();
		}
		this.doneMove();
	}
//helper method to determine the x-coordinate
	public int detXCoor(int move) {
		int result = 0;
		if((move%ROWCOL)>0) {
			result = ((move%ROWCOL)-1);
		} else {
			result = ((ROWCOL)-1);
		}
		return result;
	}
//helper methods to determine the y-coordinate
	public int detYCoor(int move) {
		int result = 0;
		if((move%ROWCOL)>0) {
			result = (move/ROWCOL);
		} else {
			result = ((move/ROWCOL)-1);
		}
		return result;
	}
//resets the board back to its original state
	public void resetBoard(){
		for(int x=0; x<ROWCOL; x++) {
			for(int y=0; y<ROWCOL; y++) {
				titato[x][y] = BLANKS;
			}
		}
		this.fullVicAchieved = 0;
	}
//resets each players victory count
	public void resetVictories(){
		players[PLAYERONE].resetVictory();
		players[PLAYERTWO].resetVictory();
	}

//prepares the messages that display who won the game
//and how they won it
	public synchronized String endGame() {

		String msg = "";
		//String pNamae;
		//msg = (players[this.currentPlayer].getName()); 
		
		switch(this.fullVicAchieved) {
			case 1:
				msg = players[detPlayer(titato[0][0])].getName();
				msg += " won on the 1st row";
				msg += ":"+players[detPlayer(titato[0][0])].getPiece();
				this.victor = detPlayer(titato[0][0]);
				break;
			case 2:
				msg = players[detPlayer(titato[0][1])].getName();
				msg +=" won on the 2nd row";
				msg += ":"+players[detPlayer(titato[0][1])].getPiece();
				this.victor = detPlayer(titato[0][1]);
				break;
			case 3:
				msg = players[detPlayer(titato[0][2])].getName();
				msg += " won on the 3rd row";
				msg += ":"+players[detPlayer(titato[0][2])].getPiece();
				this.victor = detPlayer(titato[0][2]);
				break;
			case 4:
				msg = players[detPlayer(titato[0][0])].getName();
				msg += " won on the 1st column";
				msg += ":"+players[detPlayer(titato[0][0])].getPiece();
				this.victor = detPlayer(titato[0][0]);
				break;
			case 5:
				msg = players[detPlayer(titato[1][0])].getName();
				msg += " won on the 2nd column";
				msg += ":"+players[detPlayer(titato[1][0])].getPiece();
				this.victor = detPlayer(titato[1][0]);
				break;
			case 6:
				msg = players[detPlayer(titato[2][0])].getName();
				msg += " won on the 3rd column";
				msg += ":"+players[detPlayer(titato[2][0])].getPiece();
				this.victor = detPlayer(titato[2][0]);
				break;
			case 7:
				msg = players[detPlayer(titato[0][0])].getName();
				msg += " won on the upper left diagonal";
				msg += ":"+players[detPlayer(titato[0][0])].getPiece();
				this.victor = detPlayer(titato[0][0]);
				break;
			case 8:
				msg = players[detPlayer(titato[2][0])].getName();
				msg += " won on the upper right diagonal";
				msg += ":"+players[detPlayer(titato[2][0])].getPiece();
				this.victor = detPlayer(titato[2][0]);
				break;
			case 9:
				msg = "The board is full. DRAW!";
				this.victor = 1;
				break;
		}
		return msg;
	}
	
// METHODS ADDED FOR THREADS
	public boolean yourTurn(int thisPlayer) {
		boolean result = false;
		if(this.currentPlayer == thisPlayer) {
			result = true;
		}
		return result;
	}
	
	public synchronized void giveTheName(String name, int i) {
		players[i].setName(name);
		players[i].setPiece(i);
		if(i == PLAYERONE) {
			pOneNameGot = true;
		} else {
			pTwoNameGot = true;
		}
	}
	

	public synchronized String otherNameGiven(int i) {
		String result = null;
		if(i == PLAYERONE) {
			if(pTwoNameGot) {
				result = players[PLAYERTWO].getName();
			}
		} else {
			if(pOneNameGot) {
				result = players[PLAYERONE].getName();
			}
		}
		return result;
	}
	
	public String getPlayerInfo(int i) {
		String result;
		result = players[i].getName()+":"+players[i].getPiece()+":"+players[i].getVictories();
		return result;
	}
//these methods are used to set or find out if either player has completed
//their move yet.
	public boolean hasMoved() {
		return this.currentMoved;
	}
	
	public void doneMove() {
		this.currentMoved = true;
	}
	public int getMove() {
		return this.currentMove;
	}
	public synchronized void switchPlayers() {
		if(this.currentPlayer == PLAYERONE) {
			this.currentPlayer = PLAYERTWO;
		} else {
			this.currentPlayer = PLAYERONE;
		}
		this.currentMoved = false;
		this.currentMove = 0;
	}
	public synchronized void setCurrentPlayer(int i) {
		this.currentPlayer = i;
	}
//these are methods used for checking and setting
//the booleans used to tell players if they want to reset
	
	public synchronized void iWantReset(int i) {
		if(i == PLAYERONE) {
			this.pOneReset[0] = true;
		} else {
			this.pTwoReset[0] = true;
		}
	}	
	public boolean doesOtherReset(int i) {
		boolean result;
		if(i == PLAYERONE) {
			result = pTwoReset[0];
		} else {
			result = pOneReset[0];
		}
		return result;
	}
	public boolean otherAnswerReset(int i) {
		boolean result;
		if(i == PLAYERONE) {
			result = pOneReset[2];
		} else {
			result = pTwoReset[2];
		}
		return result;
	}
	public synchronized void answerReset(int i, boolean answer) {
		if(i == PLAYERONE) {
			pTwoReset[1] = true;
			pTwoReset[2] = answer;
		} else {
			pOneReset[1] = true;
			pOneReset[2] = answer;
		}
	}
	public synchronized void resetMyReset(int i) {
		if(i == PLAYERONE) {
			for(int x=0; x<CALLNR; x++) {
				this.pOneReset[x] = false;
			}
		} else {
			for(int x=0; x<CALLNR; x++) {
				this.pTwoReset[x] = false;
			}
		}
	}
	//these are methods used for checking and setting
	//the booleans used to tell players if they want a new game
	public synchronized void iWantNew(int i) {
		if(i == PLAYERONE) {
			this.pOneNew[0] = true;
		} else {
			this.pTwoNew[0] = true;
		}
	}
	public boolean doesOtherNew(int i) {
		boolean result;
		if(i == PLAYERONE) {
			result = pTwoNew[0];
		} else {
			result = pOneNew[0];
		}
		return result;
	}
	public boolean otherAnswerNew(int i) {
		boolean result;
		if(i == PLAYERONE) {
			result = pOneNew[2];
		} else {
			result = pTwoNew[2];
		}
		return result;
	}
	public synchronized void answerNew(int i, boolean answer) {
		if(i == PLAYERONE) {
			pTwoNew[1] = true;
			pTwoNew[2] = answer;
			System.out.println("PLAYER ONE ANSWERED: "+answer);
		} else {
			pOneNew[1] = true;
			pOneNew[2] = answer;
			System.out.println("PLAYER TWO ANSWERED: "+answer);
		}
	}
	public synchronized void resetMyNew(int i) {
		if(i == PLAYERONE) {
			for(int x=0; x<CALLNR; x++){
				this.pOneNew[x] = false;
			}
		} else {
			for(int x=0; x<CALLNR; x++) {
				this.pTwoNew[x] = false;
			}
		}
	}
//has the other player answered the reset or new game call
//i = 1 is newgame call
//i = 2 is reset call
	public boolean doesOtherAnswerNR(int playNum, int i) {
		boolean result;
		if(i == NEWGAMECALL)  {
			if(playNum == PLAYERONE) {
				result = pOneNew[1];
			} else {
				result = pTwoNew[1];
			}
		} else {
			if(playNum == PLAYERONE) {
				result = pOneReset[1];
			} else {
				result = pTwoReset[1];
			}
		}
		return result;
	}
	//determine if your opponent has quit
	public boolean doesOtherQuit(int i) {
		boolean result;
		if(i == PLAYERONE) {
			result = this.pTwoQuit;
		} else {
			result = this.pOneQuit;
		}
		return result;
	}
//tell your opponent your quitting
	public void imQuitting(int i) {
		if(i == PLAYERONE) {
			this.pOneQuit = true;
		} else {
			pTwoQuit = true;
		}
	}
	
	public int getFullVic() {
		return this.fullVicAchieved;
	}
	
	public void noMove() {
		this.currentMoved = false;
	}
	
	public int detPlayer(char c) {
		int result;
		if(players[0].getPiece() == c) {
			result = 0;
		} else {
			result = 1;
		}
		return result;
	}
	
	public void setVictor(int i) {
		this.victor = i;
	}
	public synchronized int getVictor() {
		return this.victor;
	}
	
	public synchronized int getReadyForNR() {
		return this.readyForNR;
	}
	public synchronized void incReadyForNR() {
		this.readyForNR++;
	}
	public synchronized void resetReadyForNR() {
		this.readyForNR = 0;
	}
}




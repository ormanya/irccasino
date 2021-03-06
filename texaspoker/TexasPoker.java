/*
    Copyright (C) 2013-2014 Yizhe Shen <brrr@live.ca>

    This file is part of irccasino.

    irccasino is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    irccasino is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with irccasino.  If not, see <http://www.gnu.org/licenses/>.
 */

package irccasino.texaspoker;

import irccasino.cardgame.CardDeck;
import irccasino.GameManager;
import irccasino.cardgame.CardGame;
import irccasino.cardgame.Hand;
import irccasino.cardgame.Player;
import irccasino.cardgame.Record;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.pircbotx.*;

public class TexasPoker extends CardGame{
    
    public enum PokerState {
        NONE, PRE_START, BLINDS, BETTING, SHOWDOWN, CONTINUE_ROUND, END_ROUND
    }
    
    public enum PokerBet {
        NONE, PRE_FLOP, FLOP, TURN, RIVER;
        private static final PokerBet[] vals = values();
        
        public PokerBet next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }
    
    protected ArrayList<PokerPot> pots;
    protected PokerPot currentPot;
    protected PokerPlayer dealer;
    protected PokerPlayer smallBlind;
    protected PokerPlayer bigBlind;
    protected PokerPlayer topBettor;
    protected Hand community;
    // In-game properties
    protected PokerState state;
    protected PokerBet betState;
    protected int currentBet;
    protected int minRaise;
    
    public TexasPoker() {
        super();
    }
    
    /**
     * The default constructor for TexasPoker, subclass of CardGame.
     * This constructor loads the default INI file.
     * 
     * @param parent The bot that uses an instance of this class
     * @param commChar The command char
     * @param gameChannel The IRC channel in which the game is to be run.
     */
    public TexasPoker(GameManager parent, char commChar, Channel gameChannel){
        this(parent, commChar, gameChannel, "texaspoker.ini");
    }
    
    /**
     * Allows a custom INI file to be loaded.
     * 
     * @param parent The bot that uses an instance of this class
     * @param commChar The command char
     * @param gameChannel The IRC channel in which the game is to be run.
     * @param customINI the file path to a custom INI file
     */
    public TexasPoker(GameManager parent, char commChar, Channel gameChannel, String customINI) {
        super(parent, commChar, gameChannel, customINI);
    }
    
    /////////////////////////////////////////
    //// Methods that process IRC events ////
    /////////////////////////////////////////
    @Override
    public void processCommand(User user, String command, String[] params){
        String nick = user.getNick();
        String host = user.getHostmask();
        
        // Commands available in TexasPoker.
        if (command.equalsIgnoreCase("join") || command.equalsIgnoreCase("j")){
            join(nick, host);
        } else if (command.equalsIgnoreCase("leave") || command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("l") || command.equalsIgnoreCase("q")){
            leave(nick, params);
        } else if (command.equalsIgnoreCase("last")) {
            last(nick, params);
        } else if (command.equalsIgnoreCase("start") || command.equalsIgnoreCase("go")) {
            start(nick, params);
        } else if (command.equalsIgnoreCase("stop")) {
            stop(nick, params);
        } else if (command.equalsIgnoreCase("bet") || command.equalsIgnoreCase("b")) {
            bet(nick, params);
        } else if (command.equalsIgnoreCase("c") || command.equalsIgnoreCase("ca") || command.equalsIgnoreCase("call")) {
            call(nick, params);
        } else if (command.equalsIgnoreCase("x") || command.equalsIgnoreCase("ch") || command.equalsIgnoreCase("check")) {
            check(nick, params);
        } else if (command.equalsIgnoreCase("fold") || command.equalsIgnoreCase("f")) {
            fold(nick, params);
        } else if (command.equalsIgnoreCase("raise") || command.equalsIgnoreCase("r")) {
            raise(nick, params);
        } else if (command.equalsIgnoreCase("allin") || command.equalsIgnoreCase("a")){
            allin(nick, params);
        } else if (command.equalsIgnoreCase("community") || command.equalsIgnoreCase("comm")){
            community(nick, params);
        } else if (command.equalsIgnoreCase("hand")) {
            hand(nick, params);
        } else if (command.equalsIgnoreCase("turn")) {
            turn(nick, params);
        } else if (command.equalsIgnoreCase("cash") || command.equalsIgnoreCase("stack")) {
            cash(nick, params);
        } else if (command.equalsIgnoreCase("netcash") || command.equalsIgnoreCase("net")) {
            netcash(nick, params);
        } else if (command.equalsIgnoreCase("bank")) {
            bank(nick, params);
        } else if (command.equalsIgnoreCase("bankrupts")) {
            bankrupts(nick, params);
        } else if (command.equalsIgnoreCase("winnings")) {
            winnings(nick, params);
        } else if (command.equalsIgnoreCase("winrate")) {
            winrate(nick, params);
        } else if (command.equalsIgnoreCase("rounds")) {
            rounds(nick, params);
        } else if (command.equalsIgnoreCase("player") || command.equalsIgnoreCase("p")){
            player(nick, params);
        } else if (command.equalsIgnoreCase("deposit")) {
            deposit(nick, params);
        } else if (command.equalsIgnoreCase("rathole")) {
            rathole(nick, params);
        } else if (command.equalsIgnoreCase("withdraw")) {
            withdraw(nick, params);
        } else if (command.equalsIgnoreCase("players")) {
            players(nick, params);
        } else if (command.equalsIgnoreCase("waitlist")) {
            waitlist(nick, params);
        } else if (command.equalsIgnoreCase("blacklist")) {
            blacklist(nick, params);
        } else if (command.equalsIgnoreCase("rank")) {
            rank(nick, params);
        } else if (command.equalsIgnoreCase("top")) {
            top(nick, params);
        } else if (command.equalsIgnoreCase("away")){
            away(nick, params);
        } else if (command.equalsIgnoreCase("back")){
            back(nick, params);
        } else if (command.equalsIgnoreCase("ping")) {
            ping(nick, params);
        } else if (command.equalsIgnoreCase("simple")) {
            simple(nick, params);
        } else if (command.equalsIgnoreCase("stats")){
            stats(nick, params);
        } else if (command.equalsIgnoreCase("grules") || command.equalsIgnoreCase("gamerules")) {
            grules(nick, params);
        } else if (command.equalsIgnoreCase("ghelp") || command.equalsIgnoreCase("gamehelp")) {
            ghelp(nick, params);
        } else if (command.equalsIgnoreCase("gcommands") || command.equalsIgnoreCase("gamecommands")) {
            gcommands(user, nick, params);
        } else if (command.equalsIgnoreCase("game")) {
            game(nick, params);
        } else if (command.equalsIgnoreCase("gversion")) {
            gversion(nick, params);
        /* Op commands */
        } else if (command.equalsIgnoreCase("fj") || command.equalsIgnoreCase("fjoin")){
            fjoin(user, nick, params);
        } else if (command.equalsIgnoreCase("fl") || command.equalsIgnoreCase("fq") || command.equalsIgnoreCase("fquit") || command.equalsIgnoreCase("fleave")){
            fleave(user, nick, params);
        } else if (command.equalsIgnoreCase("flast")) {
            flast(user, nick, params);
        } else if (command.equalsIgnoreCase("fstart") || command.equalsIgnoreCase("fgo")){
            fstart(user, nick, params);
        } else if (command.equalsIgnoreCase("fstop")){
            fstop(user, nick, params);
        } else if (command.equalsIgnoreCase("fb") || command.equalsIgnoreCase("fbet")){
            fbet(user, nick, params);
        } else if (command.equalsIgnoreCase("fa") || command.equalsIgnoreCase("fallin")){
            fallin(user, nick, params);
        } else if (command.equalsIgnoreCase("fr") || command.equalsIgnoreCase("fraise")){
            fraise(user, nick, params);
        } else if (command.equalsIgnoreCase("fc") || command.equalsIgnoreCase("fca") || command.equalsIgnoreCase("fcall")){
            fcall(user, nick, params);
        } else if (command.equalsIgnoreCase("fx") || command.equalsIgnoreCase("fch") || command.equalsIgnoreCase("fcheck")){
            fcheck(user, nick, params);
        } else if (command.equalsIgnoreCase("ff") || command.equalsIgnoreCase("ffold")){
            ffold(user, nick, params);
        } else if (command.equalsIgnoreCase("fdeposit")) {
            fdeposit(user, nick, params);
        } else if (command.equalsIgnoreCase("fwithdraw")) {
            fwithdraw(user, nick, params);
        } else if (command.equalsIgnoreCase("shuffle")){
            shuffle(user, nick, params);
        } else if (command.equalsIgnoreCase("cards")) {
            cards(user, nick, params);
        } else if (command.equalsIgnoreCase("discards")) {
            discards(user, nick, params);
        } else if (command.equalsIgnoreCase("settings")) {
            settings(user, nick, params);
        } else if (command.equalsIgnoreCase("set")){
            set(user, nick, params);
        } else if (command.equalsIgnoreCase("get")) {
            get(user, nick, params);
        } else if (command.equalsIgnoreCase("resetaway")){
            resetaway(user, nick, params);
        } else if (command.equalsIgnoreCase("resetsimple")) {
            resetsimple(user, nick, params);
        } else if (command.equalsIgnoreCase("reload")) {
            reload(user, nick, params);
        } else if (command.equalsIgnoreCase("trim")) {
            trim(user, nick, params);
        } else if (command.equalsIgnoreCase("query") || command.equalsIgnoreCase("sql")) {
            query(user, nick, params);
        } else if (command.equalsIgnoreCase("migrate")) {
            migrate(user, nick, params);
        } else if (command.equalsIgnoreCase("test1")) {
            test1(user, nick, params);
        } else if (command.equalsIgnoreCase("test2")) {
            test2(user, nick, params);
        } else if (command.equalsIgnoreCase("test3")){
            test3(user, nick, params);
        }
    }

    /////////////////////////
    //// Command methods ////
    /////////////////////////
    
    /**
     * Starts a new round.
     * @param nick
     * @param params 
     */
    protected void start(String nick, String[] params) {
        if (!isJoined(nick)) {
            informPlayer(nick, getMsg("no_join"));
        } else if (isInProgress()) {
            informPlayer(nick, getMsg("round_started"));
        } else if (joined.size() < 2) {
            showMsg(getMsg("no_players"));
        } else if (startCount > 0) {
            informPlayer(nick, getMsg("no_manual_start"));
        } else {
            if (params.length > 0){
                try {
                    startCount = Math.min(get("autostarts") - 1, Integer.parseInt(params[0]) - 1);
                } catch (NumberFormatException e) {
                    // Do nothing and proceed
                }
            }
            state = PokerState.PRE_START;
            startTime = System.currentTimeMillis() / 1000;
            showStartRound();
            setStartRoundTask();
        }
    }
    
    /**
     * Sets a bet for the current player.
     * @param nick
     * @param params 
     */
    protected void bet(String nick, String[] params) {
        if (!isJoined(nick)){
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (findJoined(nick) != currentPlayer){
            informPlayer(nick, getMsg("wrong_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else if (params.length < 1){
            informPlayer(nick, getMsg("no_parameter"));  
        } else {
            try {
                bet(Integer.parseInt(params[0]));
            } catch (NumberFormatException e) {
                informPlayer(nick, getMsg("bad_parameter"));
            }
        }
    }
    
    /**
     * Makes the current player call.
     * @param nick
     * @param params 
     */
    protected void call(String nick, String[] params) {
        if (!isJoined(nick)){
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (findJoined(nick) != currentPlayer){
            informPlayer(nick, getMsg("wrong_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            call();
        }
    }
    
    /**
     * Makes the current player check.
     * @param nick
     * @param params 
     */
    protected void check(String nick, String[] params) {
        if (!isJoined(nick)){
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (findJoined(nick) != currentPlayer){
            informPlayer(nick, getMsg("wrong_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            check();
        }
    }
    
    /**
     * Makes the current player fold.
     * @param nick
     * @param params 
     */
    protected void fold(String nick, String[] params) {
        if (!isJoined(nick)){
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (findJoined(nick) != currentPlayer){
            informPlayer(nick, getMsg("wrong_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            fold();
        }
    }
    
    /**
     * Makes the current player raise.
     * @param nick
     * @param params 
     */
    protected void raise(String nick, String[] params) {
        if (!isJoined(nick)){
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (findJoined(nick) != currentPlayer){
            informPlayer(nick, getMsg("wrong_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else if (params.length < 1){
            informPlayer(nick, getMsg("no_parameter"));
        } else {
            try {
                bet(Integer.parseInt(params[0]) + currentBet);
            } catch (NumberFormatException e) {
                informPlayer(nick, getMsg("bad_parameter"));
            }
        }
    }
    
    /**
     * Makes the current player go all in.
     * @param nick
     * @param params 
     */
    protected void allin(String nick, String[] params) {
        if (!isJoined(nick)){
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (findJoined(nick) != currentPlayer){
            informPlayer(nick, getMsg("wrong_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            bet(currentPlayer.getInteger("cash"));
        }
    }
    
    /**
     * Outputs the current community cards.
     * @param nick
     * @param params 
     */
    protected void community(String nick, String[] params) {
        if (!isJoined(nick)) {
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else {
            showCommunityCards(false);
        }
    }
    
    /**
     * Informs a player of his hand.
     * @param nick
     * @param params 
     */
    protected void hand(String nick, String[] params) {
        if (!isJoined(nick)) {
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else {
            PokerPlayer p = (PokerPlayer) findJoined(nick);
            informPlayer(nick, getMsg("tp_hand"), p.getHand());
        }
    }
    
    /**
     * Displays whose turn it is.
     * @param nick
     * @param params 
     */
    protected void turn(String nick, String[] params) {
        if (!isJoined(nick)) {
            informPlayer(nick, getMsg("no_join"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (currentPlayer == null) {
            informPlayer(nick, getMsg("nobody_turn"));
        } else {
            showMsg(getMsg("tp_turn"), currentPlayer.getNickStr(), 
                    currentBet-currentPlayer.getInteger("bet"), 
                    currentPlayer.getInteger("bet"), currentBet, getCashInPlay(), 
                    currentPlayer.getInteger("cash")-currentPlayer.getInteger("bet"));
        }
    }
    
    /**
     * Displays the current players in the game.
     * @param nick
     * @param params 
     */
    protected void players(String nick, String[] params) {
        if (isInProgress()){
            showTablePlayers();
        } else {
            showMsg(getMsg("players"), getPlayerListString(joined));
        }
    }
    
    /**
     * Attempts to force the game to start.
     * @param user
     * @param nick
     * @param params 
     */
    protected void fstart(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (isInProgress()) {
            informPlayer(nick, getMsg("round_started"));
        } else if (joined.size() < 2) {
            showMsg(getMsg("no_players"));
        } else {
            if (params.length > 0){
                try {
                    startCount = Math.min(get("autostarts") - 1, Integer.parseInt(params[0]) - 1);
                } catch (NumberFormatException e) {
                    // Do nothing and proceed
                }
            }
            state = PokerState.PRE_START;
            showStartRound();
            setStartRoundTask();
        }
    }
    
    /**
     * Forces a round to end. Use only as last resort. Data will be lost.
     * @param user
     * @param nick
     * @param params 
     */
    protected void fstop(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else {
            cancelStartRoundTask();
            cancelIdleOutTask();
            for (Player p : joined) {
                resetPlayer((PokerPlayer) p);
            }
            resetGame();
            startCount = 0;
            showMsg(getMsg("end_round"), getGameNameStr(), commandChar);
            state = PokerState.NONE;
            betState = PokerBet.NONE;
        }
    }
    
    /**
     * Forces the current player to bet.
     * @param user
     * @param nick
     * @param params 
     */
    protected void fbet(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (currentPlayer == null) {
            informPlayer(nick, getMsg("nobody_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else if (params.length < 1){
            informPlayer(nick, getMsg("no_parameter"));       
        } else {
            try {
                bet(Integer.parseInt(params[0]));
            } catch (NumberFormatException e) {
                informPlayer(nick, getMsg("bad_parameter"));
            }
        }
    }
    
    /**
     * Forces the current player to go all in.
     * @param user
     * @param nick
     * @param params 
     */
    protected void fallin(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (currentPlayer == null) {
            informPlayer(nick, getMsg("nobody_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            bet(currentPlayer.getInteger("cash"));
        }
    }
    
    /**
     * Forces the current player to raise.
     * @param user
     * @param nick
     * @param params 
     */
    protected void fraise(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (currentPlayer == null) {
            informPlayer(nick, getMsg("nobody_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else if (params.length < 1){
            informPlayer(nick, getMsg("no_parameter"));        
        } else {
            try {
                bet(Integer.parseInt(params[0]) + currentBet);
            } catch (NumberFormatException e) {
                informPlayer(nick, getMsg("bad_parameter"));
            }    
        }
    }
    
    /**
     * Forces the current player to call.
     * @param user
     * @param nick
     * @param params 
     */
    protected void fcall(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (currentPlayer == null) {
            informPlayer(nick, getMsg("nobody_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            call();
        }
    }
    
    /**
     * Forces the current player to check.
     * @param user
     * @param nick
     * @param params 
     */
    protected void fcheck(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (currentPlayer == null) {
            informPlayer(nick, getMsg("nobody_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            check();
        }
    }
    
    /**
     * Forces the current player to fold.
     * @param user
     * @param nick
     * @param params 
     */
    protected void ffold(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (!isInProgress()) {
            informPlayer(nick, getMsg("no_start"));
        } else if (currentPlayer == null) {
            informPlayer(nick, getMsg("nobody_turn"));
        } else if (state.equals(PokerState.CONTINUE_ROUND)) {
            informPlayer(nick, getMsg("game_lagging"));
        } else {
            fold();
        }
    }
    
    /**
     * Shuffles the deck.
     * @param user
     * @param nick
     * @param params 
     */
    protected void shuffle(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (isInProgress()) {
            informPlayer(nick, getMsg("wait_round_end"));
        } else {
            shuffleDeck();
        }
    }
    
    /**
     * Reloads library files and settings.
     * @param user
     * @param nick
     * @param params 
     */
    protected void reload(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (isInProgress()) {
            informPlayer(nick, getMsg("wait_round_end"));
        } else {
            awayList.clear();
            notSimpleList.clear();
            cmdMap.clear();
            opCmdMap.clear();
            aliasMap.clear();
            msgMap.clear();
            sqlMap.clear();
            loadIni();
            loadHostList("away.txt", awayList);
            loadHostList("simple.txt", notSimpleList);
            loadStrLib();
            loadSQLLib();
            loadHelp();
            showMsg(getMsg("reload"));
        }
    }
    
    /**
     * Performs test 1. Tests if the game will properly determine the winner 
     * with 2-5 players.
     * @param user
     * @param nick
     * @param params 
     */
    protected void test1(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (isInProgress()) {
            informPlayer(nick, getMsg("wait_round_end"));
        } else if (params.length < 1) {
            informPlayer(nick, getMsg("no_parameter"));  
        } else {
            try {
                ArrayList<PokerPlayer> peeps = new ArrayList<>();
                PokerHand ph;
                int winners = 1;
                int number = Integer.parseInt(params[0]);
                if (number > 5 || number < 2){
                    throw new NumberFormatException();
                }
                // Generate new players
                for (int ctr = 0; ctr < number; ctr++){
                    PokerPlayer p = new PokerPlayer(Integer.toString(ctr + 1));
                    peeps.add(p);
                    dealHand(p);                            
                    showMsg("Player " + p.getNickStr() + ": " + p.getHand().toString());
                }
                // Generate community cards
                Hand comm = new Hand();
                for (int ctr = 0; ctr < 5; ctr++){
                    dealCard(comm);
                }
                showMsg(formatHeader(" Community: ") + " " + comm.toString());
                // Propagate poker hands
                for (PokerPlayer p : peeps){
                    ph = p.getPokerHand();
                    ph.addAll(p.getHand());
                    ph.addAll(comm);
                    Collections.sort(ph);
                    Collections.reverse(ph);
                    ph.getValue();
                }
                // Sort hands in descending order
                Collections.sort(peeps);
                Collections.reverse(peeps);
                // Determine number of winners
                for (int ctr = 1; ctr < peeps.size(); ctr++){
                    if (peeps.get(0).compareTo(peeps.get(ctr)) == 0){
                        winners++;
                    } else {
                        break;
                    }
                }
                // Output poker hands with winners listed
                for (int ctr=0; ctr < winners; ctr++){
                    PokerPlayer p = peeps.get(ctr);
                    ph = p.getPokerHand();
                    showMsg("Player "+p.getNickStr()+
                            " ("+p.getHand()+"), "+ph.getName()+": " + ph+" (WINNER)");
                }
                for (int ctr=winners; ctr < peeps.size(); ctr++){
                    PokerPlayer p = peeps.get(ctr);
                    ph = p.getPokerHand();
                    showMsg("Player "+p.getNickStr()+
                            " ("+p.getHand()+"), "+ph.getName()+": " + ph);
                }
                // Discard and shuffle
                for (int ctr=0; ctr < number; ctr++){
                    resetPlayer(peeps.get(ctr));
                }
                deck.addToDiscard(comm);
                comm.clear();
                deck.refillDeck();
                showMsg(getMsg("separator"));
            } catch (NumberFormatException e) {
                informPlayer(nick, getMsg("bad_parameter"));
            }
        }
    }
    
    /**
     * Performs test 2. Tests if arbitrary hands will be scored properly.
     * @param user
     * @param nick
     * @param params 
     */
    protected void test2(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (isInProgress()) {
            informPlayer(nick, getMsg("wait_round_end"));
        } else if (params.length < 1){
            informPlayer(nick, getMsg("no_parameter"));
        } else {
            try {
                int number = Integer.parseInt(params[0]);
                if (number > 52 || number < 5){
                    throw new NumberFormatException();
                }
                PokerHand h = new PokerHand();
                for (int ctr = 0; ctr < number; ctr++){
                    dealCard(h);
                }
                showMsg(h.toString(0, h.size()));
                Collections.sort(h);
                Collections.reverse(h);
                h.getValue();
                showMsg(h.getName()+": " + h);
                deck.addToDiscard(h);
                h.clear();
                deck.refillDeck();
                showMsg(getMsg("separator"));
            } catch (NumberFormatException e) {
                informPlayer(nick, getMsg("bad_parameter"));
            }
        }
    }
    
    /**
     * Performs test 3. Tests the percentage calculator for 2-5 players.
     * @param user
     * @param nick
     * @param params 
     */
    protected void test3(User user, String nick, String[] params) {
        if (!channel.isOp(user)) {
            informPlayer(nick, getMsg("ops_only"));
        } else if (isInProgress()) {
            informPlayer(nick, getMsg("wait_round_end"));
        } else if (params.length < 1){
            informPlayer(nick, getMsg("no_parameter"));
        } else {
            try {
                int number = Integer.parseInt(params[0]);
                if (number > 5 || number < 2){
                    throw new NumberFormatException();
                }
                ArrayList<PokerPlayer> peeps = new ArrayList<>();
                PokerSimulator sim = new PokerSimulator();
                
                // Generate players and deal cards
                for (int ctr = 0; ctr < number; ctr++) {
                    PokerPlayer p = new PokerPlayer(Integer.toString(ctr));
                    peeps.add(p);
                    dealHand(p);
                    showMsg("Player " + p.getNickStr() + ": " + p.getHand().toString());
                }
                sim.addPlayers(peeps);
                betState = PokerBet.PRE_FLOP;
                
                // Run simulations
                while (true) {
                    sim.addCommunity(community);
                    sim.run();
                    showMsg(sim.toString());
                    
                    // Don't deal any more cards after the river
                    if (betState == PokerBet.RIVER) {
                        break;
                    }
                    
                    // Deal community
                    dealCommunity();
                    showMsg(formatHeader(" Community: ") + " " + community.toString());
                    sim.reset();
                    betState = betState.next();
                }
                
                // Discard and shuffle
                for (PokerPlayer p : peeps){
                    resetPlayer(p);
                }
                discardCommunity();
                deck.refillDeck();
                betState = PokerBet.NONE;
                showMsg(getMsg("separator"));
            } catch (NumberFormatException e) {
                informPlayer(nick, getMsg("bad_parameter"));
            }
        }
    }
    
    /////////////////////////////////
    //// Game management methods ////
    /////////////////////////////////
    
    @Override
    public void addPlayer(String nick, String host) {
        addPlayer(new PokerPlayer(nick));
    }
    
    @Override
    public void addWaitlistPlayer(String nick, String host) {
        Player p = new PokerPlayer(nick);
        waitlist.add(p);
        informPlayer(p.getNick(), getMsg("join_waitlist"));
    }
    
    @Override
    public void startRound() {
        if (joined.size() < 2){
            startCount = 0;
            endRound();
        } else {
            state = PokerState.BLINDS;
            betState = PokerBet.PRE_FLOP;
            setButton();
            setBlindBets();
            showTablePlayers();
            showButtonInfo();
            dealTable();
            continueRound();
        }
    }
    
    @Override
    public void continueRound() {
        state = PokerState.CONTINUE_ROUND;
        
        // Set currentPlayer if it hasn't been set yet
        if (currentPlayer == null) {
            if (betState.equals(PokerBet.PRE_FLOP)) {
                currentPlayer = bigBlind;
            } else {
                currentPlayer = dealer;
            }
        }
        
        /*
         * Find the next available player. If we reach the currentPlayer or 
         * topBettor then stop looking.
         */
        Player nextPlayer = getPlayerAfter(currentPlayer);
        while ((nextPlayer.has("fold") || nextPlayer.has("allin")) && 
                nextPlayer != currentPlayer && nextPlayer != topBettor) {
            nextPlayer = getPlayerAfter(nextPlayer);
        }
        
        if (getNumberNotFolded() < 2) {
            // If only one player hasn't folded, expedite to end of round
            addBetsToPot();
            currentPlayer = null;
            topBettor = null;
            
            // Deal the rest of the community cards
            while (!betState.equals(PokerBet.RIVER)) {
                burnCard();
                dealCommunity();
                betState = betState.next();
                
                // Show final community if required
                if (settings.get("revealcommunity") == 1 && betState.equals(PokerBet.RIVER)){
                    showCommunityCards(true);
                }
            }

            endRound();
        } else if (nextPlayer == topBettor || nextPlayer == currentPlayer || 
                   (betState == PokerBet.PRE_FLOP && getNumberCanBet() == 0)) {
            // If we reach the firstPlayer or topBettor, then we have reached 
            // the end of a round of betting and we should deal community cards.
            minRaise = get("minbet");
            addBetsToPot();
            currentPlayer = null;
            topBettor = null;
            
            if (betState.equals(PokerBet.RIVER)){
                // If all community cards have been dealt, move to end of round.
                endRound();
            } else if (getNumberCanBet() < 2) {
                // If showdown, show player hands and their win/tie 
                // probabilities immediately and each time additional community
                // cards are revealed. Adds a dramatic delay between each reveal.
                state = PokerState.SHOWDOWN;
                PokerSimulator sim = new PokerSimulator();
                ArrayList<PokerPlayer> list = pots.get(0).getEligibles();
                sim.addPlayers(list);
                
                while (!betState.equals(PokerBet.RIVER)) {
                    sim.addCommunity(community);
                    sim.run();
                    showShowdown(list, sim);
                    sim.reset();
                    
                    // Add a delay for dramatic effect
                    try { Thread.sleep(get("showdown") * 1000); } catch (InterruptedException e){}

                    // Deal some community cards
                    burnCard();
                    dealCommunity();
                    betState = betState.next();
                    showCommunityCards(false);
                }
                endRound();
            } else {
                burnCard();
                dealCommunity();
                betState = betState.next();
                showCommunityCards(false);
                continueRound();
            }
        // Continue to the next bettor
        } else {
            state = PokerState.BETTING;
            currentPlayer = nextPlayer;
            showMsg(getMsg("tp_turn"), currentPlayer.getNickStr(), 
                    currentBet - currentPlayer.getInteger("bet"), 
                    currentPlayer.getInteger("bet"), currentBet, getCashInPlay(), 
                    currentPlayer.getInteger("cash") - currentPlayer.getInteger("bet"));
            setIdleOutTask();
        }
    }
    
    @Override
    public void endRound() {
        state = PokerState.END_ROUND;

        // Check if anybody left during post-start waiting period
        if (joined.size() > 1 && pots.size() > 0) {
            // Give all non-folded players the community cards
            for (int ctr = 0; ctr < joined.size(); ctr++){
                PokerPlayer p = (PokerPlayer) joined.get(ctr);
                if (!p.has("fold")){
                    p.getPokerHand().addAll(p.getHand());
                    p.getPokerHand().addAll(community);
                    Collections.sort(p.getPokerHand());
                    Collections.reverse(p.getPokerHand());
                }
            }

            // Determine the winners of each pot
            showResults();

            // Show updated player stacks sorted in descending order
            showStacks();
            
            /* Bookkeeping tasks
             * 1. Increment the number of rounds played for each player
             * 2. Increment idles if idled out
             * 2. Make auto-withdrawals
             * 3. Save player stats
             */
            for (Player p : joined) {
                p.add("rounds", 1);
                if (p.getBoolean("idled")) {
                    p.add("idles", 1);
                }
                if (!p.has("cash") && p.has("bank")) {
                    int amount = Math.min(p.getInteger("bank"), get("cash"));
                    p.bankTransfer(-amount);
                    saveDBPlayerBanking(p);
                    informPlayer(p.getNick(), getMsg("auto_withdraw"), amount);
                }
            }
            
            // Save game stats to DB
            endTime = System.currentTimeMillis() / 1000;
            saveDBPlayerDataBatch(joined);
            saveDBGameStats();
            
            /* Clean-up tasks
             * 1. Remove players who are bankrupt and set respawn timers
             * 2. Remove players who have quit or used the 'last' command
             * 3. Reset the players
             */
            for (int ctr = joined.size()-1; ctr >= 0 ; ctr--) {
                PokerPlayer p = (PokerPlayer) joined.get(ctr);
                if (!p.has("cash")) {
                    // Give penalty to players with no cash in their bank
                    p.add("bankrupts", 1);
                    blacklist.add(p);
                    removeJoined(p);
                    showMsg(getMsg("unjoin_bankrupt"), p.getNickStr(), joined.size());
                    setRespawnTask(p);
                } else if (p.has("quit") || p.has("last")) {
                    removeJoined(p);
                    showMsg(getMsg("unjoin"), p.getNickStr(), joined.size());
                }
                resetPlayer(p);
            }
        } else {
            showMsg(getMsg("no_players"));
        }
        
        resetGame();
        if (startCount > 0) {
            showMsg(getMsg("end_round_auto"), getGameNameStr(), commandChar);
        } else {
            showMsg(getMsg("end_round"), getGameNameStr(), commandChar);
        }
        mergeWaitlist();
        state = PokerState.NONE;
        betState = PokerBet.NONE;
        
        // Check if auto-starts remaining
        if (startCount > 0 && joined.size() > 1){
            startCount--;
            state = PokerState.PRE_START;
            startTime = System.currentTimeMillis() / 1000;
            showStartRound();
            setStartRoundTask();
        } else {
            startCount = 0;
        }
    }
    
    @Override
    public void endGame() {
        cancelStartRoundTask();
        cancelIdleOutTask();
        cancelRespawnTasks();
        gameTimer.cancel();
        deck = null;
        community = null;
        pots.clear();
        currentPot = null;
        currentPlayer = null;
        dealer = null;
        smallBlind = null;
        bigBlind = null;
        topBettor = null;
        devoiceAll();
        showMsg(getMsg("game_end"), getGameNameStr());
        joined.clear();
        waitlist.clear();
        blacklist.clear();
        awayList.clear();
        notSimpleList.clear();
        cmdMap.clear();
        opCmdMap.clear();
        aliasMap.clear();
        msgMap.clear();
        settings.clear();
    }
    
    @Override
    public void resetGame() {
        currentBet = 0;
        minRaise = 0;
        discardCommunity();
        currentPot = null;
        pots.clear();
        currentPlayer = null;
        bigBlind = null;
        smallBlind = null;
        topBettor = null;
        deck.refillDeck();
    }
    
    @Override
    public void leave(String nick) {
        PokerPlayer p = (PokerPlayer) findJoined(nick);
        
        switch (state){
            case NONE: case PRE_START:
                removeJoined(p);
                showMsg(getMsg("unjoin"), p.getNickStr(), joined.size());
                break;
            case BETTING:
                p.put("quit", true);
                informPlayer(p.getNick(), getMsg("remove_end_round"));
                if (p == currentPlayer) {
                    fold();
                } else if (!p.has("fold")){
                    p.put("fold", true);
                    // Remove this player from any existing pots
                    if (currentPot != null && currentPot.isEligible(p)){
                        currentPot.disqualify(p);
                    }
                    for (int ctr = 0; ctr < pots.size(); ctr++){
                        PokerPot cPot = pots.get(ctr);
                        if (cPot.isEligible(p)){
                            cPot.disqualify(p);
                        }
                    }
                    // If there is only one player who hasn't folded,
                    // force call on that remaining player (whose turn it must be)
                    if (getNumberNotFolded() == 1 && !state.equals(PokerState.CONTINUE_ROUND)){
                        call();
                    }
                }
                break;
            case BLINDS: case CONTINUE_ROUND:
                p.put("quit", true);
                p.put("fold", true);
                informPlayer(p.getNick(), getMsg("remove_end_round"));
                break;
            case SHOWDOWN: case END_ROUND:
                p.put("quit", true);
                informPlayer(p.getNick(), getMsg("remove_end_round"));
                break;
            default:
                break;
        }
    }
    
    /**
     * Returns next player after the specified player.
     * @param p the specified player
     * @return the next player
     */
    protected Player getPlayerAfter(Player p){
        return joined.get((joined.indexOf(p) + 1) % joined.size());
    }
    
    @Override
    protected void resetPlayer(Player p) {
        discardPlayerHand((PokerPlayer) p);
        p.clear("fold");
        p.clear("last");
        p.clear("quit");
        p.clear("allin");
        p.clear("change");
        p.clear("idled");
    }
    
    /**
     * Assigns players to the dealer, small blind and big blind roles.
     */
    protected void setButton(){
        // dealer will never be set to null
        dealer = (PokerPlayer) getPlayerAfter(dealer);
        if (joined.size() == 2){
            smallBlind = dealer;
        } else {
            smallBlind = (PokerPlayer) getPlayerAfter(dealer);
        }
        bigBlind = (PokerPlayer) getPlayerAfter(smallBlind);
    }
    
    /**
     * Sets the bets for the small and big blinds.
     */
    protected void setBlindBets(){
        // Set the small blind
        if (get("minbet")/2 >= smallBlind.getInteger("cash")) {
            smallBlind.put("allin", true);
            smallBlind.put("bet", smallBlind.getInteger("cash"));
        } else {
            smallBlind.put("bet", get("minbet")/2);
        }
        
        // Set the big blind
        if (get("minbet") >= bigBlind.getInteger("cash")) {
            bigBlind.put("allin", true);
            bigBlind.put("bet", bigBlind.getInteger("cash"));
        } else {
            bigBlind.put("bet", get("minbet"));
        }
        
        // Set the current bet to minbet regardless of actual blinds
        currentBet = get("minbet");
        minRaise = get("minbet");
    }
    
    @Override
    public boolean isInProgress() {
        return !state.equals(PokerState.NONE);
    }
    
    ////////////////////////////////////////
    //// Game initialization management ////
    ////////////////////////////////////////
    
    @Override
    protected void initSettings() {
        // Do not use set()
        // Ini file settings
        settings.put("cash", 1000);
        settings.put("idle", 60);
        settings.put("idlewarning", 45);
        settings.put("respawn", 600);
        settings.put("maxplayers", 22);
        settings.put("minbet", 10);
        settings.put("autostarts", 10);
        settings.put("startwait", 5);
        settings.put("showdown", 10);
        settings.put("revealcommunity", 0);
        settings.put("ping", 600);
    }
    
    @Override
    protected void initCustom(){
        name = "texaspoker";
        helpFile = "texaspoker.help";
        deck = new CardDeck();
        deck.shuffleCards();
        pots = new ArrayList<>();
        community = new Hand();
        
        initSettings();
        loadHelp();
        loadIni();
        state = PokerState.NONE;
        betState = PokerBet.NONE;
        showMsg(getMsg("game_start"), getGameNameStr());
    }
    
    @Override
    protected void saveIniFile() {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(iniFile)))) {
            out.println("#Settings");
            out.println("#Number of seconds before a player idles out");
            out.println("idle=" + get("idle"));
            out.println("#Number of seconds before a player is given a warning for idling");
            out.println("idlewarning=" + get("idlewarning"));
            out.println("#Initial amount given to new and bankrupt players");
            out.println("cash=" + get("cash"));
            out.println("#Number of seconds before a bankrupt player is allowed to join again");
            out.println("respawn=" + get("respawn"));
            out.println("#Minimum bet (big blind), preferably an even number");
            out.println("minbet=" + get("minbet"));
            out.println("#The maximum number of players allowed to join a game");
            out.println("maxplayers=" + get("maxplayers"));
            out.println("#The maximum number of autostarts allowed");
            out.println("autostarts=" + get("autostarts"));
            out.println("#The wait time in seconds after the start command is given");
            out.println("startwait=" + get("startwait"));
            out.println("#The wait time in seconds in between reveals during a showdown");
            out.println("showdown=" + get("showdown"));
            out.println("#Whether or not to reveal community when not required");
            out.println("revealcommunity=" + get("revealcommunity"));
            out.println("#The rate-limit of the ping command");
            out.println("ping=" + get("ping"));
        } catch (IOException e) {
            manager.log("Error creating " + iniFile + "!");
        }
    }
    
    /////////////////////////////////////////
    //// Player stats management methods ////
    /////////////////////////////////////////
    
    @Override
    protected Player loadDBPlayerRecord(String nick) {
        if (isBlacklisted(nick)) {
            return findBlacklisted(nick);
        } else if (isJoined(nick)) {
            return findJoined(nick);
        } else {
            PokerPlayer record = null;
            try (Connection conn = DriverManager.getConnection(dbURL)) {
                conn.setAutoCommit(false);
                // Retrieve data from Player table if possible
                String sql = getSQL("SELECT_TPPLAYERVIEW_BY_NICK");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nick);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.isBeforeFirst()) {
                            record = new PokerPlayer("");
                            record.put("id", rs.getInt("id"));
                            record.put("nick", rs.getString("nick"));
                            record.put("cash", rs.getInt("cash"));
                            record.put("bank", rs.getInt("bank"));
                            record.put("bankrupts", rs.getInt("bankrupts"));
                            record.put("rounds", rs.getInt("rounds"));
                            record.put("winnings", rs.getInt("winnings"));
                            record.put("idles", rs.getInt("idles"));
                        }
                    }
                }
                conn.commit();
                logDBWarning(conn.getWarnings());
            } catch (SQLException ex) {
                manager.log("SQL Error: " + ex.getMessage());
            }
            return record;
        }
    }
    
    @Override
    protected void loadDBPlayerData(Player p) {
        try (Connection conn = DriverManager.getConnection(dbURL)) {
            conn.setAutoCommit(false);
            
            // Initialize
            p.put("id", 0);
            p.put("cash", get("cash"));
            p.put("bank", 0);
            p.put("bankrupts", 0);
            p.put("rounds", 0);
            p.put("winnings", 0);
            p.put("idles", 0);
            
            // Retrieve data from Player table if possible
            String sql = getSQL("SELECT_PLAYER_BY_NICK");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getNick());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.isBeforeFirst()) {
                        p.put("id", rs.getInt("id"));
                    }
                }
            }
            
            // Add new record if not found in Player table
            if (!p.has("id")) {
                sql = getSQL("INSERT_PLAYER");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, p.getNick());
                    ps.setLong(2, System.currentTimeMillis() / 1000);
                    ps.executeUpdate();
                    p.put("id", ps.getGeneratedKeys().getInt(1));
                }
            }
            
            // Retrieve data from Purse table if possible
            boolean found = false;
            sql = getSQL("SELECT_PURSE_BY_PLAYER_ID");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, p.getInteger("id"));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.isBeforeFirst()) {
                        found = true;
                        p.put("cash", rs.getInt("cash"));
                        p.put("bank", rs.getInt("bank"));
                        p.put("bankrupts", rs.getInt("bankrupts"));
                    }
                }
            }
            
            // Add new record if not found in Purse
            if (!found) {
                informPlayer(p.getNick(), getMsg("new_player"), getGameNameStr(), get("cash"));
                sql = getSQL("INSERT_PURSE");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, p.getInteger("id"));
                    ps.setInt(2, p.getInteger("cash"));
                    ps.setInt(3, p.getInteger("bank"));
                    ps.setInt(4, p.getInteger("bankrupts"));
                    ps.executeUpdate();
                }
            }

            // Retrieve data from TPPlayerStat table if possible
            found = false;
            sql = getSQL("SELECT_TPPLAYERSTAT_BY_PLAYER_ID");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, p.getInteger("id"));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.isBeforeFirst()) {
                        found = true;
                        p.put("rounds", rs.getInt("rounds"));
                        p.put("winnings", rs.getInt("winnings"));
                        p.put("idles", rs.getInt("idles"));
                    }
                }
            }
            
            // Add new record if not found in TPPlayerStat table
            if (!found) {
                sql = getSQL("INSERT_TPPLAYERSTAT");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, p.getInteger("id"));
                    ps.setInt(2, p.getInteger("rounds"));
                    ps.setInt(3, p.getInteger("winnings"));
                    ps.setInt(4, p.getInteger("idles"));
                    ps.executeUpdate();
                }
            }
            
            conn.commit();
            logDBWarning(conn.getWarnings());
        } catch (SQLException ex) {
            manager.log("SQL Error: " + ex.getMessage());
        }
    }
    
    @Override
    protected void saveDBPlayerDataBatch(ArrayList<Player> players) {        
        try (Connection conn = DriverManager.getConnection(dbURL)) {
            conn.setAutoCommit(false);
            
            for (Player p : players) {
                // Update data in Purse table
                String sql = getSQL("UPDATE_PURSE");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, p.getInteger("cash"));
                    ps.setInt(2, p.getInteger("bank"));
                    ps.setInt(3, p.getInteger("bankrupts"));
                    ps.setInt(4, p.getInteger("id"));
                    ps.executeUpdate();
                }

                // Update data in TPPlayerStat table
                sql = getSQL("UPDATE_TPPLAYERSTAT");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, p.getInteger("rounds"));
                    ps.setInt(2, p.getInteger("winnings"));
                    ps.setInt(3, p.getInteger("idles"));
                    ps.setInt(4, p.getInteger("id"));
                    ps.executeUpdate();
                }
            }
            
            conn.commit();
            logDBWarning(conn.getWarnings());
        } catch (SQLException ex) {
            manager.log("SQL Error: " + ex.getMessage());
        }
    }
    
    ///////////////////////////////
    //// Game stats management ////
    ///////////////////////////////
    
    /**
     * Returns the total stats for the game.
     * @return a record containing queried stats
     */
    private Record loadDBGameTotals() {
        Record record = new Record();
        record.put("total_players", 0);
        record.put("total_rounds", 0);
        
        try (Connection conn = DriverManager.getConnection(dbURL)) {            
            String sql = getSQL("SELECT_TPGAMETOTALS");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.isBeforeFirst()) {
                        record.put("total_players", rs.getInt("total_players"));
                        record.put("total_rounds", rs.getInt("total_rounds"));
                    }
                }
            }
            logDBWarning(conn.getWarnings());
        } catch (SQLException ex) {
            manager.log("SQL Error: " + ex.getMessage());
        }
        return record;
    }
    
    @Override
    protected void saveDBGameStats() {
        int roundID, handID, potID;
        try (Connection conn = DriverManager.getConnection(dbURL)) {
            conn.setAutoCommit(false);
            
            // Insert data into TPRound table
            String sql = getSQL("INSERT_TPROUND");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, startTime);
                ps.setLong(2, endTime);
                ps.setString(3, channel.getName());
                ps.setString(4, community.toStringDB());
                ps.executeUpdate();
                roundID = ps.getGeneratedKeys().getInt(1);
            }
            
            
            for (Player p : joined) {
                // Insert data into TPPlayerChange table
                sql = getSQL("INSERT_TPPLAYERCHANGE");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, p.getInteger("id"));
                    ps.setInt(2, roundID);
                    ps.setInt(3, p.getInteger("change"));
                    ps.setInt(4, p.getInteger("cash"));
                    ps.executeUpdate();
                }
                
                // Insert data into TPHand table
                sql = getSQL("INSERT_TPHAND");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, roundID);
                    ps.setString(2, ((PokerPlayer) p).getHand().toStringDB());
                    ps.executeUpdate();
                    handID = ps.getGeneratedKeys().getInt(1);
                }
                
                // Insert data into TPPlayerHand table
                sql = getSQL("INSERT_TPPLAYERHAND");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, p.getInteger("id"));
                    ps.setInt(2, handID);
                    ps.setBoolean(3, p.getBoolean("fold"));
                    ps.setBoolean(4, p.getBoolean("allin"));
                    ps.executeUpdate();
                }
                
                if (p.getBoolean("idled")) {
                    // Insert data into TPPlayerIdle table
                    sql = getSQL("INSERT_TPPLAYERIDLE");
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, p.getInteger("id"));
                        ps.setInt(2, roundID);
                        ps.setInt(3, get("idle"));
                        ps.setInt(4, get("idlewarning"));
                        ps.executeUpdate();
                    }
                }
            }
            
            for (PokerPot pot : pots) {
                // Insert data into TPPot table
                sql = getSQL("INSERT_TPPOT");
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, roundID);
                    ps.setInt(2, pot.getTotal());
                    ps.executeUpdate();
                    potID = ps.getGeneratedKeys().getInt(1);
                }
                
                // Insert data into TPPlayerPot table
                for (PokerPlayer p : pot.getDonors()) {
                    sql = getSQL("INSERT_TPPLAYERPOT");
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, p.getInteger("id"));
                        ps.setInt(2, potID);
                        ps.setInt(3, pot.getContribution(p));
                        ps.setBoolean(4, pot.isWinner(p));
                        ps.executeUpdate();
                    }
                }
            }
            
            conn.commit();
            logDBWarning(conn.getWarnings());
        } catch (SQLException ex) {
            manager.log("SQL Error: " + ex.getMessage());
        }
    }
    
    /////////////////////////////////////////////////////////
    //// Card management methods for Texas Hold'em Poker ////
    /////////////////////////////////////////////////////////
    /**
     * Deals cards to the community hand.
     */
    protected void dealCommunity(){
        if (betState.equals(PokerBet.PRE_FLOP)) {
            for (int ctr = 1; ctr <= 3; ctr++){
                dealCard(community);
            }
        } else {
            dealCard(community);
        }
    }
    
    /**
     * Deals two cards to the specified player.
     * @param p the player to be dealt to
     */
    protected void dealHand(PokerPlayer p) {
        dealCard(p.getHand());
        dealCard(p.getHand());
    }
    
    /**
     * Deals hands to everybody at the table.
     */
    protected void dealTable() {
        PokerPlayer p;
        for (int ctr = 0; ctr < joined.size(); ctr++) {
            p = (PokerPlayer) joined.get(ctr);
            dealHand(p);
            informPlayer(p.getNick(), getMsg("tp_hand"), p.getHand());
        }
    }
    
    /**
     * Discards a player's hand into the discard pile.
     * @param p the player whose hand is to be discarded
     */
    protected void discardPlayerHand(PokerPlayer p) {
        if (p.hasHand()) {
            deck.addToDiscard(p.getHand());
            p.resetHand();
        }
    }
    
    /**
     * Discards the community cards into the discard pile.
     */
    protected void discardCommunity(){
        if (!community.isEmpty()){
            deck.addToDiscard(community);
            community.clear();
        }
    }
    
    /**
     * Merges the discards and shuffles the deck.
     */
    protected void shuffleDeck() {
        deck.refillDeck();
        showMsg(getMsg("tp_shuffle_deck"));
    }
    
    //////////////////////////////////////////////
    //// Texas Hold'em Poker gameplay methods ////
    //////////////////////////////////////////////
    
    /**
     * Processes a bet command.
     * @param amount the amount to bet
     */
    public void bet(int amount) {
        cancelIdleOutTask();
        PokerPlayer p = (PokerPlayer) currentPlayer;
        
        // A bet that's an all-in (takes precedence)
        if (amount == p.getInteger("cash")){
            if (amount > currentBet || topBettor == null){
                topBettor = p;
            }
            if (amount - currentBet > minRaise){
                minRaise = amount - currentBet;
            }
            if (amount > currentBet) {
                currentBet = amount;
            }
            p.put("bet", amount);
            p.put("allin", true);
            continueRound();
        // A bet that's larger than a player's stack
        } else if (amount > p.getInteger("cash")) {
            informPlayer(p.getNick(), getMsg("insufficient_funds"));
            setIdleOutTask();
        // A bet that's lower than the current bet
        } else if (amount < currentBet) {
            informPlayer(p.getNick(), getMsg("tp_bet_too_low"), currentBet);
            setIdleOutTask();
        // A bet that's equivalent to a call or check
        } else if (amount == currentBet){
            if (topBettor == null){
                topBettor = p;
            }
            p.put("bet", amount);
            continueRound();
        // A bet that's lower than the minimum raise
        } else if (amount - currentBet < minRaise){
            informPlayer(p.getNick(), getMsg("raise_too_low"), minRaise);
            setIdleOutTask();
        // A valid bet that's greater than the currentBet
        } else {
            p.put("bet", amount);
            topBettor = p;
            minRaise = amount - currentBet;
            currentBet = amount;
            continueRound();
        }
    }
    
    /**
     * Processes a check command.
     * Only allow a player to check when the currentBet is 0 or if the player
     * has already committed the required amount to pot.
     */
    public void check(){
        cancelIdleOutTask();
        PokerPlayer p = (PokerPlayer) currentPlayer;
        
        if (currentBet == 0 || p.getInteger("bet") == currentBet){
            if (topBettor == null){
                topBettor = p;
            }
            continueRound();
        } else {
            informPlayer(p.getNick(), getMsg("no_checking"), currentBet);
            setIdleOutTask();
        }
    }
    
    /**
     * Processes a call command.
     * A player's bet will be matched to the currentBet. If a player's stack
     * is less than the currentBet, the player will move all-in.
     */
    public void call(){
        cancelIdleOutTask();
        PokerPlayer p = (PokerPlayer) currentPlayer;
        int total = Math.min(p.getInteger("cash"), currentBet);
        
        if (topBettor == null){
            topBettor = p;
        }
        if (total == p.getInteger("cash")){
            p.put("allin", true);
        }
        
        p.put("bet", total);
        continueRound();
    }
    
    /**
     * Process a fold command.
     * The folding player is removed from all pots.
     */
    public void fold(){
        cancelIdleOutTask();
        PokerPlayer p = (PokerPlayer) currentPlayer;
        p.put("fold", true);

        //Remove this player from any existing pots
        if (currentPot != null && currentPot.isEligible(p)){
            currentPot.disqualify(p);
        }
        for (PokerPot pot : pots) {
            if (pot.isEligible(p)){
                pot.disqualify(p);
            }
        }
        continueRound();
    }
    
    ///////////////////////////////////
    //// Behind the scenes methods ////
    ///////////////////////////////////
    
    /**
     * Determines the number of players who have not folded.
     * @return the number of non-folded players
     */
    protected int getNumberNotFolded(){
        int numberNotFolded = 0;
        for (Player p : joined) {
            if (!p.has("fold")){
                numberNotFolded++;
            }
        }
        return numberNotFolded;
    }
    
    /**
     * Determines the number players who can still make a bet.
     * @return the number of players who can bet
     */
    protected int getNumberCanBet(){
        int numberCanBet = 0;
        for (Player p : joined) {
            if (!p.has("fold") && !p.has("allin")){
                numberCanBet++;
            }
        }
        return numberCanBet;
    }
    
    /**
     * Determines the number of players who have made a bet in a round of 
     * betting.
     * @return the number of bettors
     */
    protected int getNumberBettors() {
        int numberBettors = 0;
        for (Player p : joined) {
            if (p.has("bet")){
                numberBettors++;
            }
        }
        return numberBettors;
    }
    
    /**
     * Determines total amount committed by all players.
     * @return the total running amount 
     */
    protected int getCashInPlay() {
        int total = 0;
        
        // Add in the processed pots
        for (PokerPot pp : pots) {
            total += pp.getTotal();
        }
        
        // Add in the amounts currently being betted
        for (Player p : joined) {
            total += p.getInteger("bet");
        }
        
        return total;
    }
    
    /**
     * Adds the bets during a round of betting to the pot.
     * If no pot exists, a new one is created. Sidepots are created as necessary.
     */
    protected void addBetsToPot(){
        int lowBet;
        while(currentBet != 0){
            lowBet = currentBet;
            // Only add bets to a pot if more than one player has made a bet
            if (getNumberBettors() > 1) {
                // Create a new pot if currentPot is set to null
                if (currentPot == null){
                    currentPot = new PokerPot();
                    pots.add(currentPot);
                } else {
                    // Determine if anybody in the current pot has no more bet
                    // left to contribute but is still in the game. If so, 
                    // then a new pot will be required.
                    for (PokerPlayer p : currentPot.getEligibles()) {
                        if (!p.has("bet") && currentBet != 0 && !p.has("fold")) {
                            currentPot = new PokerPot();
                            pots.add(currentPot);
                            break;
                        }
                    }
                }

                // Determine the lowest non-zero bet
                for (Player p : joined) {
                    if (p.getInteger("bet") < lowBet && p.has("bet")){
                        lowBet = p.getInteger("bet");
                    }
                }
                
                // Subtract lowBet from each player's (non-zero) bet and add to pot.
                for (Player p : joined) {
                    if (p.has("bet")){
                        // Add a lowBet pot contribution for the player
                        currentPot.contribute((PokerPlayer) p, lowBet);
                        if (p.has("fold")) {
                            currentPot.disqualify((PokerPlayer) p);
                        }
                        p.add("cash", -1 * lowBet);
                        p.add("winnings", -1 * lowBet);
                        p.add("bet", -1 * lowBet);
                        p.add("change", -1 * lowBet);
                    }
                }
                // Update currentbet
                currentBet -= lowBet;
            
            // If only one player has any bet left then it should not be
            // contributed to the current pot and his bet and currentBet should
            // be reset.
            } else {
                for (Player p : joined) {
                    if (p.getInteger("bet") != 0){
                        p.clear("bet");
                        break;
                    }
                }
                currentBet = 0;
                break;
            }
        }
    }
    
    ////////////////////////////////////////////////////////
    //// Message output methods for Texas Hold'em Poker ////
    ////////////////////////////////////////////////////////
    
    /**
     * Displays the players who are involved in a round. Players who have not
     * folded are displayed in bold. Designations for small blind, big blind,
     * and dealer are also shown.
     */
    public void showTablePlayers(){
        String msg = formatBold(joined.size()) + " players: ";
        for (Player p : joined) {
            // Give bold to remaining non-folded players
            String nickColor = "";
            if (!p.has("fold")){
                nickColor = Colors.BOLD;
            }
            msg += nickColor + p.getNick();

            // Give special players a label
            if (p == dealer || p == smallBlind || p == bigBlind){
                msg += "(";
                if (p == dealer){
                    msg += "D";
                }
                if (p == smallBlind){
                    msg += "S";
                } else if (p == bigBlind){
                    msg += "B";
                }
                msg += ")";
            }
            msg += nickColor + ", ";
        }
        showMsg(msg.substring(0, msg.length() - 2));
    }
    
    /**
     * Displays info on the dealer and blinds.
     */
    public void showButtonInfo() {
        showMsg(getMsg("tp_button_info"), dealer.getNickStr(false), smallBlind.getNickStr(false), smallBlind.get("bet"), bigBlind.getNickStr(false), bigBlind.get("bet"));
    }
    
    /**
     * Displays the community cards along with existing pots.
     * @param plainTitle
     */
    public void showCommunityCards(boolean plainTitle){
        String msg = "";
        
        // Append community cards to String
        if (plainTitle && betState.equals(PokerBet.RIVER)) {
            msg += formatHeader(" Community: ") + " " + community.toString() + " ";
        } else if (betState.equals(PokerBet.FLOP)) {
            msg += formatHeader(" Flop: ") + " " + community.toString() + " ";
        } else if (betState.equals(PokerBet.TURN)) {
            msg += formatHeader(" Turn: ") + " " + community.toString() + " ";
        } else if (betState.equals(PokerBet.RIVER)) {
            msg += formatHeader(" River: ") + " " + community.toString() + " ";
        } else {
            showMsg(getMsg("tp_no_community"));
            return;
        }
        
        // Append existing pots to String
        for (int ctr = 0; ctr < pots.size(); ctr++){
            msg += Colors.YELLOW+",01 Pot #"+(ctr+1)+": "+Colors.GREEN+",01$"+formatNumber(pots.get(ctr).getTotal())+" "+Colors.NORMAL+" ";
        }
        
        // Append remaining non-folded players
        int notFolded = getNumberNotFolded();
        String pstr = "(" + formatBold(notFolded) + " players: ";
        for (Player p : joined) {
            if (!p.has("fold")){
                pstr += p.getNick(false) + ", ";
            }
        }
        msg += pstr.substring(0, pstr.length() - 2) + ")";
        
        showMsg(msg);
    }

    /**
     * Displays a list of players in a showdown.
     * @param list
     * @param sim 
     */
    public void showShowdown(ArrayList<PokerPlayer> list, PokerSimulator sim) {
        String showdownStr = formatHeader(" Showdown: ") + " ";
        for (PokerPlayer p : list) {
            showdownStr += p.getNickStr() + " (" + p.getHand() + "||" + formatBold(Math.round(sim.getWinPct(p)) + "/" + Math.round(sim.getTiePct(p)) + "%%") + "), ";
        }
        showMsg(showdownStr.substring(0, showdownStr.length()-2));
    }
    
    /**
     * Displays the results of a round.
     */
    public void showResults(){
        // Show introduction to end results
        showMsg(formatHeader(" Results: "));
        
        // Show each remaining player's hand if more than one player unfolded
        if (getNumberNotFolded() > 1){
            ArrayList<PokerPlayer> players = pots.get(0).getEligibles();
            Collections.sort(players);
            Collections.reverse(players);
            for (PokerPlayer p : players) {
                showMsg(getMsg("tp_player_result"), p.getNickStr(false), p.getHand(), p.getPokerHand().getName(), p.getPokerHand());
            }
        }
        
        // Find the winner(s) from each pot
        for (int ctr = 0; ctr < pots.size(); ctr++){
            currentPot = pots.get(ctr);
            ArrayList<PokerPlayer> players = currentPot.getEligibles();
            Collections.sort(players);
            Collections.reverse(players);
            
            int winners = 1;
            int potTotal = currentPot.getTotal();
            
            // Determine number of winners
            currentPot.setWinner(players.get(0));
            for (int ctr2 = 1; ctr2 < players.size(); ctr2++){
                if (players.get(0).compareTo(players.get(ctr2)) == 0){
                    currentPot.setWinner(players.get(ctr2));
                    winners++;
                }
            }
            
            // Output winners
            for (PokerPlayer p : currentPot.getWinners()) {
                p.add("cash", potTotal/winners);
                p.add("winnings", potTotal/winners);
                p.add("change", potTotal/winners);
                showMsg(Colors.YELLOW + ",01 Pot #" + (ctr+1) + ": " + 
                        Colors.NORMAL + " " + p.getNickStr() + " wins $" + 
                        formatNumber(potTotal/winners) + ". (" + 
                        getPlayerListString(players) + ")");
            }
        }
    }

    @Override
    public void showPlayerRank(String nick, String stat) throws IllegalArgumentException {
        String statName = "";
        String line = Colors.BLACK + ",08";
        String sql;
        
        // Build SQL query
        if (stat.equals("cash")) {
            sql = getSQL("SELECT_RANK_CASH_BY_NICK");
            statName = "cash";
            line += "Cash: ";
        } else if (stat.equalsIgnoreCase("bank")) {
            sql = getSQL("SELECT_RANK_BANK_BY_NICK");
            statName = "bank";
            line += "Bank: ";
        } else if (stat.equalsIgnoreCase("bankrupts")) {
            sql = getSQL("SELECT_RANK_BANKRUPTS_BY_NICK");
            statName = "bankrupts";
            line += "Bankrupts: ";
        } else if (stat.equalsIgnoreCase("net") || stat.equals("netcash")) {
            sql = getSQL("SELECT_RANK_NETCASH_BY_NICK");
            statName = "netcash";
            line += "Net Cash: ";
        } else if (stat.equalsIgnoreCase("rounds")) {
            sql = getSQL("SELECT_RANK_TPROUNDS_BY_NICK");
            statName = "rounds";
            line += "Texas Hold'em Rounds (min. 1 round): ";
        } else if (stat.equalsIgnoreCase("winnings")) {
            sql = getSQL("SELECT_RANK_TPWINNINGS_BY_NICK");
            statName = "winnings";
            line += "Texas Hold'em Winnings (min. 1 round): ";
        } else if (stat.equalsIgnoreCase("winrate")) {
            sql = getSQL("SELECT_RANK_TPWINRATE_BY_NICK");
            statName = "winrate";
            line += "Texas Hold'em Win Rate (min. 50 rounds): ";
        } else {
            throw new IllegalArgumentException();
        }
        
        try (Connection conn = DriverManager.getConnection(dbURL)) {
            // Retrieve data from DB if possible
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nick);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.isBeforeFirst()) {
                        line += "#" + rs.getInt("rank") + " " + Colors.WHITE + ",04 " + formatNoPing(rs.getString("nick"));
                        if (statName.equals("winrate")){
                            if (rs.getInt("rounds") < 50) {
                                line = String.format("%s (%d) has not played enough rounds of %s. A minimum of 50 rounds must be played to qualify for a win rate ranking.", formatNoPing(rs.getString("nick")), rs.getInt("rounds"), getGameNameStr());
                            } else {
                                line += " $" + formatDecimal(rs.getDouble(statName));
                            }
                        } else if (statName.equals("rounds")) {
                            if (rs.getInt("rounds") == 0) {
                                line = String.format(getMsg("player_no_rounds"), formatNoPing(rs.getString("nick")), getGameNameStr());
                            } else {
                                line += " " + formatNumber(rs.getInt(statName));
                            }
                        } else if (statName.equals("winnings")) {
                            if (rs.getInt("rounds") == 0) {
                                line = String.format(getMsg("player_no_rounds"), formatNoPing(rs.getString("nick")), getGameNameStr());
                            } else {
                                line += " $" + formatNumber(rs.getInt(statName));
                            }
                        } else if (statName.equals("bankrupts")) {
                            line += " " + formatNumber(rs.getInt(statName));
                        } else {
                            line += " $" + formatNumber(rs.getInt(statName));
                        }
                        
                        // Show rank
                        showMsg(line);
                    } else {
                        showMsg(getMsg("no_data"), formatNoPing(nick));
                    }
                }
            }
            logDBWarning(conn.getWarnings());
        } catch (SQLException ex) {
            manager.log("SQL Error: " + ex.getMessage());
        }
    }
    
    @Override
    public void showTopPlayers(String stat, int n) throws IllegalArgumentException {
        if (n < 1){
            throw new IllegalArgumentException();
        }
        
        String title = Colors.BOLD + Colors.BLACK + ",08 Top %,d-%,d";
        String list = Colors.BLACK + ",08";
        String statName = "";
        String sql = "";
        String sqlBounds = "";
        
        if (stat.equalsIgnoreCase("cash")) {
            sqlBounds = getSQL("SELECT_TOP_BOUNDS_PURSE");
            sql = getSQL("SELECT_TOP_CASH");
            statName = "cash";
            title += " Cash ";
        } else if (stat.equalsIgnoreCase("bank")) {
            sqlBounds = getSQL("SELECT_TOP_BOUNDS_PURSE");
            sql = getSQL("SELECT_TOP_BANK");
            statName = "bank";
            title += " Bank ";
        } else if (stat.equalsIgnoreCase("bankrupts")) {
            sqlBounds = getSQL("SELECT_TOP_BOUNDS_PURSE");
            sql = getSQL("SELECT_TOP_BANKRUPTS");
            statName = "bankrupts";
            title += " Bankrupts ";
        } else if (stat.equalsIgnoreCase("net") || stat.equalsIgnoreCase("netcash")) {
            sqlBounds = getSQL("SELECT_TOP_BOUNDS_PURSE");
            sql = getSQL("SELECT_TOP_NETCASH");
            statName = "netcash";
            title += " Net Cash ";
        } else if (stat.equalsIgnoreCase("winnings")){
            sqlBounds = getSQL("SELECT_TOP_BOUNDS_TPWINNINGS");
            sql = getSQL("SELECT_TOP_TPWINNINGS");
            statName = "winnings";
            title += " Texas Hold'em Winnings (min. 1 round) ";
        } else if (stat.equalsIgnoreCase("rounds")) {
            sqlBounds = getSQL("SELECT_TOP_BOUNDS_TPROUNDS");
            sql = getSQL("SELECT_TOP_TPROUNDS");
            statName = "rounds";
            title += " Texas Hold'em Rounds (min. 1 round) ";
        } else if (stat.equalsIgnoreCase("winrate")) {
            sqlBounds = getSQL("SELECT_TOP_BOUNDS_TPWINRATE");
            sql = getSQL("SELECT_TOP_TPWINRATE");
            statName = "winrate";
            title += " Texas Hold'em Win Rate (min. 50 rounds) ";
        } else {
            throw new IllegalArgumentException();
        }
        
        try (Connection conn = DriverManager.getConnection(dbURL)) {
            conn.setAutoCommit(false);
            int limit = 10;
            int offset = 0;
            
            // Retrieve offset
            try (PreparedStatement ps = conn.prepareStatement(sqlBounds)) {
                ps.setInt(1, n);
                ps.setInt(2, n);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.isBeforeFirst()) {
                        limit = rs.getInt("top_limit");
                        offset = rs.getInt("top_offset");
                    }
                }
            }
            
            title = String.format(title, offset+1, offset+limit);
            
            // Retrieve data from DB if possible
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.isBeforeFirst()) {
                        int ctr = offset + 1;
                        // Add the players in the required range
                        while (rs.next()) {
                            list += " #" + ctr++ + ": " + Colors.WHITE + ",04 ";
                            if (statName.equals("winrate")) {
                                list += formatNoPing(rs.getString("nick")) + " $" + formatDecimal(rs.getDouble(statName));
                            } else if (statName.equals("rounds") || statName.equals("bankrupts")) {
                                list += formatNoPing(rs.getString("nick")) + " " + formatNumber(rs.getInt(statName));
                            } else {
                                list += formatNoPing(rs.getString("nick")) + " $" + formatNumber(rs.getInt(statName));
                            }
                            list += " " + Colors.BLACK + ",08";
                        }
                        
                        // Output title and the list
                        showMsg(title);
                        showMsg(list);
                    } else {
                        showMsg("No %s data for %s.", statName, getGameNameStr());
                    }
                }
            }
            conn.commit();
            logDBWarning(conn.getWarnings());
        } catch (SQLException ex) {
            manager.log("SQL Error: " + ex.getMessage());
        }
    }
    
    /**
     * Displays the stack of each player in the given list in descending order.
     */
    public void showStacks() {
        ArrayList<Player> list = new ArrayList<>(joined);
        String msg = Colors.YELLOW + ",01 Stacks: " + Colors.NORMAL + " ";
        Collections.sort(list, Player.getComparator("cash"));
        
        for (Player p : list) {
            msg += p.getNick(false) + " (" + formatBold("$" + formatNumber(p.getInteger("cash")));
            // Add player stack change
            if (p.getInteger("change") > 0) {
                msg += "[" + Colors.DARK_GREEN + Colors.BOLD + "$" + formatNumber(p.getInteger("change")) + Colors.NORMAL + "]";
            } else if (p.getInteger("change") < 0) {
                msg += "[" + Colors.RED + Colors.BOLD + "$" + formatNumber(p.getInteger("change")) + Colors.NORMAL + "]";
            } else {
                msg += "[" + Colors.BOLD + "$" + formatNumber(p.getInteger("change")) + Colors.NORMAL + "]";
            }
            msg += "), ";
        }
        
        showMsg(msg.substring(0, msg.length()-2));
    }
    
    ///////////////////////////
    //// Formatted strings ////
    ///////////////////////////
    
    @Override
    public String getGameNameStr() {
        return formatBold(getMsg("tp_game_name"));
    }
    
    @Override
    public String getGameRulesStr() {
        return String.format(getMsg("tp_rules"), get("minbet")/2, get("minbet"));
    }
    
    @Override
    public String getGameStatsStr() {
        Record record = loadDBGameTotals();
        return String.format(getMsg("tp_stats"), record.get("total_players"), record.get("total_rounds"), getGameNameStr());
    }
}

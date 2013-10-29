/*
    Copyright (C) 2013 Yizhe Shen <brrr@live.ca>

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

package irccasino;

import org.pircbotx.Colors;

/**
 * An object that represents a card. 
 * @author Yizhe Shen
 */
public class Card implements Comparable<Card>{
    /** The card's suit. */
    private String suit;
    /** The card's face. */
    private String face;
    
    /**
     * Creates a new Card with suit and face.
     * 
     * @param s Card suit.
     * @param f Card face.
     */
    public Card(String s, String f){
        suit = s;
        face = f;
    }
    
    /* Accessor methods */
    /**
     * Returns the card's face.
     * 
     * @return the card's face
     */
    public String getFace(){
        return face;
    }
    
    /**
     * Returns the card's suit.
     * 
     * @return the card's suit
     */
    public String getSuit(){
        return suit;
    }
    
    /**
     * Returns the blackjack value of this card.
     * 
     * @return 10 for face card, 11 for ace or the parsed Integer of the face
     */
    public int getBlackjackValue() {
        int num;
        if (face.equals("A")){
            num = 11; // Give aces a default value of 11
        } else {
            try {
                num = Integer.parseInt(getFace());
            } catch (NumberFormatException e) {
                num = 10;
            }
        }
        return num;
    }
    
    /**
     * Returns the index in the static array CardDeck.faces that matches this
     * card's face.
     * 
     * @return the index or -1 if not found
     */
    public int getFaceValue(){
        for (int ctr=0; ctr < CardDeck.faces.length; ctr++){
            if (face.equals(CardDeck.faces[ctr])){
                return ctr;
            }
        }
        return -1;
    }
    
    /**
     * Returns the index in the static array CardDeck.suits that matches this
     * card's suit.
     * 
     * @return the index or -1 if not found
     */
    public int getSuitValue(){
        for (int ctr=0; ctr < CardDeck.suits.length; ctr++){
            if (suit.equals(CardDeck.suits[ctr])){
                return ctr;
            }
        }
        return -1;
    }
    
    /** 
     * Compares this Card to another by face value, then by suit value. 
     * 
     * @param c the Card to compare
     * @return -1 if face value is less or if equal, suit value is less, 0 if
     * suit value and face value are equal, 1 if face value is greater or if 
     * equal, suit value is greater
     */
    @Override
    public int compareTo(Card c){
        if (c == null) {
            throw new NullPointerException();
        }
        int valueDiff = getFaceValue() - c.getFaceValue();
        int suitDiff = getSuitValue() - c.getSuitValue();
        if (valueDiff == 0){
            return suitDiff;
        } else {
            return valueDiff;
        }
    }
    
    /**
     * String representation of the card with IRC color formatting.
     * Gives the card a colour based on suit and adds a white background.
     * 
     * @return a IRC colour formatted string with the card's face and suit
     */
    @Override
    public String toString(){
        String color;
        if (suit.equals(CardDeck.suits[0])){
            color = Colors.RED;
        } else if ( suit.equals(CardDeck.suits[1])){
            color = Colors.BROWN;
        } else if ( suit.equals(CardDeck.suits[2])){
            color = Colors.DARK_BLUE;
        } else {
            color = Colors.BLACK;
        }
        
        return color+",00"+face+suit;
    }
}
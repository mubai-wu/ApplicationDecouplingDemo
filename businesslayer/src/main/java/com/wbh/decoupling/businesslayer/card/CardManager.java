package com.wbh.decoupling.businesslayer.card;

import java.util.List;

public class CardManager {

    public static void registerCard(ICard card) {
        CardRegister.getInstance().register(card);
    }

    public static List<ICard> getAllCard() {
        return CardRegister.getInstance().getCardList();
    }
}

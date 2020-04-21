package com.wbh.decoupling.businesslayer.card;

import java.util.ArrayList;
import java.util.List;

class CardRegister {

    private List<ICard> mCardList;

    private CardRegister() {
        mCardList = new ArrayList<>();
    }

    static CardRegister getInstance() {
        return InternalHolder.sInstance;
    }

    private static class InternalHolder {
        final static CardRegister sInstance = new CardRegister();
    }

    void register(ICard card) {
        mCardList.add(card);
    }

    List<ICard> getCardList() {
        return mCardList;
    }
}

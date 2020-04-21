package com.wbh.decoupling.businessexp.card;

import com.wbh.decoupling.annotation.Register;
import com.wbh.decoupling.businesslayer.card.ICard;

@Register
public class ExpACard implements ICard {
    @Override
    public String getCardName() {
        return "ExpACard";
    }
}

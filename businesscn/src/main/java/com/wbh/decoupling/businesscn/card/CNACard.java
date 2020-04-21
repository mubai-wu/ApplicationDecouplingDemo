package com.wbh.decoupling.businesscn.card;

import com.wbh.decoupling.annotation.Register;
import com.wbh.decoupling.businesslayer.card.ICard;

@Register
public class CNACard implements ICard {
    @Override
    public String getCardName() {
        return "CNACard";
    }
}

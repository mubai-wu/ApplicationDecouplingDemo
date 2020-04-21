package com.wbh.decoupling.businesslayer.card;

import com.wbh.decoupling.annotation.Register;

@Register
public class BCard implements ICard {
    @Override
    public String getCardName() {
        return "BCard";
    }
}

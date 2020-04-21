package com.wbh.decoupling.businesslayer.card;

import com.wbh.decoupling.annotation.Register;

@Register
public class ACard implements ICard {
    @Override
    public String getCardName() {
        return "ACard";
    }
}

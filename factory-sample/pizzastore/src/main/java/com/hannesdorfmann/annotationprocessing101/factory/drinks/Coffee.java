package com.hannesdorfmann.annotationprocessing101.factory.drinks;

import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;

@Factory(
    id = "Coffee",
    type = Drink.class
)
public class Coffee implements Drink {
    @Override
    public float getPrice() {
        return 2.5f;
    }

    @Override
    public float getAmountInMl() {
        return 200f;
    }
}

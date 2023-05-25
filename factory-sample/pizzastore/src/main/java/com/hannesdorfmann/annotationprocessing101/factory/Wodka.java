package com.hannesdorfmann.annotationprocessing101.factory;

import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;

@Factory(
    id = "Wodka",
    type = Drink.class
)
public class Wodka implements Drink {
    @Override
    public float getPrice() {
        return 6.45f;
    }

    @Override
    public float getAmountInMl() {
        return 25f;
    }
}

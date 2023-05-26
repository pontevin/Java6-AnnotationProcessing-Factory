/*
 * Copyright (C) 2015 Hannes Dorfmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hannesdorfmann.annotationprocessing101.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Hannes Dorfmann
 */
public class PizzaStore {

  private final MealFactory mealFactory = new MealFactory();
  private final DrinkFactory drinkFactory = new DrinkFactory();

  public Meal orderMeal(String mealName) {
    return mealFactory.create(mealName);
  }
  
  public Drink orderDrink(String drinkName) {
    return drinkFactory.create(drinkName);
  }

  private static String readConsole(String what) throws IOException {
    System.out.println("What do you like to " + what);
    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
    String input = bufferRead.readLine();
    return input;
  }

  public static void main(String[] args) throws IOException {
    PizzaStore pizzaStore = new PizzaStore();
    Meal meal = pizzaStore.orderMeal(readConsole("eat"));
    Drink drink = pizzaStore.orderDrink(readConsole("drink"));
    System.out.println("--- Bill ---");
    String veggy = meal.isVegetarian() ? " - vegetarian" : "";
    System.out.println("Meal: $" + meal.getPrice() + veggy);
    System.out.println("Drink: $" + drink.getPrice() + "(" + drink.getAmountInMl() + "ml)");
  }
}

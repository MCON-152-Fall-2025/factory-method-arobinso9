package com.mcon152.recipeshare;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {

    @Test
    void testCreateRecipe() {
        Recipe recipe = new Recipe(1L, "Cake", "Delicious cake", "Flour, Sugar, Eggs", "Mix and bake", 8);
        assertEquals(1L, recipe.getId());
        assertEquals("Cake", recipe.getTitle());
        assertEquals("Delicious cake", recipe.getDescription());
        assertEquals("Flour, Sugar, Eggs", recipe.getIngredients());
        assertEquals("Mix and bake", recipe.getInstructions());
        assertEquals(Integer.valueOf(8), recipe.getServings());
    }

    @Test
    void testSubtypeInstances_andFields() {
        BasicRecipe basic = new BasicRecipe(2L, "Toast", "Buttery toast", "Bread, Butter", "Toast bread and spread butter", 1);
        VegetarianRecipe veg = new VegetarianRecipe(3L, "Veg Salad", "Fresh salad", "Lettuce, Tomato", "Toss", 2);
        DessertRecipe dessert = new DessertRecipe(4L, "Ice Cream", "Vanilla", "Milk, Sugar", "Freeze", 4);
        DairyRecipe dairy = new DairyRecipe(5L, "Cheese Plate", "Assorted cheeses", "Cheeses", "Arrange on plate", 3);

        assertInstanceOf(BasicRecipe.class, basic);
        assertInstanceOf(VegetarianRecipe.class, veg);
        assertInstanceOf(DessertRecipe.class, dessert);
        assertInstanceOf(DairyRecipe.class, dairy);

        assertEquals("Toast", basic.getTitle());
        assertEquals("Veg Salad", veg.getTitle());
        assertEquals("Ice Cream", dessert.getTitle());
        assertEquals(Integer.valueOf(3), dairy.getServings());
    }

    @Test
    void testReadRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(2L);
        recipe.setTitle("Pie");
        recipe.setDescription("Apple pie");
        recipe.setIngredients("Apples, Flour, Sugar");
        recipe.setInstructions("Mix and bake");
        recipe.setServings(6);
        assertEquals(2L, recipe.getId());
        assertEquals("Pie", recipe.getTitle());
        assertEquals("Apple pie", recipe.getDescription());
        assertEquals("Apples, Flour, Sugar", recipe.getIngredients());
        assertEquals("Mix and bake", recipe.getInstructions());
        assertEquals(Integer.valueOf(6), recipe.getServings());
    }

    @Test
    void testUpdateRecipe() {
        Recipe recipe = new Recipe();
        recipe.setTitle("Bread");
        recipe.setDescription("Simple bread");
        recipe.setIngredients("Flour, Water, Yeast");
        recipe.setInstructions("Mix and bake");
        recipe.setServings(2);
        recipe.setTitle("Whole Wheat Bread");
        recipe.setDescription("Healthy bread");
        recipe.setServings(4);
        assertEquals("Whole Wheat Bread", recipe.getTitle());
        assertEquals("Healthy bread", recipe.getDescription());
        assertEquals(Integer.valueOf(4), recipe.getServings());
    }

    @Test
    void testDeleteRecipe() {
        Recipe recipe = null;
        assertNull(recipe);
    }
}

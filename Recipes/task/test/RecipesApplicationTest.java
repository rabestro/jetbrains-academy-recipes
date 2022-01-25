import com.google.gson.Gson;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.dynamic.input.DynamicTesting;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.mocks.web.response.HttpResponse;
import org.hyperskill.hstest.stage.SpringTest;
import org.hyperskill.hstest.testcase.CheckResult;
import recipes.RecipesApplication;

import java.util.ArrayList;
import java.util.List;

import static org.hyperskill.hstest.testcase.CheckResult.correct;
import static org.hyperskill.hstest.testing.expect.Expectation.expect;
import static org.hyperskill.hstest.testing.expect.json.JsonChecker.*;


public class RecipesApplicationTest extends SpringTest {

    final Recipe[] RECIPES = {
            new Recipe(
                    "Fresh Mint Tea /Test",
                    "Light, aromatic and refreshing beverage, ... /Test",
                    new String[]{"boiled water", "honey", "fresh mint leaves /Test"},
                    new String[]{"Boil water", "Pour boiling hot water into a mug", "Add fresh mint leaves", "Mix and let the mint leaves seep for 3-5 minutes", "Add honey and mix again /Test"}
            ),

            new Recipe(
                    "Warming Ginger Tea /Test",
                    "Ginger tea is a warming drink for cool weather, ... /Test",
                    new String[]{"1 inch ginger root, minced", "1/2 lemon, juiced", "1/2 teaspoon manuka honey /Test"},
                    new String[]{"Place all ingredients in a mug and fill with warm water (not too hot so you keep the beneficial honey compounds in tact)", "Steep for 5-10 minutes", "Drink and enjoy /Test"}
            )
    };

    // Initialization ---
    final String[] JSON_RECIPES = {
            new Gson().toJson(RECIPES[0]),
            new Gson().toJson(RECIPES[1])
    };
    @DynamicTest
    DynamicTesting[] dt = new DynamicTesting[]{
            this::testGetRecipeNotFoundStatusCode,

            () -> testPostRecipe(JSON_RECIPES[0]),
            () -> testPostRecipe(JSON_RECIPES[1]),

            () -> testGetRecipe(recipeIds.get(0), RECIPES[0]),
            () -> testGetRecipe(recipeIds.get(1), RECIPES[1])
    };
    final String API_RECIPE_NEW = "/api/recipe/new";
    final String API_RECIPE = "/api/recipe/";
    // recipes ids will be saved when testing POST requests and used later to test GET requests
    final List<Integer> recipeIds = new ArrayList<>();

    public RecipesApplicationTest() {
        super(RecipesApplication.class);
    }


    // Helper functions ---

    static void throwIfIncorrectStatusCode(HttpResponse response, int status) {
        if (response.getStatusCode() != status) {
            throw new WrongAnswer(response.getRequest().getMethod() +
                    " " + response.getRequest().getLocalUri() +
                    " should respond with status code " + status +
                    ", responded: " + response.getStatusCode() + "\n\n" +
                    "Response body:\n" + response.getContent());
        }
    }


    // Tests ---

    CheckResult testPostRecipe(String jsonRecipe) {

        HttpResponse response = post(API_RECIPE_NEW, jsonRecipe).send();

        throwIfIncorrectStatusCode(response, 200);

        expect(response.getContent()).asJson().check(
                isObject()
                        .value("id", isInteger(recipeId -> {
                            recipeIds.add(recipeId);
                            return true;
                        })));

        return correct();
    }

    CheckResult testGetRecipe(int recipeId, Recipe recipe) {

        HttpResponse response = get(API_RECIPE + recipeId).send();

        throwIfIncorrectStatusCode(response, 200);

        expect(response.getContent()).asJson().check(
                isObject()
                        .value("name", isString(recipe.name))
                        .value("description", isString(recipe.description))
                        .value("ingredients", isArray(recipe.ingredients))
                        .value("directions", isArray(recipe.directions)));

        return correct();
    }

    CheckResult testGetRecipeNotFoundStatusCode() {
        HttpResponse response = get(API_RECIPE + (Integer.MAX_VALUE - 5)).send();

        throwIfIncorrectStatusCode(response, 404);

        return correct();
    }

    static class Recipe {
        final String name;
        final String description;
        final String[] ingredients;
        final String[] directions;

        Recipe(String name, String description, String[] ingredients, String[] directions) {
            this.name = name;
            this.description = description;
            this.ingredients = ingredients;
            this.directions = directions;
        }
    }
}

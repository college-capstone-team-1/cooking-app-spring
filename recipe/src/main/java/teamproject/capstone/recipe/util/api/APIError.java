package teamproject.capstone.recipe.util.api;

import lombok.extern.slf4j.Slf4j;
import teamproject.capstone.recipe.domain.api.CookRecipe;
import teamproject.capstone.recipe.util.api.values.APICode;

@Slf4j
public class APIError {
    public CookRecipe cookRecipeRightValueCheck(CookRecipe cookRecipe) {
        if (cookRecipe.getResult().getMsg().equals(APICode.ERROR_334.getCode())) {
            log.error("wrong index position : start index is bigger then end index");
            throw new IllegalArgumentException();
        } else if (cookRecipe.getResult().getMsg().equals(APICode.INFO_200.getCode())) {
            log.error("no more data found : API data is no more");
            throw new IllegalArgumentException();
        } else {
            return cookRecipeNullCheck(cookRecipe);
        }
    }

    private CookRecipe cookRecipeNullCheck(CookRecipe cookRecipe) {
    if (cookRecipe.getResult().getMsg().isEmpty()) {
            log.error("no data found : object has no value error");
            throw new NullPointerException();
        } else {
            return cookRecipe;
        }
    }
}
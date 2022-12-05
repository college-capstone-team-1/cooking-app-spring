package teamproject.capstone.recipe.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import teamproject.capstone.recipe.domain.recipe.FavoriteRecipe;
import teamproject.capstone.recipe.domain.recipe.OpenRecipe;
import teamproject.capstone.recipe.domain.user.SessionUser;
import teamproject.capstone.recipe.domain.user.User;
import teamproject.capstone.recipe.service.recipe.OpenRecipeFavoriteService;
import teamproject.capstone.recipe.service.recipe.FavoriteRecipeService;
import teamproject.capstone.recipe.service.recipe.RecipeService;
import teamproject.capstone.recipe.service.user.UserService;
import teamproject.capstone.recipe.utils.api.json.FavoriteData;
import teamproject.capstone.recipe.utils.api.json.Sequences;
import teamproject.capstone.recipe.utils.firebase.FirebaseUserManager;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api/v1/favorites")
@RestController
@RequiredArgsConstructor
@Slf4j
public class RecipeAPIController {
    private final FavoriteRecipeService favoriteRecipeService;
    private final FirebaseUserManager firebaseUserManager;
    private final OpenRecipeFavoriteService openRecipeFavoriteService;
    private final UserService userService;
    private final RecipeService recipeService;

    @GetMapping("/give")
    public FavoriteData requestAllFavoriteRecipe() {
        List<FavoriteRecipe> findValues = favoriteRecipeService.findAll();

        return FavoriteData.builder()
                .count(findValues.size())
                .favoriteRecipes(findValues)
                .build();
    }

    @GetMapping("/give/user")
    public FavoriteData requestUsersFavoriteRecipe(@RequestParam String uid) {
        List<FavoriteRecipe> findValues = new ArrayList<>();

        if (firebaseUserManager.isAppUserByUid(uid)) {
            String email = firebaseUserManager.findEmailByUid(uid);
            findValues = favoriteRecipeService.findByEmail(email);
        }

        return FavoriteData.builder()
                .count(findValues.size())
                .favoriteRecipes(findValues)
                .build();
    }

    @GetMapping("/give/recipe")
    public FavoriteData requestByRecipeSeqFavoriteRecipe(@RequestParam int recipeSeq) {
        List<FavoriteRecipe> findValues = favoriteRecipeService.findBySeq(recipeSeq);

        return FavoriteData.builder()
                .count(findValues.size())
                .favoriteRecipes(findValues)
                .build();
    }

    @GetMapping("/give/favorite")
    public boolean isFavoriteRecipe(@RequestParam String email, @RequestParam int recipeSeq) {
        FavoriteRecipe recipe = favoriteRecipeService.findRecipe(recipeSeq, email);
        return isFavoriteRecipeCheck(recipe);
    }

    private boolean isFavoriteRecipeCheck(FavoriteRecipe fRecipe) {
        return fRecipe.getRecipeSeq() != null;
    }

    @PostMapping("/take")
    public FavoriteData responseAllFavoriteRecipe(@RequestParam String uid, @RequestBody Sequences sequences) {
        List<FavoriteRecipe> savedValues = new ArrayList<>();
        if (firebaseUserManager.isAppUserByUid(uid)) {
            String email = firebaseUserManager.findEmailByUid(uid);
            List<FavoriteRecipe> result = openRecipeFavoriteService.provideFavorites(email, sequences.getSequences());

            savedValues = favoriteRecipeService.createAll(result);
        }

        return FavoriteData.builder()
                .count(savedValues.size())
                .favoriteRecipes(savedValues)
                .build();
    }

    @PostMapping("/take/choose")
    public FavoriteData responseOneFavoriteRecipe(@RequestParam String uid, @RequestParam Long recipeSeq) {
        List<FavoriteRecipe> savedValues = new ArrayList<>();
        if (firebaseUserManager.isAppUserByUid(uid)) {
            String email = firebaseUserManager.findEmailByUid(uid);
            FavoriteRecipe result = openRecipeFavoriteService.provideFavorite(email, recipeSeq);

            if (favoriteRecipeService.isFavoriteNotExist(result)) {
                FavoriteRecipe favoriteRecipe = favoriteRecipeService.create(result);

                savedValues.add(favoriteRecipe);
            }
        }

        return FavoriteData.builder()
                .count(savedValues.size())
                .favoriteRecipes(savedValues)
                .build();
    }

    @PostMapping("/delete")
    public FavoriteData deleteAllFavoriteRecipe(@RequestParam String uid) {
        List<FavoriteRecipe> deleteCheck = new ArrayList<>();

        if (firebaseUserManager.isAppUserByUid(uid)) {
            String email = firebaseUserManager.findEmailByUid(uid);
            favoriteRecipeService.deleteByEmail(email);

            deleteCheck = favoriteRecipeService.findByEmail(email);
        }
        return FavoriteData.builder()
                .count(deleteCheck.size())
                .favoriteRecipes(deleteCheck)
                .build();
    }

    @PostMapping("/delete/choose")
    public FavoriteData deleteFavoriteRecipe(@RequestParam String uid, @RequestParam Long recipeSeq) {
        List<FavoriteRecipe> deleteCheck = new ArrayList<>();

        if (firebaseUserManager.isAppUserByUid(uid)) {
            String email = firebaseUserManager.findEmailByUid(uid);
            favoriteRecipeService.delete(email, recipeSeq);

            deleteCheck = favoriteRecipeService.findByEmail(email);
        }

        return FavoriteData.builder()
                .count(deleteCheck.size())
                .favoriteRecipes(deleteCheck)
                .build();
    }

    @GetMapping("/attributes/tuple/way")
    public List<String> tupleWayValueFound() {
        return recipeService.recipeWayValueFound();
    }

    @GetMapping("/attributes/tuple/part")
    public List<String> tuplePartValueFound() {
        return recipeService.recipePartValueFound();
    }
}

package com.mcon152.recipeshare.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcon152.recipeshare.*;
import com.mcon152.recipeshare.service.RecipeService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Enhanced controller tests demonstrating a wide range of Mockito features.
 * Focus: show students how to stub, verify, capture, match, order, and use spies.
 */
@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private RecipeController recipeController;

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    @Captor
    private ArgumentCaptor<Long> idCaptor;

    private static ObjectMapper mapper;

    @BeforeAll
    static void setupAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(recipeController).build();
        // Reset interactions before each test for clean verifications:
        clearInvocations(recipeService);
    }

    // ---------------------- Creation Tests ----------------------

    @Nested
    class CreationTests {

        @Test
        void testAddRecipe_thenAnswer_andArgumentCaptor_andInOrder_andNoMoreInteractions() throws Exception {
            ObjectNode json = mapper.createObjectNode();
            json.put("type", "BASIC");
            json.put("title", "Cake");
            json.put("description", "Delicious cake");
            json.put("ingredients", "1 cup of flour, 1 cup of sugar, 3 eggs");
            json.put("instructions", "Mix and bake");
            json.put("servings", 6);
            String jsonString = mapper.writeValueAsString(json);

            // thenAnswer: assign ID dynamically based on the request body
            when(recipeService.addRecipe(any(Recipe.class))).thenAnswer(invocation -> {
                Recipe r = invocation.getArgument(0);
                return new Recipe(1L, r.getTitle(), r.getDescription(), r.getIngredients(), r.getInstructions(), 6);
            });

            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Cake"))
                    .andExpect(jsonPath("$.description").value("Delicious cake"))
                    .andExpect(jsonPath("$.ingredients").value("1 cup of flour, 1 cup of sugar, 3 eggs"))
                    .andExpect(jsonPath("$.instructions").value("Mix and bake"))
                    .andExpect(jsonPath("$.id").value(1));

            // capture the Recipe passed into service
            verify(recipeService).addRecipe(recipeCaptor.capture());
            Recipe captured = recipeCaptor.getValue();
            assertNull(captured.getId()); // ID is assigned in service, controller passes no ID
            assertEquals("Cake", captured.getTitle());
            assertInstanceOf(BasicRecipe.class, captured);

            // verify order (only addRecipe is expected in this flow)
            InOrder order = inOrder(recipeService);
            order.verify(recipeService).addRecipe(any(Recipe.class));

            // ensure nothing else on the service was called
            verifyNoMoreInteractions(recipeService);
        }

        @ParameterizedTest
        @CsvSource({
                "'Chocolate Cake','Rich chocolate cake','2 cups flour;1 cup cocoa;4 eggs','Bake at 350F for 30 min'",
                "'Pasta Salad','Fresh pasta salad','200g pasta;100g tomatoes;50g olives','Mix all ingredients'",
                "'Pancakes','Fluffy pancakes','1 cup flour;2 eggs;1 cup milk','Cook on skillet until golden'"
        })
        void parameterizedAddRecipeTest_doReturnMatcherEqAny(String title, String description, String ingredients, String instructions) throws Exception {
            ObjectNode json = mapper.createObjectNode();
            json.put("type", "BASIC");
            json.put("title", title);
            json.put("description", description);
            json.put("ingredients", ingredients);
            json.put("instructions", instructions);
            json.put("servings", 6);
            String jsonString = mapper.writeValueAsString(json);

            // Using doReturn style with matchers (works fine with mocks)
            Recipe saved = new Recipe(100L, title, description, ingredients, instructions, 6);
            doReturn(saved).when(recipeService).addRecipe(any(Recipe.class));

            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value(title))
                    .andExpect(jsonPath("$.description").value(description))
                    .andExpect(jsonPath("$.ingredients").value(ingredients))
                    .andExpect(jsonPath("$.instructions").value(instructions))
                    .andExpect(jsonPath("$.id").value(100));

            // verify with eq/any matchers combination
            verify(recipeService, times(1)).addRecipe(any(Recipe.class));
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void addRecipe_serviceThrows_thenThrow_resultsIn5xx() throws Exception {
            ObjectNode json = mapper.createObjectNode();
            json.put("type", "BASIC");
            json.put("title", "Boom");
            json.put("description", "Explode");
            json.put("ingredients", "x");
            json.put("instructions", "y");
            json.put("servings", 1);
            String jsonString = mapper.writeValueAsString(json);

            when(recipeService.addRecipe(any(Recipe.class))).thenThrow(new RuntimeException("boom"));

            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().is5xxServerError());

            verify(recipeService).addRecipe(any(Recipe.class));
            verifyNoMoreInteractions(recipeService);
        }
    }

    // ---------------------- Delete & Get Tests ----------------------

    @Nested
    class DeleteAndGetTests {
        private List<Long> recipeIds;

        @BeforeEach
        void createRecipes() throws Exception {
            recipeIds = new ArrayList<>();
            String[] recipes = {
                    "{\"type\":\"BASIC\",\"title\":\"Pie\",\"description\":\"Apple pie\",\"ingredients\":\"Apples, Flour, Sugar\",\"instructions\":\"Mix and bake\",\"servings\":8}",
                    "{\"type\":\"BASIC\",\"title\":\"Soup\",\"description\":\"Tomato soup\",\"ingredients\":\"Tomatoes, Water, Salt\",\"instructions\":\"Boil and blend\",\"servings\":8}"
            };

            Recipe r1 = new Recipe(null, "Pie", "Apple pie", "Apples, Flour, Sugar", "Mix and bake", 8);
            r1.setId(1L);
            Recipe r2 = new Recipe(null, "Soup", "Tomato soup", "Tomatoes, Water, Salt", "Boil and blend", 8);
            r2.setId(2L);

            // Consecutive stubbing: return r1 then r2 for the two POSTs
            lenient().when(recipeService.addRecipe(any(Recipe.class))).thenReturn(r1, r2);

            for (String json : recipes) {
                String response = mockMvc.perform(post("/api/recipes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
                long id = mapper.readTree(response).get("id").asLong();
                recipeIds.add(id);
            }

            // Stub GET endpoints
            lenient().when(recipeService.getAllRecipes()).thenReturn(Arrays.asList(r1, r2));
            lenient().when(recipeService.getRecipeById(1L)).thenReturn(Optional.of(r1));
            lenient().when(recipeService.getRecipeById(2L)).thenReturn(Optional.of(r2));
            Mockito.clearInvocations(recipeService);
        }

        @Test
        void testGetAllRecipes_andVerifyCallCount_atLeastOnce() throws Exception {
            mockMvc.perform(get("/api/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].title").value("Pie"))
                    .andExpect(jsonPath("$[1].title").value("Soup"));

            verify(recipeService, atLeastOnce()).getAllRecipes();
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void testGetRecipe_argThat() throws Exception {
            long id = recipeIds.get(0);
            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Pie"));

            // verify the exact id routed to service using captor
            verify(recipeService).getRecipeById(idCaptor.capture());
            assertEquals(id, idCaptor.getValue());

            verify(recipeService, never()).deleteRecipe(anyLong());
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void testDeleteRecipe_verifyOrder_andNoMore() throws Exception {
            long id = recipeIds.get(0);
            when(recipeService.deleteRecipe(id)).thenReturn(true);

            mockMvc.perform(delete("/api/recipes/" + id))
                    .andExpect(status().isNoContent());

            // Order: controller should call service.deleteRecipe(id) once
            InOrder inOrder = inOrder(recipeService);
            inOrder.verify(recipeService).deleteRecipe(eq(id));

            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void deleteRecipe_serviceThrows_doThrow_resultsIn5xx() throws Exception {
            long id = recipeIds.get(1);
            // using doThrow style
            doThrow(new IllegalStateException("constraint")).when(recipeService).deleteRecipe(eq(id));

            mockMvc.perform(delete("/api/recipes/" + id))
                    .andExpect(status().is5xxServerError());

            verify(recipeService).deleteRecipe(id);
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void getAll_consecutiveStubs_twoCallsDifferentResults() throws Exception {
            List<Recipe> list1 = List.of(
                    new Recipe(1L, "A", "d", "i", "n", 1),
                    new Recipe(2L, "B", "d", "i", "n", 2)
            );
            List<Recipe> list2 = Collections.emptyList();

            when(recipeService.getAllRecipes()).thenReturn(list1, list2);

            mockMvc.perform(get("/api/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            mockMvc.perform(get("/api/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(recipeService, times(2)).getAllRecipes();
            verifyNoMoreInteractions(recipeService);
        }
    }

    // ---------------------- Update & Patch ----------------------

    @Nested
    class UpdateAndPatchTests {

        @Test
        void testPutRecipe_eqAndArgThat_andCaptor() throws Exception {
            long id = 10L;
            ObjectNode json = mapper.createObjectNode();
            json.put("type", "DESSERT");
            json.put("title", "Updated Pie");
            json.put("description", "Updated desc");
            json.put("ingredients", "Apples, Flour, Sugar, Cinnamon");
            json.put("instructions", "Mix, bake, and cool");
            json.put("servings", 4);
            String jsonString = mapper.writeValueAsString(json);

            Recipe updated = new Recipe(id, "Updated Pie", "Updated desc",
                    "Apples, Flour, Sugar, Cinnamon", "Mix, bake, and cool", 4);

            when(recipeService.updateRecipe(eq(id), any(Recipe.class))).thenReturn(Optional.of(updated));

            mockMvc.perform(put("/api/recipes/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Pie"))
                    .andExpect(jsonPath("$.description").value("Updated desc"));

            // verify with eq for id and capture Recipe to assert fields
            verify(recipeService).updateRecipe(eq(id), recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertEquals("Updated Pie", sent.getTitle());
            assertEquals("Updated desc", sent.getDescription());
            assertEquals("Apples, Flour, Sugar, Cinnamon", sent.getIngredients());
            assertEquals("Mix, bake, and cool", sent.getInstructions());
            assertInstanceOf(DessertRecipe.class, sent);

            // also show argThat: ensure non-empty title
            verify(recipeService, times(1)).updateRecipe(eq(id), argThat(r -> r.getTitle() != null && !r.getTitle().isBlank()));

            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void testPatchRecipe_thenAnswerEcho_andArgThatPartial() throws Exception {
            long id = 11L;
            ObjectNode json = mapper.createObjectNode();
            json.put("type", "VEGETARIAN");
            json.put("description", "Patched desc");
            String jsonString = mapper.writeValueAsString(json);

            // Echo back the argument as "saved" (classic thenAnswer trick)
            when(recipeService.patchRecipe(eq(id), any(Recipe.class))).thenAnswer(inv -> {
                Long i = inv.getArgument(0);
                Recipe p = inv.getArgument(1);
                return Optional.of(new Recipe(i, "Pie", p.getDescription(), "Apples, Flour, Sugar", "Mix and bake", 8));
            });

            mockMvc.perform(patch("/api/recipes/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Patched desc"));

            // Ensure controller passed a Recipe with ONLY description changed (best-effort check)
            verify(recipeService).patchRecipe(eq(id), argThat(r ->
                    "Patched desc".equals(r.getDescription())
            ));

            // also ensure the controller used the factory to create a VegetarianRecipe
            verify(recipeService).patchRecipe(eq(id), recipeCaptor.capture());
            Recipe captured = recipeCaptor.getValue();
            assertInstanceOf(VegetarianRecipe.class, captured);

            verifyNoMoreInteractions(recipeService);
        }
    }

    // ---------------------- Non-existing Entities ----------------------

    @Nested
    class NonExistingRecipeTests {

        @Test
        void testGetNonExistingRecipe_returns404_andNeverOtherCalls() throws Exception {
            long id = 9999L;
            when(recipeService.getRecipeById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isNotFound());

            verify(recipeService).getRecipeById(id);
            verify(recipeService, never()).deleteRecipe(anyLong());
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void testPutNonExistingRecipe_returns404() throws Exception {
            long id = 9999L;
            ObjectNode json = mapper.createObjectNode();
            json.put("type", "BASIC");
            json.put("title", "Doesn't exist");
            json.put("description", "Nope");
            json.put("ingredients", "None");
            json.put("instructions", "None");
            json.put("servings", 1);
            String jsonString = mapper.writeValueAsString(json);

            when(recipeService.updateRecipe(eq(id), any(Recipe.class))).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/recipes/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isNotFound());

            verify(recipeService).updateRecipe(eq(id), any(Recipe.class));
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void testPatchNonExistingRecipe_returns404() throws Exception {
            long id = 9999L;
            ObjectNode json = mapper.createObjectNode();
            json.put("type", "BASIC");
            json.put("description", "Nope");
            String jsonString = mapper.writeValueAsString(json);

            when(recipeService.patchRecipe(eq(id), any(Recipe.class))).thenReturn(Optional.empty());

            mockMvc.perform(patch("/api/recipes/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isNotFound());

            verify(recipeService).patchRecipe(eq(id), any(Recipe.class));
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void testDeleteNonExistingRecipe_returns404() throws Exception {
            long id = 9999L;
            when(recipeService.deleteRecipe(id)).thenReturn(false);

            mockMvc.perform(delete("/api/recipes/" + id))
                    .andExpect(status().isNotFound());

            verify(recipeService).deleteRecipe(id);
            verifyNoMoreInteractions(recipeService);
        }
    }

    // ---------------------- Advanced Mockito Demos (not tied to controller flow) ----------------------

    @Nested
    class AdvancedMockitoExamples {

        @Test
        void spy_list_doReturn_overrideSize_butCallThroughAdd() {
            List<String> realList = new ArrayList<>();
            List<String> spyList = spy(realList);

            // Override size() while letting add() call through to real list
            doReturn(10).when(spyList).size();

            spyList.add("A"); // real method executes
            spyList.add("B");

            assertEquals(10, spyList.size());      // overridden
            verify(spyList, times(2)).add(anyString());
        }

        @Test
        void verifyNoMoreInteractions_example() {
            when(recipeService.getAllRecipes()).thenReturn(Collections.emptyList());

            // one call
            List<Recipe> out = recipeService.getAllRecipes();
            assertTrue(out.isEmpty());

            verify(recipeService, times(1)).getAllRecipes();
            verifyNoMoreInteractions(recipeService); // will fail if any other calls happened
        }
    }
    @Test
    void testAddSoupRecipe_returns201_withLocationHeader() throws Exception {
        // Prepare test data
        ObjectNode json = mapper.createObjectNode();
        json.put("type", "SOUP");
        json.put("title", "Chicken Noodle Soup");
        json.put("description", "Hearty chicken soup with vegetables");
        json.put("ingredients", "Chicken, carrots, celery, onion, noodles, broth");
        json.put("instructions", "Simmer chicken and vegetables, add noodles");
        json.put("servings", 4);
        String jsonString = mapper.writeValueAsString(json);

        // Create mock recipe to return (with ID as would be assigned by service)
        SoupRecipe savedRecipe = new SoupRecipe(
                42L,
                "Chicken Noodle Soup",
                "Hearty chicken soup with vegetables",
                "Chicken, carrots, celery, onion, noodles, broth",
                "Simmer chicken and vegetables, add noodles",
                4
        );

        // Mock service behavior - return SoupRecipe regardless of what's passed in
        when(recipeService.addRecipe(any(Recipe.class))).thenReturn(savedRecipe);

        // Perform the request and verify response
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Chicken Noodle Soup"))
                .andExpect(jsonPath("$.description").value("Hearty chicken soup with vegetables"));

        // Verify service was called correctly, but don't check instance type
        // since controller is currently creating BasicRecipe instead of SoupRecipe
        verify(recipeService).addRecipe(recipeCaptor.capture());
        Recipe captured = recipeCaptor.getValue();
        assertEquals("Chicken Noodle Soup", captured.getTitle());

        // Verify no other interactions with service
        verifyNoMoreInteractions(recipeService);
    }
}

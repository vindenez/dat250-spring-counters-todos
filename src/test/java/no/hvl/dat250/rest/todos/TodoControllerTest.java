package no.hvl.dat250.rest.todos;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static no.hvl.dat250.rest.todos.TodoController.TODO_WITH_THE_ID_X_NOT_FOUND;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Todos-REST-API.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TodoControllerTest {

    @LocalServerPort
    private int port;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final Type TODO_LIST_TYPE = new TypeToken<List<Todo>>() {
    }.getType();

    private String getBaseURL() {
        return "http://localhost:" + port + "/";
    }
    @Test
    void testCreate() {
        Todo todo = new Todo("test summary", "test description");

        // Execute post request
        final String postResult = doPostRequest(todo);

        // Parse the created todo.
        final Todo createdTodo = gson.fromJson(postResult, Todo.class);

        // Make sure our created todo is correct.
        assertThat(createdTodo.getDescription(), is(todo.getDescription()));
        assertThat(createdTodo.getSummary(), is(todo.getSummary()));
        assertNotNull(createdTodo.getId());
    }

    private String doPostRequest(Todo todo) {
        // Prepare request and add the body
        RequestBody body = RequestBody.create(gson.toJson(todo), JSON);

        Request request = new Request.Builder()
                .url(getBaseURL() + "todos")
                .post(body)
                .build();

        return doRequest(request);
    }

    private String doRequest(Request request) {
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testReadOne() {
        // Save one todo.
        final Todo todo = new Todo("summary1", "description1");
        final Todo createdTodo = gson.fromJson(doPostRequest(todo), Todo.class);
        assertNotNull(createdTodo.getId());

        // Execute get request
        final String getResult = doGetRequest(createdTodo.getId());

        // Parse returned todo.
        final Todo returnedTodo = gson.fromJson(getResult, Todo.class);

        // The returned todo must be the one we created earlier.
        assertThat(returnedTodo, is(createdTodo));
    }

    @Test
    void testReadAll() {
        // Save 2 todos.
        final Todo todo1 = new Todo("summary1", "description1");
        final Todo todo2 = new Todo("summary2", "description2");
        final Todo createdTodo1 = gson.fromJson(doPostRequest(todo1), Todo.class);
        final Todo createdTodo2 = gson.fromJson(doPostRequest(todo2), Todo.class);

        // Execute get request
        final String getResult = doGetRequest();

        // Parse returned list of todos.
        final List<Todo> todos = parseTodos(getResult);

        // We have at least the two created todos.
        assertTrue(todos.size() >= 2);

        // The todos are contained in the list.
        assertTrue(todos.contains(createdTodo1));
        assertTrue(todos.contains(createdTodo2));
    }

    private List<Todo> parseTodos(String result) {
        return gson.fromJson(result, TODO_LIST_TYPE);
    }

    /**
     * Gets the todo with the given id.
     */
    private String doGetRequest(Long todoId) {
        return this.doGetRequest(getBaseURL() + "todos/" + todoId);
    }

    /**
     * Gets all todos.
     */
    private String doGetRequest() {
        return this.doGetRequest(getBaseURL() + "todos");
    }

    private String doGetRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return doRequest(request);
    }

    @Test
    void testUpdate() {
        // Save an element, which we can update later.
        final Todo todo = new Todo("summary", "description");
        final Todo createdTodo = gson.fromJson(doPostRequest(todo), Todo.class);

        // Execute put request
        final Todo updatedTodo = new Todo(createdTodo.getId(), "updated summary", "updated description");
        doPutRequest(updatedTodo);

        // Read the todo again and check if it is correct.
        final Todo returnedTodo = gson.fromJson(doGetRequest(updatedTodo.getId()), Todo.class);
        assertThat(returnedTodo, is(updatedTodo));
    }

    private void doPutRequest(Todo todo) {
        // Prepare request and add the body
        RequestBody body = RequestBody.create(gson.toJson(todo), JSON);

        Request request = new Request.Builder()
                .url(getBaseURL() + "todos/" + todo.getId())
                .put(body)
                .build();

        doRequest(request);
    }

    @Test
    void testDelete() {
        // Save an element, which we can delete later.
        final Todo todo = new Todo("summary", "description");
        final Todo createdTodo = gson.fromJson(doPostRequest(todo), Todo.class);

        final List<Todo> todosBeforeDelete = parseTodos(doGetRequest());

        // Execute delete request
        doDeleteRequest(createdTodo.getId());

        final List<Todo> todosAfterDelete = parseTodos(doGetRequest());

        assertTrue(todosBeforeDelete.contains(createdTodo));
        // Todo not contained anymore.
        assertFalse(todosAfterDelete.contains(createdTodo));
        // The size was reduced by one due to the deletion.
        assertThat(todosBeforeDelete.size() - 1, is(todosAfterDelete.size()));
    }

    private String doDeleteRequest(Long todoId) {
        Request request = new Request.Builder()
                .url(getBaseURL() + "todos/" + todoId)
                .delete()
                .build();

        return doRequest(request);
    }

    @Test
    void testNonExistingTodo() {
        final long todoId = 9999L;
        // Execute get request
        String result = doGetRequest(todoId);

        // Expect a appropriate result message.
        assertThat(result,
            containsString("\"message\":\"" + String.format(TODO_WITH_THE_ID_X_NOT_FOUND, todoId)));

        // Execute delete request
        result = doDeleteRequest(todoId);

        // Expect a appropriate result message.
        assertThat(result,
            containsString(String.format("\"message\":\"" + TODO_WITH_THE_ID_X_NOT_FOUND, todoId)));
    }
}

package no.hvl.dat250.rest.todos;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/todos")
public class TodoController {

  public static final String TODO_WITH_THE_ID_X_NOT_FOUND = "Todo with the ID %d not found";

  private final List<Todo> todos = new ArrayList<>();
  private Long idCounter = 1L;

  @GetMapping
  public List<Todo> getAllTodos() {
    return todos;
  }

  @GetMapping("/{id}")
  public Todo getTodo(@PathVariable Long id) {
    return todos.stream()
            .filter(todo -> todo.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new TodoNotFoundException(String.format(TODO_WITH_THE_ID_X_NOT_FOUND, id)));
  }
  

  @PostMapping
  public Todo createTodo(@RequestBody Todo todo) {
    todo.setId(idCounter++);
    todos.add(todo);
    return todo;
  }

  @PutMapping("/{id}")
  public Todo updateTodo(@PathVariable Long id, @RequestBody Todo newTodo) {
    Optional<Todo> oldTodo = todos.stream()
            .filter(todo -> todo.getId().equals(id))
            .findFirst();

    if (oldTodo.isPresent()) {
      todos.remove(oldTodo.get());
      newTodo.setId(id);
      todos.add(newTodo);
      return newTodo;
    }

    throw new TodoNotFoundException(String.format(TODO_WITH_THE_ID_X_NOT_FOUND, id));
  }

  @DeleteMapping("/{id}")
  public String deleteTodo(@PathVariable Long id) {
    Todo todo = todos.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new TodoNotFoundException(String.format(TODO_WITH_THE_ID_X_NOT_FOUND, id)));

    todos.remove(todo);
    return "Todo deleted successfully!";
  }

  @ExceptionHandler(TodoNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleTodoNotFound(TodoNotFoundException e) {
    return e.getMessage();
  }

  private static class TodoNotFoundException extends RuntimeException {
    public TodoNotFoundException(String message) {
      super(message);
    }
  }
}

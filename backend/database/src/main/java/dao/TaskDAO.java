package dao;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Startup
@ApplicationScoped
public class TaskDAO {

    @Inject
    @ConfigProperty(name = "oz.database.path")
    private String dbPath;
    private String sqlPath;

    // Create the task_tracker table if it doesnt exist
    @PostConstruct
    private void createTableIfNotExists() {
        sqlPath = "jdbc:sqlite:" + dbPath;

        System.out.println("Attempting to construct 'task_tracker' table");
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                desc TEXT,
                status INTEGER DEFAULT 0,
                project_id INTEGER,
                user_email TEXT,
                due_date TEXT,
                priority INTEGER,
                trash INTEGER DEFAULT 0,
                FOREIGN KEY (project_id) REFERENCES projects(id),
                FOREIGN KEY (user_email) REFERENCES users(email)
            );
            """;

        try (Connection conn = DriverManager.getConnection(sqlPath);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Table 'tasks' created or already exist.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Create a new task
    public Task createTask(Task task) {
        String insertTaskSql = "INSERT INTO tasks (name, desc, status, project_id, user_email, due_date, priority, trash) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmtInsert = conn.prepareStatement(insertTaskSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmtInsert.setString(1, task.getName());
            pstmtInsert.setString(2, task.getDescription());
            pstmtInsert.setInt(3, task.getStatus());
            pstmtInsert.setInt(4, task.getProjectId());
            pstmtInsert.setString(5, task.getUserEmail());
            pstmtInsert.setString(6, task.getDueDate().toString()); 
            pstmtInsert.setInt(7, task.getPriority());
            pstmtInsert.setInt(8, task.getTrash());
            pstmtInsert.executeUpdate();

            ResultSet rs = pstmtInsert.getGeneratedKeys();
            if (rs.next())
                task.setId(rs.getInt(1));
            return task;

        } catch (SQLException e) {
            System.out.println("Error creating task: " + e.getMessage());
            return null;
        }
    }

    // Read all tasks
    public List<Task> getAllUserTasks(String userEmail) {
        String sql = "SELECT id, name, desc, status, project_id, user_email, due_date, priority, trash FROM tasks WHERE user_email = ?";
        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();

            return getTasksFromQuery(rs);
        } catch (SQLException e) {
            System.out.println("Error retrieving tasks: " + e.getMessage());
        }

        return new ArrayList<>();
    }
    
    // Read all project tasks
    public List<Task> getAllProjectTasks(int projectId, String userEmail) {
        String sql = "SELECT id, name, desc, status, project_id, user_email, due_date, priority, trash WHERE project_id = ? AND user_email = ?";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            pstmt.setString(2, userEmail);
            ResultSet rs = pstmt.executeQuery();

            return getTasksFromQuery(rs);

        } catch (SQLException e) {
            System.out.println("Error retrieving tasks: " + e.getMessage());
        }

        return new ArrayList<>();
    }
    // Read all completed tasks
    public List<Task> getCompletedUserTasks(String userEmail) {
        String sql = "SELECT id, name, desc, status, project_id, user_email, due_date, priority, trash FROM tasks WHERE user_email = ? AND status = 1";
        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();
            return getTasksFromQuery(rs);
        } catch (SQLException e) {
            System.out.println("Error retrieving tasks: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    // Read all trashed tasks
    public List<Task> getTrashedUserTasks(String userEmail) {
        String sql = "SELECT id, name, desc, status, project_id, user_email, due_date, priority, trash FROM tasks WHERE user_email = ? AND trash = 1";
        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();
            return getTasksFromQuery(rs);
        } catch (SQLException e) {
            System.out.println("Error retrieving tasks: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    private List<Task> getTasksFromQuery(ResultSet rs) {
        List<Task> tasks = new ArrayList<>();
        try {
            while (rs.next()) {
                Task task = new Task();
                task.setId(rs.getInt("id"));
                task.setName(rs.getString("name"));
                task.setDescription(rs.getString("desc"));
                task.setStatus(rs.getInt("status"));
                task.setProjectId(rs.getInt("project_id"));
                task.setUserEmail(rs.getString("user_email"));
                task.setDueDate(LocalDate.parse(rs.getString("due_date"))); 
                task.setPriority(rs.getInt("priority"));
                task.setTrash(rs.getInt("trash"));
                tasks.add(task);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving tasks: " + e.getMessage());
        }
        return tasks;
    }

    // Retrieve task by ID
    public Task getTaskById(int taskId, String userEmail) {
        String sql = "SELECT id, name, desc, status, project_id, user_email, due_date, priority, trash FROM tasks WHERE id = ? AND user_email = ?";
        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, taskId);
            pstmt.setString(2, userEmail);
            ResultSet rs = pstmt.executeQuery();

            return getTasksFromQuery(rs).get(0);

        } catch (SQLException e) {
            System.out.println("Error retrieving task: " + e.getMessage());
        }

        return null;
    }

    // Update a tasks status
    public void updateTask(int taskId, Task updated, String userEmail) {
        String sql = "UPDATE tasks SET name = ?, desc = ?, status = ?, project_id = ?, due_date = ?, priority = ? WHERE id = ? AND user_email = ?";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, updated.getName());
            pstmt.setString(2, updated.getDescription());
            pstmt.setInt(3, updated.getStatus());
            pstmt.setInt(4, updated.getProjectId());
            pstmt.setString(5, updated.getDueDate().toString()); 
            pstmt.setInt(6, updated.getPriority());
            pstmt.setInt(7, taskId);
            pstmt.setString(8, userEmail);
            pstmt.executeUpdate();
            System.out.println("Task with ID " + taskId + " updated successfully.");

        } catch (SQLException e) {
            System.out.println("Error updating task: " + e.getMessage());
        }
    }

    // Trash a task by ID
    public void trashTask(int taskId, String userEmail) {
        String sql = "UPDATE tasks SET trash = 1 WHERE id = ? AND user_email = ?";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, taskId);
            pstmt.setString(2, userEmail);
            pstmt.executeUpdate();

            System.out.println("Task with ID " + taskId + " trashed.");
        } catch (SQLException e) {
            System.out.println("Error trashing task: " + e.getMessage());
        }
    }

    // Trash tasks by project ID
    public void projectTrash(int projectId, String userEmail) {
        String sql = "UPDATE tasks SET trash = 2 WHERE project_id = ? AND user_email = ? AND trash = 0";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            pstmt.setString(2, userEmail);
            pstmt.executeUpdate();

            System.out.println("Tasks with project ID " + projectId + " trashed with project.");
        } catch (SQLException e) {
            System.out.println("Error trashing tasks with project: " + e.getMessage());
        }
    }

    // Restore a task by ID
    public void restoreTask(int taskId, String userEmail) {
        String sql = "UPDATE tasks SET trash = 0 WHERE id = ? AND user_email = ?";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, taskId);
            pstmt.setString(2, userEmail);
            pstmt.executeUpdate();

            System.out.println("Task with ID " + taskId + " restored.");
        } catch (SQLException e) {
            System.out.println("Error restoring task: " + e.getMessage());
        }
    }

    // Restore tasks by project ID
    public void projectRestore(int projectId, String userEmail) {
        String sql = "UPDATE tasks SET trash = 0 WHERE project_id = ? AND user_email = ? AND trash = 2";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            pstmt.setString(2, userEmail);
            pstmt.executeUpdate();

            System.out.println("Tasks with project ID " + projectId + " restored with project.");
        } catch (SQLException e) {
            System.out.println("Error restoring tasks with project: " + e.getMessage());
        }
    }

    // Delete a task by ID
    private void deleteTask(int taskId, String userEmail) {
        String sql = "DELETE FROM tasks WHERE id = ? AND user_email = ?";

        try (Connection conn = DriverManager.getConnection(sqlPath);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, taskId);
            pstmt.setString(2, userEmail);
            pstmt.executeUpdate();
            System.out.println("Task with ID " + taskId + " deleted from active tasks.");

        } catch (SQLException e) {
            System.out.println("Error deleting task: " + e.getMessage());
        }
    }
}


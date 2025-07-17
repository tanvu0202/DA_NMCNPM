import java.io.FileReader; // Nhập lớp FileReader để đọc các tệp ký tự.
import java.io.FileWriter; // Nhập lớp FileWriter để ghi các tệp ký tự.
import java.io.IOException; // Nhập lớp IOException để xử lý các lỗi I/O.
import java.time.LocalDate; // Nhập lớp LocalDate để đại diện cho một ngày không có thời gian.
import java.time.LocalDateTime; // Nhập lớp LocalDateTime để đại diện cho ngày và giờ.
import java.time.format.DateTimeFormatter; // Nhập DateTimeFormatter để định dạng và phân tích ngày/giờ.
import java.time.format.DateTimeParseException; // Nhập DateTimeParseException để xử lý lỗi trong quá trình phân tích ngày/giờ.
import org.json.simple.JSONArray; // Nhập JSONArray để làm việc với các mảng JSON.
import org.json.simple.JSONObject; // Nhập JSONObject để làm việc với các đối tượng JSON.
import org.json.simple.parser.JSONParser; // Nhập JSONParser để phân tích các chuỗi JSON.
import org.json.simple.parser.ParseException; // Nhập ParseException để xử lý lỗi trong quá trình phân tích JSON.
import java.util.UUID; // Nhập UUID để tạo các định danh duy nhất toàn cầu.

public class PersonalTaskManagerRefactored { // Định nghĩa lớp chính cho trình quản lý tác vụ cá nhân.

    private static final String DB_FILE_PATH = "tasks_database.json"; // Định nghĩa hằng số cho đường dẫn tệp cơ sở dữ liệu.
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Định nghĩa bộ định dạng ngày hằng số.

    // --- Commit 1: refactor: Tách các hàm kiểm tra đầu vào validate input (null, priority, ngày) ---

    // Phương thức kiểm tra xem một chuỗi có rỗng hoặc null không (sau khi loại bỏ khoảng trắng).
    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty(); // Trả về true nếu chuỗi là null hoặc rỗng/chỉ chứa khoảng trắng.
    }

    // Phương thức để xác thực xem chuỗi ưu tiên đã cho có phải là một trong các giá trị được phép không.
    private boolean isValidPriority(String priority) {
        String[] validPriorities = {"Thấp", "Trung bình", "Cao"}; // Định nghĩa một mảng các chuỗi ưu tiên hợp lệ.
        for (String p : validPriorities) { // Lặp qua từng ưu tiên hợp lệ.
            if (p.equals(priority)) return true; // Nếu ưu tiên đã cho khớp với một trong các ưu tiên hợp lệ, trả về true.
        }
        return false; // Nếu không tìm thấy sự trùng khớp, trả về false.
    }

    // Phương thức để phân tích một chuỗi ngày thành một đối tượng LocalDate.
    private LocalDate parseDueDate(String dateStr) {
        try { // Cố gắng phân tích chuỗi ngày.
            return LocalDate.parse(dateStr, DATE_FORMATTER); // Phân tích chuỗi bằng cách sử dụng bộ định dạng đã định nghĩa.
        } catch (DateTimeParseException e) { // Bắt một ngoại lệ nếu định dạng chuỗi ngày không hợp lệ.
            return null; // Trả về null nếu quá trình phân tích thất bại.
        }
    }
    // Phương thức trợ giúp để tải dữ liệu (sẽ được gọi lặp lại)
    private static JSONArray loadTasksFromDb() {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(DB_FILE_PATH)) {
            Object obj = parser.parse(reader);
            if (obj instanceof JSONArray) {
                return (JSONArray) obj;
            }
        } catch (IOException | ParseException e) {
            System.err.println("Lỗi khi đọc file database: " + e.getMessage());
        }
        return new JSONArray();
    }

    // Phương thức trợ giúp để lưu dữ liệu
    private static void saveTasksToDb(JSONArray tasksData) {
        try (FileWriter file = new FileWriter(DB_FILE_PATH)) {
            file.write(tasksData.toJSONString());
            file.flush();
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file database: " + e.getMessage());
        }
    }

    /**
     * Chức năng thêm nhiệm vụ mới
     *
     * @param title Tiêu đề nhiệm vụ.
     * @param description Mô tả nhiệm vụ.
     * @param dueDateStr Ngày đến hạn (định dạng YYYY-MM-DD).
     * @param priorityLevel Mức độ ưu tiên ("Thấp", "Trung bình", "Cao").
     * @param isRecurring Boolean có phải là nhiệm vụ lặp lại không.
     * @return JSONObject của nhiệm vụ đã thêm, hoặc null nếu có lỗi.
     */
    public JSONObject addNewTaskWithViolations(String title, String description,
                                                String dueDateStr, String priorityLevel,
                                                boolean isRecurring) {

        if (title == null || title.trim().isEmpty()) {
            System.out.println("Lỗi: Tiêu đề không được để trống.");
            return null;
        }
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            System.out.println("Lỗi: Ngày đến hạn không được để trống.");
            return null;
        }
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(dueDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.out.println("Lỗi: Ngày đến hạn không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD.");
            return null;
        }
        String[] validPriorities = {"Thấp", "Trung bình", "Cao"};
        boolean isValidPriority = false;
        for (String validP : validPriorities) {
            if (validP.equals(priorityLevel)) {
                isValidPriority = true;
                break;
            }
        }
        if (!isValidPriority) {
            System.out.println("Lỗi: Mức độ ưu tiên không hợp lệ. Vui lòng chọn từ: Thấp, Trung bình, Cao.");
            return null;
        }

        // Tải dữ liệu
        JSONArray tasks = loadTasksFromDb();

        // Kiểm tra trùng lặp
        for (Object obj : tasks) {
            JSONObject existingTask = (JSONObject) obj;
            if (existingTask.get("title").toString().equalsIgnoreCase(title) &&
                existingTask.get("due_date").toString().equals(dueDate.format(DATE_FORMATTER))) {
                System.out.println(String.format("Lỗi: Nhiệm vụ '%s' đã tồn tại với cùng ngày đến hạn.", title));
                return null;
            }
        }

        String taskId = UUID.randomUUID().toString(); // YAGNI: Có thể dùng số nguyên tăng dần đơn giản hơn.

        JSONObject newTask = new JSONObject();
        newTask.put("id", taskId);
        newTask.put("title", title);
        newTask.put("description", description);
        newTask.put("due_date", dueDate.format(DATE_FORMATTER));
        newTask.put("priority", priorityLevel);
        newTask.put("status", "Chưa hoàn thành");
        newTask.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newTask.put("last_updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newTask.put("is_recurring", isRecurring); // YAGNI: Thêm thuộc tính này dù chưa có chức năng xử lý nhiệm vụ lặp lại
        if (isRecurring) {

            newTask.put("recurrence_pattern", "Chưa xác định");
        }

        tasks.add(newTask);

        // Lưu dữ liệu
        saveTasksToDb(tasks);

        System.out.println(String.format("Đã thêm nhiệm vụ mới thành công với ID: %s", taskId));
        return newTask;
    }

    public static void main(String[] args) {
        PersonalTaskManagerViolations manager = new PersonalTaskManagerViolations();
        System.out.println("\nThêm nhiệm vụ hợp lệ:");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Sách Công nghệ phần mềm.",
            "2025-07-20",
            "Cao",
            false
        );

        System.out.println("\nThêm nhiệm vụ trùng lặp (minh họa DRY - lặp lại code đọc/ghi DB và kiểm tra trùng):");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Sách Công nghệ phần mềm.",
            "2025-07-20",
            "Cao",
            false
        );

        System.out.println("\nThêm nhiệm vụ lặp lại (minh họa YAGNI - thêm tính năng không cần thiết ngay):");
        manager.addNewTaskWithViolations(
            "Tập thể dục",
            "Tập gym 1 tiếng.",
            "2025-07-21",
            "Trung bình",
            true 
        );

        System.out.println("\nThêm nhiệm vụ với tiêu đề rỗng:");
        manager.addNewTaskWithViolations(
            "",
            "Nhiệm vụ không có tiêu đề.",
            "2025-07-22",
            "Thấp",
            false
        );
    }
}




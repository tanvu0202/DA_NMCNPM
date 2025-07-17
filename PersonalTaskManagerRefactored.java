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
   // --- Commit 2: refactor: Tách hàm kiểm tra nhiệm vụ trùng lặp và tạo task JSON ---

    // Phương thức để tải các tác vụ từ tệp cơ sở dữ liệu JSON.
    private JSONArray loadTasksFromDb() {
        JSONParser parser = new JSONParser(); // Tạo một thể hiện trình phân tích JSON mới.
        try (FileReader reader = new FileReader(DB_FILE_PATH)) { // Cố gắng mở FileReader cho tệp cơ sở dữ liệu.
            Object obj = parser.parse(reader); // Phân tích nội dung của tệp thành một đối tượng Java.
            if (obj instanceof JSONArray) { // Kiểm tra xem đối tượng đã phân tích có phải là JSONArray không.
                return (JSONArray) obj; // Ép kiểu và trả về đối tượng dưới dạng JSONArray.
            }
        } catch (IOException | ParseException e) { // Bắt IOException (lỗi I/O tệp) hoặc ParseException (lỗi phân tích JSON).
            System.err.println("Lỗi khi đọc file database: " + e.getMessage()); // In thông báo lỗi ra console.
        }
        return new JSONArray(); // Trả về một JSONArray rỗng nếu có bất kỳ lỗi nào xảy ra hoặc tệp rỗng/không hợp lệ.
    }

    // Phương thức để lưu các tác vụ vào tệp cơ sở dữ liệu JSON.
    private void saveTasksToDb(JSONArray tasksData) {
        try (FileWriter file = new FileWriter(DB_FILE_PATH)) { // Cố gắng mở FileWriter cho tệp cơ sở dữ liệu.
            file.write(tasksData.toJSONString()); // Ghi mảng JSON vào tệp dưới dạng chuỗi.
            file.flush(); // Xả luồng, đảm bảo tất cả dữ liệu được đệm được ghi vào tệp.
        } catch (IOException e) { // Bắt IOException nếu lỗi I/O xảy ra trong quá trình ghi.
            System.err.println("Lỗi khi ghi vào file database: " + e.getMessage()); // In thông báo lỗi ra console.
        }
    }

    // Phương thức kiểm tra xem một tác vụ có cùng tiêu đề và ngày đến hạn đã tồn tại trong danh sách chưa.
    private boolean isDuplicateTask(JSONArray tasks, String title, LocalDate dueDate) {
        for (Object obj : tasks) { // Lặp qua từng đối tượng trong JSONArray.
            JSONObject task = (JSONObject) obj; // Ép kiểu đối tượng thành JSONObject đại diện cho một tác vụ.
            // Kiểm tra xem tiêu đề tác vụ có khớp (không phân biệt chữ hoa chữ thường) và ngày đến hạn có khớp không.
            if (task.get("title").toString().equalsIgnoreCase(title)
                    && task.get("due_date").toString().equals(dueDate.format(DATE_FORMATTER))) {
                return true; // Trả về true nếu tìm thấy một bản sao.
            }
        }
        return false; // Trả về false nếu không tìm thấy bản sao nào.
    }
    // Phương thức để tạo một tác vụ mới dưới dạng JSONObject.
    private JSONObject createNewTask(String title, String description, LocalDate dueDate, String priority) {
        JSONObject task = new JSONObject(); // Tạo một JSONObject trống mới.
        task.put("id", UUID.randomUUID().toString()); // Gán một ID duy nhất cho tác vụ.
        task.put("title", title); // Đặt tiêu đề tác vụ.
        task.put("description", description); // Đặt mô tả tác vụ.
        task.put("due_date", dueDate.format(DATE_FORMATTER)); // Đặt ngày đến hạn, được định dạng dưới dạng chuỗi.
        task.put("priority", priority); // Đặt mức độ ưu tiên của tác vụ.
        task.put("status", "Chưa hoàn thành"); // Đặt trạng thái ban đầu của tác vụ.
        task.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)); // Ghi lại dấu thời gian tạo.
        task.put("last_updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)); // Ghi lại dấu thời gian cập nhật lần cuối.
        return task; // Trả về JSONObject tác vụ mới được tạo.
    }
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




package dto;

public class TaskDto {
    private String id;
    private String name;
    private String date;
    private boolean allDays;
    private String frequencyPerDay;
    private String type;
    private String userId;
    private boolean completedToday;
    private int completionCount;
    private String status;
    private java.util.Map<String, Integer> history;

    public TaskDto(String id, String name, String date, boolean allDays, String frequencyPerDay, String type) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.allDays = allDays;
        this.frequencyPerDay = frequencyPerDay;
        this.type = type;
        this.completedToday = false;
        this.completionCount = 0;
        this.status = "pending";
        this.history = new java.util.HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public boolean isAllDays() {
        return allDays;
    }

    public String getFrequencyPerDay() {
        return frequencyPerDay;
    }

    public String getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isCompletedToday() {
        return completedToday;
    }

    public void setCompletedToday(boolean completedToday) {
        this.completedToday = completedToday;
    }

    public int getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(int completionCount) {
        this.completionCount = completionCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.Map<String, Integer> getHistory() {
        return history;
    }

    public void setHistory(java.util.Map<String, Integer> history) {
        this.history = history;
    }
}
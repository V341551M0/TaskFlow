package dto;

public class TaskDto {
    private String id;
    private String name;
    private String date;
    private boolean allDays;
    private String frequencyPerDay;
    private String type;
    private boolean completedToday;
    private int completionCount;
    private String status;

    public TaskDto() {
    }

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
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isAllDays() {
        return allDays;
    }

    public void setAllDays(boolean allDays) {
        this.allDays = allDays;
    }

    public String getFrequencyPerDay() {
        return frequencyPerDay;
    }

    public void setFrequencyPerDay(String frequencyPerDay) {
        this.frequencyPerDay = frequencyPerDay;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
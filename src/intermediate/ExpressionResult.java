package intermediate;

public class ExpressionResult {
    private String type;
    private String place;

    public ExpressionResult(String type, String place) {
        this.type = type;
        this.place = place;
    }

    public String getType() { return type; }
    public String getPlace() { return place; }
}

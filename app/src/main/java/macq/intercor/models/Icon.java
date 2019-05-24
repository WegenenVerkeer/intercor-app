package macq.intercor.models;

import java.util.Map;

public class Icon {
    private String message;
    private Boolean selected;
    private String detail;
    private String type;
    private String urlIcon;
    private String id;
    private int position;

    /******************
     ** CONSTRUCTORS **
     ******************/

    private Icon(String detail, String message, String type, String urlIcon, String id, int position) {
        this.selected = false;
        this.message = message;
        this.detail = detail;
        this.type = type;
        this.urlIcon = urlIcon;
        this.id = id;
        this.position = position;
    }

    public Icon(Map<String, Object> data, String id) {
        this(
                String.valueOf(data.get("detail")),
                String.valueOf(data.get("message")),
                String.valueOf(data.get("type")),
                String.valueOf(data.get("urlIcon")),
                id,
                Integer.parseInt(String.valueOf(data.get("position")))
        );
    }

    public Icon() { }

    /*****************
     ** GET METHODS **
     *****************/

    public String getDetail() { return detail; }

    public String getId() { return id; }

    public String getMessage() { return message; }

    public String getType() { return type; }

    public String getUrlIcon() { return urlIcon; }

    public Boolean getSelected() { return selected; }

    public int getPosition() { return position; }

    /*****************
     ** SET METHODS **
     *****************/

    public void setDetail(String detail) { this.detail = detail; }

    public void setId(String id) { this.id = id; }

    public void setMessage(String message) { this.message = message; }

    public void setPosition(int position) { this.position = position; }

    public void setSelected(Boolean selected) { this.selected = selected; }

    public void setType(String type) { this.type = type; }

    public void setUrlIcon(String urlIcon) { this.urlIcon = urlIcon; }
}

package pl.pollub.myrecommendation.models;

public class Category {
    protected String id;
    protected String name;
    protected String icon;
    protected boolean isSelected = false;

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void toggleSelection(){
        if(isSelected) isSelected = false;
        else isSelected = true;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    //to display object as a string in spinner
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Category){
            Category c = (Category ) obj;
            if(c.getName().equals(name) && c.getId()== id ) return true;
        }

        return false;
    }

}

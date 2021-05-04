package com.example.displaymap;

public class Model {
    private boolean isSelected;
    private String item;
    String getItem() {
        return item;
    }
    void setItem(String item) {
        this.item = item;
    }
    boolean getSelected() {
        return isSelected;
    }
    String isSelected() {
        if(isSelected) return "1";
        else return "0";
    }
    void setSelected(boolean selected) {
        isSelected = selected;
    }
}

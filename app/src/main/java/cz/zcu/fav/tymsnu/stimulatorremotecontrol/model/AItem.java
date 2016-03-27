package cz.zcu.fav.tymsnu.stimulatorremotecontrol.model;


public abstract class AItem {

    protected String name;

    public boolean loaded;
    public boolean selected;
    public boolean changed;

    public AItem(String name) {
        if (name.isEmpty())
            throw new IllegalArgumentException();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

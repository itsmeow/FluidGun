package dev.itsmeow.fluidgun.content;

public enum ScrollMode {

    BOTH,
    OUT,
    IN;

    public static ScrollMode get(int value) {
        if(value >= 0 && value < values().length) {
            return values()[value];
        }
        return BOTH;
    }

    public ScrollMode next() {
        int i = this.ordinal() + 1;
        if(i >= values().length) {
            i = 0;
        }
        return values()[i];
    }

    public ScrollMode prev() {
        int i = this.ordinal() - 1;
        if(i < 0) {
            i = values().length - 1;
        }
        return values()[i];
    }

}

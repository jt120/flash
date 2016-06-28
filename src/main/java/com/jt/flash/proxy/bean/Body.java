package com.jt.flash.proxy.bean;

/**
 * since 2016/6/25.
 */
public class Body {

    public final int red;
    public final int blue;

    public Body(int red, int blue) {
        this.red = red;
        this.blue = blue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Body body = (Body) o;

        if (blue != body.blue) return false;
        if (red != body.red) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + blue;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Body{");
        sb.append("red=").append(red);
        sb.append(", blue=").append(blue);
        sb.append('}');
        return sb.toString();
    }
}

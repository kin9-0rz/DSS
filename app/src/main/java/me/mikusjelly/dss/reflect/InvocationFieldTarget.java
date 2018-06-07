package me.mikusjelly.dss.reflect;

import java.util.ArrayList;

public class InvocationFieldTarget {
    private String className;
    private ArrayList<String> fieldNames;

    /**
     * @param className com.ms.plugin.bm
     * @param fieldNames [a, b, c]
     */
    public InvocationFieldTarget(String className, ArrayList<String> fieldNames) {
        this.className = className;
        this.fieldNames = fieldNames;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ArrayList<String> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(ArrayList<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    @Override
    public String toString() {
        return "InvocationTarget{" +
                ", className='" + className + '\'' +
                ", fieldNames='" + fieldNames + '\'' +
                '}';
    }
}

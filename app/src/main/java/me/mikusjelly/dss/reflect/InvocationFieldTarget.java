package me.mikusjelly.dss.reflect;

import java.util.ArrayList;

public class InvocationFieldTarget {
    private String className;
    private ArrayList<String> fieldNames;
    private Class<?> fileType;

    /**
     * @param className com.ms.plugin.bm
     * @param fieldNames [a, b, c]
     */
    public InvocationFieldTarget(String className, ArrayList<String> fieldNames) {
        this.className = className;
        this.fieldNames = fieldNames;
//        this.fileType = fileType;
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

    public Class<?> getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return "InvocationFieldTarget{" +
                "className='" + className + '\'' +
                ", fieldNames=" + fieldNames +
                ", fileType=" + fileType +
                '}';
    }
}

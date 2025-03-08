package com.example.java2json;

import java.io.Serializable;
import java.util.List;

public class FirstClass  implements Serializable {

    /**
     * 数量
     */
    private Integer num;

    /**
     * 二级类列表
     */
    private List<SencodClass> sencodClassList;

    /**
     * 二级类
     */
    private SencodClass sencodClass;

    /**
     * 名称列表
     */
    private List<String> nameList;
}

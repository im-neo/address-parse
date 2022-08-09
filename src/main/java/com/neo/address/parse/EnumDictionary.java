package com.neo.address.parse;


import java.io.Serializable;

/**
 * 数据字典
 *
 * @author Neo
 * @since 2022/6/24 09:07
 */
public class EnumDictionary implements Serializable {
    private static final long serialVersionUID = -1113764940965416458L;

    private Integer code;

    private String name;

    private String message;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package com.neo.address.parse;

/**
 * 地区级别枚举
 *
 * @author Neo
 * @since 2022/6/24 10:50
 */
public enum AreaEnum implements BaseEnum {
    PROVINCE(0, "省/直辖市"),
    CITY(1, "市/州"),
    AREA(2, "县/区 "),
    TOWN(3, "乡/镇"),
    VILLAGE(4, "村/社区"),
    ;


    private final int code;

    private final String message;


    AreaEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
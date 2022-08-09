package com.neo.address.parse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 解析结果
 *
 * @author Neo
 * @since 2022/8/9 13:56
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    private String name;

    private String province;
    private String city;
    private String area;
    private String detail;

    private String zipCode;

    private String mobile;
    private String phone;

    private AreaEnum type;
    private String address;


    public static ParseResult assign(ParseResult target, ParseResult source) {
        if (Objects.isNull(target) && Objects.nonNull(source)) {
            return source;
        }
        if (Objects.nonNull(target) && Objects.isNull(source)) {
            return target;
        }

        target.setName(StringUtils.isBlank(source.getName()) ? target.getName() : source.getName());

        target.setProvince(StringUtils.isBlank(source.getProvince()) ? target.getProvince() : source.getProvince());
        target.setCity(StringUtils.isBlank(source.getCity()) ? target.getCity() : source.getCity());
        target.setArea(StringUtils.isBlank(source.getArea()) ? target.getArea() : source.getArea());
        target.setDetail(StringUtils.isBlank(source.getDetail()) ? target.getDetail() : source.getDetail());

        target.setZipCode(StringUtils.isBlank(source.getZipCode()) ? target.getZipCode() : source.getZipCode());

        target.setMobile(StringUtils.isBlank(source.getMobile()) ? target.getMobile() : source.getMobile());
        target.setPhone(StringUtils.isBlank(source.getPhone()) ? target.getPhone() : source.getPhone());

        target.setType(Objects.nonNull(source.getType()) ? target.getType() : source.getType());
        target.setAddress(StringUtils.isBlank(source.getAddress()) ? target.getAddress() : source.getAddress());

        return target;
    }

    public String format() {
        return String.format("姓名：%s，电话：%s，手机：%s，省：%s，市：%s，区：%s，详细地址：%s，类型：%s",
                this.getName(), this.getPhone(), this.getMobile(), this.getProvince(), this.getCity(), this.getArea(), this.getDetail(), this.getType());
    }

}
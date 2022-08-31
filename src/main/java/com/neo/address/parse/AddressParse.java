package com.neo.address.parse;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Pair;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 收货地址智能解析主类
 *
 * @author Neo
 * @since 2022/6/24 10:50
 */
@Slf4j
public class AddressParse {

    /**
     * 自定义去除关键字，可自行添加
     */
    public static final List<String> EXCLUDE_KEYS = Lists.newArrayList("详细地址", "收货地址", "收件地址", "地址", "所在地区", "地区",
            "姓名", "收货人", "收件人", "联系人", "收", "邮编",
            "联系电话", "联系电話", "电话", "电話", "联系人手机号码", "手机号码", "手机号",
            "自治区直辖县级行政区划", "省直辖县级行政区划");

    /**
     * 特殊符号正则
     */
    public static final String SPECIAL_SYMBOL_REGEX = "[`~!@#$^&*()=|{}':;',\\[\\]\\.<>/?~！@#￥……&*（）——|{}【】‘；：”“’。，、？]";
    /**
     * 手机号正则
     */
    public static final Pattern MOBILE_PATTERN = Pattern.compile("(86-[1][3-9][0-9]{9})|(86[1][3-9][0-9]{9})|([1][3-9][0-9]{9})");
    /**
     * 电话号码正则
     */
    public static final Pattern PHONE_PATTERN = Pattern.compile("(([0-9]{3,4}-)[0-9]{7,8})|([0-9]{12})|([0-9]{11})|([0-9]{10})|([0-9]{9})|([0-9]{8})|([0-9]{7})");
    /**
     * 邮编正则
     */
    public static final Pattern ZIP_CODE_PATTERN = Pattern.compile("([0-9]{6})");

    /**
     * 省市区县数据文件路径（根据实际情况调整）
     */
    public static final String FILE_PATH = "/address-parse/china-area.json";

    public static final String EMPTY = "", BLANK = " ";

    public static List<AreaTree> PROVINCE_LIST, CITY_LIST, AREA_LIST;


    static {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<String> lines = FileUtil.readUtf8Lines(AddressParse.class.getResource(FILE_PATH));
        String file = String.join(EMPTY, lines);

        Gson gson = new Gson();
        List<AreaTree> areas = gson.fromJson(file, new TypeToken<List<AreaTree>>() {}.getType());
        Iterator<AreaTree> iterator = areas.iterator();
        while (iterator.hasNext()) {
            AreaTree next = iterator.next();
            if (AreaEnum.CITY.getCode().equals(next.getLevel()) || AreaEnum.DISTRICT.getCode().equals(next.getLevel())) {
                if (StringUtils.length(next.getName()) <= 2) {
                    iterator.remove();
                }
                if (StringUtils.equals(next.getName(), "市辖区")) {
                    iterator.remove();
                }
            }
        }

        areas = TreeUtils.buildPath(areas, o -> Objects.equals(o.getParentCode(), 0L));
        Map<Integer, List<AreaTree>> areaMapping = areas.stream().collect(Collectors.groupingBy(AreaTree::getLevel));

        PROVINCE_LIST = areaMapping.get(AreaEnum.PROVINCE.getCode());
        CITY_LIST = areaMapping.get(AreaEnum.CITY.getCode());
        AREA_LIST = areaMapping.get(AreaEnum.DISTRICT.getCode());

        log.info("地址解析器初始化耗时：{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }


    /**
     * 解析主入口
     *
     * @author Neo
     * @since 2021/3/25 14:44
     */
    public static List<ParseResult> parse(String address) {
        if (StringUtils.isBlank(address)) {
            return Collections.EMPTY_LIST;
        }

        // 地址清洗
        address = cleanAddress(address);

        // 提取手机号
        String mobile = parseByPattern(MOBILE_PATTERN, address);
        address = StringUtils.replace(address, mobile, BLANK);

        // 提取电话号码
        String phone = parseByPattern(PHONE_PATTERN, address);
        address = StringUtils.replace(address, phone, BLANK);

        // 提取邮编
        String zipCode = parseByPattern(ZIP_CODE_PATTERN, address);
        address = StringUtils.replace(address, zipCode, BLANK);

        // 提取名字
        Pair<String, String> nameInfo = parseName(EMPTY, address);
        address = nameInfo.getValue();

        List<ParseResult> results = parseArea(address);


        for (ParseResult r : results) {
            r.setMobile(mobile);
            r.setPhone(phone);
            r.setZipCode(StringUtils.isBlank(r.getZipCode()) ? zipCode : r.getZipCode());
            r.setName(StringUtils.isBlank(r.getName()) ? nameInfo.getKey() : r.getName());
        }

        if (CollectionUtils.isEmpty(results)) {
            nameInfo = parseName(EMPTY, address);
            results.add(ParseResult.builder().name(nameInfo.getKey()).address(nameInfo.getValue()).build());
        }

        return results;
    }


    public static List<ParseResult> parseArea(String address) {
        List<ParseResult> results = new ArrayList<>();
        if (StringUtils.isBlank(address)) {
            return results;
        }

        // 清除两个以上的空格
        address = address.replaceAll(" {2,}", BLANK);

        // 正向解析
        results.addAll(parseByProvince(address));

        // 通过城市逆向解析
        results.addAll(parseByCity(address));

        //通过地区逆向解析
        results.addAll(parseByArea(address));


        return results;
    }

    /**
     * 通过地区逆向解析
     *
     * @author Neo
     * @since 2021/3/25 9:29
     */
    public static List<ParseResult> parseByArea(String addressBase) {
        List<ParseResult> results = new ArrayList<>();
        ParseResult result;
        String address = addressBase;
        for (AreaTree area : AREA_LIST) {
            if (StringUtils.length(area.getName()) < 2) {
                continue;
            }
            MatchResult match = match(area, address);
            if (!match.isMatch()) {
                continue;
            }

            result = new ParseResult();
            result.setProvince(area.getParent().getParent().getName());
            result.setCity(area.getParent().getName());
            result.setArea(area.getName());
            result.setZipCode(area.getZipCode());
            result.setType(AreaEnum.DISTRICT);

            // 左侧排除省份城市名剩下的内容识别为姓名
            String leftAddress = StringUtils.left(address, match.getIndex());
            MatchResult provinceMatch = null, cityMatch = null;
            if (StringUtils.isNotBlank(leftAddress)) {
                provinceMatch = match(area.getParent().getParent(), leftAddress);
                leftAddress = provinceMatch.isMatch() ? StringUtils.remove(leftAddress, provinceMatch.getMatchName()) : leftAddress;
            }

            if (StringUtils.isNotBlank(leftAddress)) {
                cityMatch = match(area.getParent(), leftAddress);
                leftAddress = cityMatch.isMatch() ? StringUtils.remove(leftAddress, cityMatch.getMatchName()) : leftAddress;
            }

            if (StringUtils.isNotBlank(leftAddress)) {
                result.setName(StringUtils.trim(leftAddress));
            }

            // 出现同省地区匹配错误处理，广东省惠来县惠城镇 如不经处理匹配到 广东省惠州市惠城区
            if (Objects.nonNull(provinceMatch) && Objects.nonNull(cityMatch)
                    && StringUtils.isNotBlank(provinceMatch.getMatchName()) && StringUtils.isNotBlank(cityMatch.getMatchName())) {
                List<ParseResult> tempResult = parseByArea(StringUtils.left(address, match.getIndex()));
                if (CollectionUtils.isNotEmpty(tempResult)) {
                    result = ParseResult.assign(result, CollectionUtil.getFirst(tempResult.iterator()));
                    address = StringUtils.right(address, match.getIndex());


                    if (StringUtils.isBlank(result.getArea())) {
                        address = parseAreaByCity(area.getParent(), result, address);
                    }
                }
            }

            if (StringUtils.isNotBlank(result.getProvince()) && StringUtils.isNotBlank(result.getCity()) && StringUtils.isNotBlank(result.getArea())) {
                address = StringUtils.substring(address, match.getIndex() + match.getMatchNameLength());
                result.setDetail(StringUtils.trim(address));
                results.add(result);
                break;
            }
        }

        return results;
    }


    /**
     * 通过城市逆向解析
     *
     * @author Neo
     * @since 2021/3/25 9:19
     */
    public static List<ParseResult> parseByCity(String addressBase) {
        List<ParseResult> results = new ArrayList<>();
        ParseResult result;
        String address = addressBase;
        for (AreaTree city : CITY_LIST) {
            // 排除重庆市下的 500200:县
            if (StringUtils.length(city.getName()) < 2) {
                continue;
            }
            MatchResult match = match(city, address);
            if (!match.isMatch()) {
                continue;
            }

            result = new ParseResult();
            result.setProvince(city.getParent().getName());
            result.setCity(city.getName());
            result.setZipCode(city.getZipCode());
            result.setType(AreaEnum.CITY);

            // 将城市左侧的部分排除省份后剩下的内容识别为姓名
            String leftAddress = StringUtils.left(address, match.getIndex());
            if (StringUtils.isNotBlank(leftAddress)) {
                if (StringUtils.contains(leftAddress, city.getParent().getName())) {
                    leftAddress = StringUtils.remove(leftAddress, city.getParent().getName());
                } else {
                    leftAddress = StringUtils.remove(leftAddress, city.getParent().getShortName());
                }
                if (StringUtils.isNotBlank(leftAddress)) {
                    result.setName(StringUtils.trim(leftAddress));
                }
            }
            address = StringUtils.substring(address, match.getIndex() + match.getMatchNameLength());
            address = parseAreaByCity(city, result, address);

            result.setDetail(StringUtils.trim(address));

            if (StringUtils.isNotBlank(result.getProvince()) && StringUtils.isNotBlank(result.getCity())) {
                results.add(result);
            }

        }
        return results;
    }


    /**
     * 解析省份
     *
     * @author Neo
     * @since 2021/3/24 16:55
     */
    public static List<ParseResult> parseByProvince(String addressBase) {
        List<ParseResult> results = new ArrayList<>();
        ParseResult result;
        String address = addressBase;

        for (AreaTree province : PROVINCE_LIST) {
            result = new ParseResult();
            MatchResult match = match(province, address);

            if (match.isMatch()) {
                result.setProvince(province.getName());
                result.setZipCode(province.getZipCode());
                result.setType(AreaEnum.PROVINCE);

                address = StringUtils.remove(address, match.getMatchName());
            }


            // 如果省份不是第一位 在省份之前的字段识别为名称
            if (match.getIndex() > 0) {
                result.setName(StringUtils.trim(StringUtils.substring(address, 0, match.getIndex())));
                address = StringUtils.remove(address, result.getName());
            }

            if (StringUtils.isNotBlank(result.getProvince())) {
                address = parseCityByProvince(province, result, address);
            }

            if (StringUtils.isNotBlank(result.getProvince())) {
                address = parseAreaByProvince(province, result, address);
            }

            if (StringUtils.isNotBlank(result.getZipCode())) {
                result.setDetail(StringUtils.trim(address));
            }

            if (StringUtils.isNotBlank(result.getProvince())) {
                results.add(result);
            }
        }

        return results;
    }

    /**
     * 解析地区通过省份
     *
     * @author Neo
     * @since 2021/3/24 16:59
     */
    public static String parseAreaByProvince(AreaTree province, ParseResult result, String address) {
        for (AreaTree city : province.getChildren()) {
            for (AreaTree area : city.getChildren()) {
                MatchResult match = match(area, address);
                if (!match.isMatch() || match.getIndex() > 5) {
                    continue;
                }

                result.setCity(city.getName());
                result.setArea(area.getName());
                result.setZipCode(area.getZipCode());

                address = StringUtils.substring(address, match.getIndex() + match.getMatchNameLength());
            }
        }
        return address;
    }


    /**
     * 解析城市通过省份
     *
     * @author Neo
     * @since 2021/3/24 16:54
     */
    public static String parseCityByProvince(AreaTree province, ParseResult result, String address) {
        for (AreaTree city : province.getChildren()) {
            MatchResult match = match(city, address);
            if (!match.isMatch()) {
                continue;
            }

            result.setCity(city.getName());
            result.setZipCode(city.getZipCode());

            address = StringUtils.remove(address, match.getMatchName());
            address = parseAreaByCity(city, result, address);
        }

        return address;
    }


    /**
     * 提取地区通过城市
     *
     * @author Neo
     * @since 2021/3/24 16:49
     */
    public static String parseAreaByCity(AreaTree city, ParseResult result, String address) {
        for (AreaTree area : city.getChildren()) {
            MatchResult match = match(area, address);
            if (!match.isMatch()) {
                continue;
            }

            result.setArea(match.getMatchName());
            result.setZipCode(area.getZipCode());

            address = StringUtils.remove(address, match.getMatchName());
        }
        return address;
    }


    /**
     * 1. 地址清洗
     *
     * @author Neo
     * @since 2021/3/24 15:44
     */
    public static String cleanAddress(String address) {
        address = address.replaceAll("\\r\\n", BLANK)
                .replaceAll("\\n", BLANK)
                .replaceAll("\\t", BLANK)
                .replaceAll(" {2,}", BLANK)
                .replaceAll("(\\d{3})-(\\d{4})-(\\d{4})", "$1$2$3")
                .replaceAll("(\\d{3}) (\\d{4}) (\\d{4})", "$1$2$3")
        ;


        for (String search : EXCLUDE_KEYS) {
            address = address.replaceAll(search, BLANK);
        }

        address = address.replaceAll(SPECIAL_SYMBOL_REGEX, BLANK);

        return address;
    }


    /**
     * 通过正则解析数据
     *
     * @author Neo
     * @since 2021/3/24 15:45
     */
    public static String parseByPattern(Pattern pattern, String address) {
        if (Objects.isNull(pattern) || StringUtils.isBlank(address)) {
            return EMPTY;
        }
        Matcher matcher = pattern.matcher(address);
        return matcher.find() ? matcher.group(0) : EMPTY;
    }


    /**
     * 解析收货人姓名
     *
     * @author Neo
     * @since 2021/3/24 15:45
     */
    public static Pair<String, String> parseName(String name, String address) {
        if (StringUtils.isNotBlank(name)) {
            return new Pair<>(name, address);
        }


        List<String> items = Splitter.on(BLANK).trimResults().omitEmptyStrings().splitToList(address);
        if (CollectionUtils.size(items) < 2) {
            return new Pair<>(name, address);
        }
        String parseName = items.get(0);
        for (String item : items) {
            if (length(parseName) > length(item)) {
                parseName = item;
            }
        }

        String finalParseName = parseName;
        address = items.stream().filter(i -> !StringUtils.equals(i, finalParseName)).collect(Collectors.joining(BLANK));

        return new Pair<>(parseName, address);
    }

    /**
     * 统计字符串长度
     * 汉字算两位，英文一位
     *
     * @author Neo
     * @since 2021/3/24 15:40
     */
    public static int length(String str) {
        int result = 0;
        if (Objects.isNull(str) || EMPTY.equals(str)) {
            return result;
        }

        for (char c : str.toCharArray()) {
            result += c >= 0x0391 && c <= 0xFFE5 ? 2 : c <= 0x00FF ? 1 : 0;
        }
        return result;
    }


    /**
     * 地区节点匹配
     *
     * @author Neo
     * @since 2021/3/24 15:56
     */
    public static MatchResult match(AreaTree area, String address) {
        int index = StringUtils.indexOf(address, area.getName());
        boolean matchShort = false;

        if (index == -1) {
            index = StringUtils.indexOf(address, area.getShortName());
            matchShort = index > -1;
        }
        String matchName = index > -1 ? matchShort ? area.getShortName() : area.getName() : EMPTY;
        return new MatchResult(matchShort, matchName, index);
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchResult {
        private boolean match;
        private boolean matchShort;
        private String matchName;
        private int matchNameLength;
        private int index;

        public MatchResult(boolean matchShort, String matchName, int index) {
            this.matchShort = matchShort;
            this.matchName = matchName;
            this.index = index;
        }


        public boolean isMatch() {
            return this.index > -1;
        }

        public int getMatchNameLength() {
            return StringUtils.length(this.matchName);
        }
    }
}

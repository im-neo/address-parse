package com.neo.address.parse;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 枚举通用接口
 *
 * @author Neo
 * @since 2022/6/24 09:07
 */
public interface BaseEnum {

    String CODE = "code";

    String DESC = "desc";

    String ENUM_CLASS_TAIL = "Enum";

    String DEFAULT_SEPARATOR = ",";

    /**
     * 编码
     *
     * @return
     */
    Integer getCode();

    /**
     * 描述
     *
     * @return
     */
    String getDesc();

    /**
     * 是否需要以字典的形式被加载，默认：false
     *
     * @return
     */
    default boolean isLoadDictionary() {
        return false;
    }

    /**
     * 字典名称
     *
     * @return
     */
    default String dictionaryName() {
        return this.getClass().getSimpleName();
    }


    /**
     * 是否使用默认方式生成字典，默认：false
     *
     * @return
     */
    default boolean isDefaultAssembly() {
        return false;
    }

    /**
     * 自定义字典数据
     *
     * @return
     */
    default List<EnumDictionary> dictionaries() {
        return Collections.EMPTY_LIST;
    }

    /**
     * 当前对象的 code 是否 和参数中的 code 相等
     *
     * @param code
     * @return
     */
    default boolean equalsCode(Integer code) {
        return Objects.equals(this.getCode(), code);
    }

    /**
     * 当前对象是否和已知对象不等
     *
     * @param e
     * @return
     */
    default boolean notEquals(BaseEnum e) {
        return this != e;
    }


    // =================================【上为接口】=================================
    // ============================================================================
    // =================================【下为方法】=================================


    /**
     * 通过 code 获取指定 枚举类型中的 枚举对象
     *
     * @param clazz
     * @param code
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> T getByCode(Class<T> clazz, Integer code) {
        if (Objects.isNull(clazz) || Objects.isNull(code)) {
            return null;
        }
        for (T e : clazz.getEnumConstants()) {
            if (e.equalsCode(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 通过 code 获取指定 枚举类型中的 detail
     *
     * @param enumClass
     * @param code
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> String getMessageByCode(Class<T> enumClass, Integer code) {
        BaseEnum e = getByCode(enumClass, code);
        if (null == e) {
            return null;
        }
        return e.getDesc();
    }

    /**
     * 枚举 转 List
     *
     * @param enums
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> List<Map<String, String>> toList(T[] enums) {
        if (ArrayUtils.isEmpty(enums)) {
            return Collections.EMPTY_LIST;
        }
        List<Map<String, String>> result = new ArrayList<>(enums.length);
        Map<String, String> map;
        for (T e : enums) {
            map = new HashMap<>(2);
            map.put(CODE, String.valueOf(e.getCode()));
            map.put(DESC, e.getDesc());
            result.add(map);
        }
        return result;
    }


    /**
     * 枚举 转 Map
     *
     * @param enums
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> Map<Integer, String> toMap(T[] enums) {
        if (ArrayUtils.isEmpty(enums)) {
            return Collections.EMPTY_MAP;
        }
        Map<Integer, String> result = new HashMap<>(enums.length);
        for (T e : enums) {
            result.put(e.getCode(), e.getDesc());
        }
        return result;
    }

    /**
     * 转枚举 Map
     *
     * @param enums
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> Map<Integer, T> toEnumMap(T[] enums) {
        if (ArrayUtils.isEmpty(enums)) {
            return Collections.EMPTY_MAP;
        }
        Map<Integer, T> result = new HashMap<>(enums.length);
        for (T e : enums) {
            result.put(e.getCode(), e);
        }
        return result;
    }

    /**
     * 枚举转字典集合
     *
     * @param enums
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> List<EnumDictionary> toDictionaries(T[] enums) {
        if (ArrayUtils.isEmpty(enums)) {
            return Collections.EMPTY_LIST;
        }
        List<EnumDictionary> result = new ArrayList<>(enums.length);
        for (T e : enums) {
            result.add(toDictionary(e));
        }
        return result;
    }

    /**
     * 枚举转字典
     *
     * @param e
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> EnumDictionary toDictionary(T e) {
        if (Objects.isNull(e)) {
            return null;
        }
        EnumDictionary result = new EnumDictionary();
        result.setCode(e.getCode());
        result.setName(e.toString());
        result.setMessage(e.getDesc());
        return result;
    }


    /**
     * 清除枚举类尾巴
     *
     * @param clazz
     * @return
     */
    static String clearEnumClassTail(Class<?> clazz) {
        return Objects.isNull(clazz) ? StringUtils.EMPTY : clearEnumClassTail(clazz.getSimpleName());
    }

    /**
     * 清除枚举类尾巴
     *
     * @param className
     * @return
     */
    static String clearEnumClassTail(String className) {
        return StringUtils.removeEndIgnoreCase(className, ENUM_CLASS_TAIL);
    }


    /**
     * 通过 code 获取实例
     *
     * @param map
     * @param code
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> T of(Map<Integer, T> map, Integer code) {
        if (MapUtils.isEmpty(map) || Objects.isNull(code)) {
            return null;
        }
        return map.get(code);
    }

    /**
     * 通过 code 获取详情
     *
     * @param map
     * @param code
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> String getMessageByCode(Map<Integer, T> map, Integer code) {
        T t = of(map, code);
        return Objects.nonNull(t) ? t.getDesc() : StringUtils.EMPTY;
    }

    /**
     * 通过 codes 批量获取详情
     *
     * @param map
     * @param codes
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> String getMessagesByCodes(Map<Integer, T> map, String codes) {
        return getMessagesByCodes(map, codes, DEFAULT_SEPARATOR);
    }

    /**
     * 通过 codes 批量获取详情
     *
     * @param map
     * @param codes
     * @param separator
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> String getMessagesByCodes(Map<Integer, T> map, String codes, String separator) {
        if (StringUtils.isBlank(codes) || Objects.isNull(separator)) {
            return StringUtils.EMPTY;
        }
        List<String> codeList = Splitter.on(separator).trimResults().omitEmptyStrings().splitToList(codes);
        return getMessagesByCodes(map, codeList);
    }

    /**
     * 通过 codes 批量获取详情
     *
     * @param map
     * @param codes
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> String getMessagesByCodes(Map<Integer, T> map, List<String> codes) {
        return Joiner.on(DEFAULT_SEPARATOR).join(getMessageListByCodes(map, codes));
    }


    /**
     * 通过 codes 批量获取详情
     *
     * @param map
     * @param codes
     * @param <T>
     * @return
     */
    static <T extends BaseEnum> List<String> getMessageListByCodes(Map<Integer, T> map, List<String> codes) {
        List<T> enums = getEnumsByCodes(map, codes);
        if (CollectionUtils.isEmpty(enums)) {
            return Collections.EMPTY_LIST;
        }
        return enums.stream().map(BaseEnum::getDesc).collect(Collectors.toList());
    }

    /**
     * 通过 codes 批量获取枚举
     *
     * @author Neo
     * @since 2021/12/15 11:01
     */
    static <T extends BaseEnum> List<T> getEnumsByCodes(Map<Integer, T> map, String codes) {
        if (StringUtils.isBlank(codes)) {
            return Collections.EMPTY_LIST;
        }
        return getEnumsByCodes(map, codes, DEFAULT_SEPARATOR);
    }

    /**
     * 通过 codes 批量获取枚举
     *
     * @author Neo
     * @since 2021/12/15 11:01
     */
    static <T extends BaseEnum> List<T> getEnumsByCodes(Map<Integer, T> map, String codes, String separator) {
        if (StringUtils.isBlank(codes) || Objects.isNull(separator)) {
            return Collections.EMPTY_LIST;
        }
        List<String> codeList = Splitter.on(separator).trimResults().omitEmptyStrings().splitToList(codes);
        return getEnumsByCodes(map, codeList);
    }

    /**
     * 通过 codes 批量获取枚举
     *
     * @author Neo
     * @since 2021/12/15 11:01
     */
    static <T extends BaseEnum> List<T> getEnumsByCodes(Map<Integer, T> map, List<String> codes) {
        if (CollectionUtils.isEmpty(codes)) {
            return Collections.EMPTY_LIST;
        }
        List<T> result = new ArrayList<>();
        for (String code : codes) {
            T e = map.get(Integer.valueOf(code));
            if(Objects.nonNull(e)){
                result.add(e);
            }
        }
        return result;
    }
}

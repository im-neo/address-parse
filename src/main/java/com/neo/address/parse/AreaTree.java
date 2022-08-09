package com.neo.address.parse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 中国行政地区
 *
 * @author Neo
 * @since 2022/6/24 09:04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"parent", "children"})
public class AreaTree implements ITree<AreaTree, Long> {
    private static final long serialVersionUID = -32407026969579150L;

    /**
     * 层级
     */
    private Integer level;
    /**
     * 父级行政代码
     */
    private Long parentCode;
    /**
     * 行政代码
     */
    private Long areaCode;
    /**
     * 邮政编码
     */
    private String zipCode;
    /**
     * 区号
     */
    private String cityCode;
    /**
     * 名称
     */
    private String name;
    /**
     * 简称
     */
    private String shortName;

    /**
     * 父节点
     */
    private AreaTree parent;

    /**
     * 子节点
     */
    private List<AreaTree> children;


    @Override
    public Long id() {
        return areaCode;
    }

    @Override
    public Long parentId() {
        return this.parentCode;
    }

    @Override
    public void parent(AreaTree parent) {
        this.parent = parent;
    }

    @Override
    public void children(List<AreaTree> children) {
        this.children = children;
    }

    public List<AreaTree> getChildren() {
        return Objects.isNull(this.children) ? Collections.EMPTY_LIST : this.children;
    }
}

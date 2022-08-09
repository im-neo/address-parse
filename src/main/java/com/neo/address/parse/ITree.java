package com.neo.address.parse;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 树接口
 *
 * @author Neo
 * @since 2022/6/24 09:03
 */
public interface ITree<T, K> {


    /**
     * 节点ID
     *
     * @return
     */
    K id();


    /**
     * 父节点ID
     *
     * @return
     */
    K parentId();

    /**
     * 设置父节点
     * 
     * @param parent
     */
    default void parent(T parent){
    }

    /**
     * 用于拼接 path 的属性
     * 
     * child.path(parentNode.path() + spliterator + child.pathProperty());
     * 
     * @return
     */
    default String pathProperty(){
        return StringUtils.EMPTY;
    }
    
    /**
     * 获取节点名称
     *
     * @return
     */
    default String path() {
        return StringUtils.EMPTY;
    }

    /**
     * 设置节点名称
     *
     * @param path
     */
    default void path(String path) {
    }

    /**
     * 排序
     *
     * @return
     */
    default Integer index() {
        return 0;
    }

    /**
     * 设置子节点
     *
     * @param children
     */
    default void children(List<T> children) {
    }


}

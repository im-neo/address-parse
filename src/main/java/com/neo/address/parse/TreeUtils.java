package com.neo.address.parse;


import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 将扁平数据构造成树结构
 *
 * @author Neo
 * @since 2022/6/24 09:09
 */
public class TreeUtils {

    public static final String DEFAULT_SPLITERATOR = "/";

    private TreeUtils() {
    }


    enum Model {
        TREE, PATH, TREE_AND_PATH
    }


    /**
     * 构造树的入口方法
     *
     * @param originData    数据列表
     * @param rootPredicate 根节点条件
     * @param <T>           implements ITree
     * @param <K>           implements Serializable
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> buildTree(List<T> originData, Predicate<T> rootPredicate) {
        return baseBuild(Model.TREE, originData, null, rootPredicate);
    }


    /**
     * 构建树路径，不改变数据结构
     *
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> buildPath(List<T> originData, Predicate<T> rootPredicate) {
        return buildPath(originData, DEFAULT_SPLITERATOR, rootPredicate);
    }

    /**
     * 构建树路径，不改变数据结构
     *
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> buildPath(List<T> originData, CharSequence spliterator, Predicate<T> rootPredicate) {
        return baseBuild(Model.PATH, originData, spliterator, rootPredicate);
    }

    /**
     * 构建树和路径
     *
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> buildTreeAndPath(List<T> originData, Predicate<T> rootPredicate) {
        return buildTreeAndPath(originData, DEFAULT_SPLITERATOR, rootPredicate);
    }

    /**
     * 构建树和路径
     *
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> buildTreeAndPath(List<T> originData, CharSequence spliterator, Predicate<T> rootPredicate) {
        return baseBuild(Model.TREE_AND_PATH, originData, spliterator, rootPredicate);
    }


    /**
     * 树构建的基础方法
     *
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> baseBuild(Model model,
                                                                                    List<T> originData,
                                                                                    CharSequence spliterator,
                                                                                    Predicate<T> rootPredicate) {
        if (Objects.isNull(model) || CollectionUtils.isEmpty(originData) || Objects.isNull(rootPredicate)) {
            return Collections.EMPTY_LIST;
        }

        List<T> result = new ArrayList<>(CollectionUtils.size(originData));

        List<T> roots = originData.stream().filter(rootPredicate).sorted(Comparator.comparing(T::index)).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(roots)) {
            return Collections.EMPTY_LIST;
        }

        // 删除根节点，避免重复遍历
        originData.removeAll(roots);

        switch (model) {
            case TREE:
                roots.forEach(r -> result.add(buildTree(r, originData)));
                break;
            case PATH:
                roots.forEach(r -> result.addAll(buildPath(r, originData, spliterator)));
                break;
            case TREE_AND_PATH:
                roots.forEach(r -> result.add(buildTreeAndPath(r, originData, spliterator)));
                break;
            default:
                throw new RuntimeException();
        }
        return result;
    }


    /**
     * 构建子节点
     *
     * @param parentNode 父节点
     * @param originData 数据列表
     * @param <T>        implements ITree
     * @param <K>        implements Serializable
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> T buildTree(T parentNode, List<T> originData) {
        List<T> childrenNode = new ArrayList<>();

        List<T> children = filterByParentId(parentNode.id(), originData);

        if (CollectionUtils.isNotEmpty(children)) {
            // 删除节点，避免重复遍历
            originData.removeAll(children);
        }
        for (T child : children) {
            child.parent(parentNode);
            childrenNode.add(buildTree(child, originData));
        }
        parentNode.children(childrenNode);
        return parentNode;
    }


    /**
     * 构建子节点路径
     *
     * @author Neo
     * @since 2022/8/9 13:58
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> buildPath(T parentNode, List<T> originData, CharSequence spliterator) {
        List<T> result = new ArrayList<>();

        List<T> children = filterByParentId(parentNode.id(), originData);

        result.add(parentNode);

        if (CollectionUtils.isEmpty(children)) {
            return result;
        }

        // 删除节点，避免重复遍历
        originData.removeAll(children);

        for (T child : children) {
            child.parent(parentNode);
            child.path(parentNode.path() + spliterator + child.pathProperty());
            result.addAll(buildPath(child, originData, spliterator));
        }
        parentNode.children(children);
        return result;
    }

    /**
     * 构建树和路径
     *
     * @author Neo
     * @since 2022/8/9 13:57
     */
    public static <T extends ITree<T, K>, K extends Serializable> T buildTreeAndPath(T parentNode, List<T> originData, CharSequence spliterator) {
        List<T> children = filterByParentId(parentNode.id(), originData);

        if (CollectionUtils.isNotEmpty(children)) {
            originData.removeAll(children);
        }

        for (T child : children) {
            child.parent(parentNode);
            child.path(parentNode.path() + spliterator + child.path());
            buildTreeAndPath(child, originData, spliterator);
        }

        parentNode.children(children);
        return parentNode;

    }


    /**
     * @param parentId   父节点ID
     * @param originData 数据列表
     * @param <T>        implements ITree
     * @param <K>        implements Serializable
     * @author Neo
     * @since 2022/8/9 13:57
     */
    public static <T extends ITree<T, K>, K extends Serializable> List<T> filterByParentId(K parentId, List<T> originData) {
        return originData.stream()
                .filter(i -> Objects.equals(parentId, i.parentId()))
                .sorted(Comparator.comparing(T::index))
                .collect(Collectors.toList());

    }
}
